package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.TenantService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final BackupRegistroRepository backupRepo;
    private final TenantService tenantService;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${DB_USER:postgres}")
    private String dbUser;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    public BackupController(BackupRegistroRepository backupRepo, TenantService tenantService) {
        this.backupRepo = backupRepo;
        this.tenantService = tenantService;
    }

    @GetMapping("/listar")
    public ResponseEntity<?> listar() {
        Long empresaId = tenantService.getEmpresaId();
        List<BackupRegistro> registros = backupRepo.findByEmpresaIdOrderByDataCriacaoDesc(empresaId);
        return ResponseEntity.ok(registros.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("arquivo_nome", r.getArquivoNome());
            m.put("tamanho_bytes", r.getTamanhoBytes());
            m.put("tipo", r.getTipo());
            m.put("status", r.getStatus());
            m.put("data_criacao", r.getDataCriacao() != null ? r.getDataCriacao().toString() : null);
            m.put("observacoes", r.getObservacoes() != null ? r.getObservacoes() : "");
            return m;
        }).toList());
    }

    @PostMapping("/criar")
    public ResponseEntity<?> criar(@RequestBody(required = false) Map<String, Object> body) {
        Long empresaId = tenantService.getEmpresaId();
        Usuario usuario = tenantService.currentUser();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nomeArquivo = "backup_empresa_" + empresaId + "_" + timestamp + ".sql";
        Path backupDir = Path.of("backups");
        Path arquivoPath = backupDir.resolve(nomeArquivo);

        BackupRegistro registro = new BackupRegistro();
        registro.setArquivoNome(nomeArquivo);
        registro.setArquivoPath(arquivoPath.toString());
        registro.setTipo(body != null && body.containsKey("tipo") ? body.get("tipo").toString() : "manual");
        registro.setUsuario(usuario);
        Empresa emp = new Empresa(); emp.setId(empresaId);
        registro.setEmpresa(emp);

        try {
            Files.createDirectories(backupDir);
            String dbName = extractDbName(datasourceUrl);
            String host = extractHost(datasourceUrl);
            String port = extractPort(datasourceUrl);

            ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", host,
                "-p", port,
                "-U", dbUser,
                "--no-password",
                "-d", dbName,
                "-f", arquivoPath.toAbsolutePath().toString()
            );
            pb.environment().put("PGPASSWORD", dbPassword);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                registro.setStatus("falha");
                registro.setObservacoes("pg_dump falhou (exit " + exitCode + "): " + output.substring(0, Math.min(500, output.length())));
                backupRepo.save(registro);
                return ResponseEntity.status(500).body(Map.of("error", "Falha ao criar backup.", "detalhes", registro.getObservacoes()));
            }

            long tamanho = Files.exists(arquivoPath) ? Files.size(arquivoPath) : 0;
            registro.setTamanhoBytes(tamanho);
            registro.setStatus("sucesso");
            backupRepo.save(registro);

            return ResponseEntity.ok(Map.of(
                "ok", true,
                "arquivo", nomeArquivo,
                "tamanho_bytes", tamanho
            ));
        } catch (Exception ex) {
            registro.setStatus("falha");
            registro.setObservacoes(ex.getMessage());
            backupRepo.save(registro);
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao criar backup: " + ex.getMessage()));
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        BackupRegistro registro = backupRepo.findById(id)
            .filter(r -> empresaId.equals(r.getEmpresa() != null ? r.getEmpresa().getId() : null))
            .orElse(null);
        if (registro == null) return ResponseEntity.notFound().build();

        File arquivo = new File(registro.getArquivoPath());
        if (!arquivo.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(arquivo);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + registro.getArquivoNome() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        BackupRegistro registro = backupRepo.findById(id)
            .filter(r -> empresaId.equals(r.getEmpresa() != null ? r.getEmpresa().getId() : null))
            .orElse(null);
        if (registro == null) return ResponseEntity.notFound().build();
        try {
            Files.deleteIfExists(Path.of(registro.getArquivoPath()));
        } catch (IOException ignored) {}
        backupRepo.delete(registro);
        return ResponseEntity.noContent().build();
    }

    private String extractDbName(String url) {
        if (url == null || url.isBlank()) return "refricenter";
        String path = url.replaceAll("\\?.*$", "");
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private String extractHost(String url) {
        if (url == null || url.isBlank()) return "localhost";
        try {
            String noPrefix = url.replace("jdbc:postgresql://", "");
            String hostPort = noPrefix.split("/")[0];
            return hostPort.contains(":") ? hostPort.split(":")[0] : hostPort;
        } catch (Exception e) { return "localhost"; }
    }

    private String extractPort(String url) {
        if (url == null || url.isBlank()) return "5432";
        try {
            String noPrefix = url.replace("jdbc:postgresql://", "");
            String hostPort = noPrefix.split("/")[0];
            return hostPort.contains(":") ? hostPort.split(":")[1] : "5432";
        } catch (Exception e) { return "5432"; }
    }
}
