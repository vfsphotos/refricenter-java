package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.TenantService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/fiscal")
public class FiscalController {

    private final ConfiguracaoFiscalRepository fiscalConfigRepo;
    private final NotaFiscalRepository notaRepo;
    private final TenantService tenantService;

    public FiscalController(ConfiguracaoFiscalRepository fiscalConfigRepo, NotaFiscalRepository notaRepo,
                             TenantService tenantService) {
        this.fiscalConfigRepo = fiscalConfigRepo;
        this.notaRepo = notaRepo;
        this.tenantService = tenantService;
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        ConfiguracaoFiscal cfg = fiscalConfigRepo.findByEmpresaId(empresaId).orElse(new ConfiguracaoFiscal());
        return ResponseEntity.ok(serializeConfig(cfg));
    }

    @PostMapping("/config")
    public ResponseEntity<?> salvarConfig(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        ConfiguracaoFiscal cfg = fiscalConfigRepo.findByEmpresaId(empresaId).orElse(new ConfiguracaoFiscal());
        if (cfg.getEmpresa() == null) { Empresa emp = new Empresa(); emp.setId(empresaId); cfg.setEmpresa(emp); }
        if (data.containsKey("token")) cfg.setToken(data.get("token").toString());
        if (data.containsKey("ambiente")) cfg.setAmbiente(data.get("ambiente").toString());
        if (data.containsKey("cnpj_emitente")) cfg.setCnpjEmitente(data.get("cnpj_emitente").toString());
        if (data.containsKey("razao_social")) cfg.setRazaoSocial(data.get("razao_social").toString());
        if (data.containsKey("ativo")) cfg.setAtivo(Boolean.parseBoolean(data.get("ativo").toString()));
        fiscalConfigRepo.save(cfg);
        return ResponseEntity.ok(serializeConfig(cfg));
    }

    @GetMapping("/notas")
    public ResponseEntity<?> listarNotas() {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        return ResponseEntity.ok(notaRepo.findByEmpresaIdOrderByDataEmissaoDesc(empresaId).stream().map(this::serializeNota).toList());
    }

    @PostMapping("/emitir")
    public ResponseEntity<?> emitir(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        ConfiguracaoFiscal cfg = fiscalConfigRepo.findByEmpresaId(empresaId).orElse(null);
        if (cfg == null || !Boolean.TRUE.equals(cfg.getAtivo())) {
            return ResponseEntity.badRequest().body(Map.of("error","Módulo fiscal não configurado ou inativo."));
        }
        // Emissão real via Focus NFe delegada ao time Java — aqui cria rascunho
        NotaFiscal nota = new NotaFiscal();
        Empresa emp = new Empresa(); emp.setId(empresaId); nota.setEmpresa(emp);
        nota.setTipo(data.getOrDefault("tipo","nfce").toString());
        nota.setStatus("processando");
        nota.setValorTotal(data.containsKey("valor_total") ? new BigDecimal(data.get("valor_total").toString()) : BigDecimal.ZERO);
        notaRepo.save(nota);
        return ResponseEntity.status(201).body(serializeNota(nota));
    }

    @GetMapping("/consultar/{ref}")
    public ResponseEntity<?> consultar(@PathVariable String ref) {
        Long empresaId = tenantService.getEmpresaId();
        ConfiguracaoFiscal cfg = fiscalConfigRepo.findByEmpresaId(empresaId).orElse(null);
        if (cfg == null) return ResponseEntity.badRequest().body(Map.of("error","Fiscal não configurado."));
        // Integração real com Focus NFe a ser implementada
        return ResponseEntity.ok(Map.of("ref", ref, "status", "pendente", "mensagem", "Consultar via Focus NFe"));
    }

    @DeleteMapping("/cancelar/{notaId}")
    public ResponseEntity<?> cancelar(@PathVariable Long notaId) {
        Long empresaId = tenantService.getEmpresaId();
        NotaFiscal nota = notaRepo.findById(notaId)
            .filter(n -> empresaId == null || empresaId.equals(n.getEmpresa() != null ? n.getEmpresa().getId() : null))
            .orElse(null);
        if (nota == null) return ResponseEntity.notFound().build();
        nota.setStatus("cancelado");
        notaRepo.save(nota);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private Map<String, Object> serializeConfig(ConfiguracaoFiscal cfg) {
        return Map.of(
            "token", cfg.getToken() != null ? cfg.getToken() : "",
            "ambiente", cfg.getAmbiente() != null ? cfg.getAmbiente() : "2",
            "cnpj_emitente", cfg.getCnpjEmitente() != null ? cfg.getCnpjEmitente() : "",
            "razao_social", cfg.getRazaoSocial() != null ? cfg.getRazaoSocial() : "",
            "ativo", cfg.getAtivo() != null ? cfg.getAtivo() : false
        );
    }

    private Map<String, Object> serializeNota(NotaFiscal n) {
        return Map.of(
            "id", n.getId(),
            "tipo", n.getTipo(),
            "status", n.getStatus(),
            "numero", n.getNumero() != null ? n.getNumero() : "",
            "chave", n.getChave() != null ? n.getChave() : "",
            "valor_total", n.getValorTotal(),
            "data_emissao", n.getDataEmissao().toString()
        );
    }
}
