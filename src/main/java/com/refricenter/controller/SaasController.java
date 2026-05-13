package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.security.JwtTokenProvider;
import com.refricenter.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/saas")
public class SaasController {

    private final EmpresaRepository empresaRepo;
    private final UsuarioEmpresaRepository ueRepo;
    private final SuperAdminRepository superAdminRepo;
    private final PlataformaConfigRepository plataformaRepo;
    private final TenantService tenantService;
    private final JwtTokenProvider tokenProvider;

    public SaasController(EmpresaRepository empresaRepo, UsuarioEmpresaRepository ueRepo,
                          SuperAdminRepository superAdminRepo, PlataformaConfigRepository plataformaRepo,
                          TenantService tenantService, JwtTokenProvider tokenProvider) {
        this.empresaRepo = empresaRepo;
        this.ueRepo = ueRepo;
        this.superAdminRepo = superAdminRepo;
        this.plataformaRepo = plataformaRepo;
        this.tenantService = tenantService;
        this.tokenProvider = tokenProvider;
    }

    private ResponseEntity<?> checkSuperAdmin() {
        if (!tenantService.isSuperAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error","Acesso restrito a superadmins."));
        }
        return null;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        ResponseEntity<?> check = checkSuperAdmin();
        if (check != null) return check;
        return ResponseEntity.ok(Map.of(
            "total_empresas", empresaRepo.count(),
            "empresas_ativas", empresaRepo.findAll().stream().filter(e -> Boolean.TRUE.equals(e.getAtiva())).count(),
            "total_usuarios", ueRepo.count()
        ));
    }

    @GetMapping("/empresas")
    public ResponseEntity<?> listarEmpresas() {
        ResponseEntity<?> check = checkSuperAdmin();
        if (check != null) return check;
        return ResponseEntity.ok(empresaRepo.findAll().stream().map(this::serializeEmpresa).toList());
    }

    @GetMapping("/empresas/{id}")
    public ResponseEntity<?> detalheEmpresa(@PathVariable Long id) {
        ResponseEntity<?> check = checkSuperAdmin();
        if (check != null) return check;
        return empresaRepo.findById(id).map(e -> ResponseEntity.ok(serializeEmpresa(e)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/empresas/{id}")
    public ResponseEntity<?> atualizarEmpresa(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        ResponseEntity<?> check = checkSuperAdmin();
        if (check != null) return check;
        Empresa emp = empresaRepo.findById(id).orElse(null);
        if (emp == null) return ResponseEntity.notFound().build();
        if (data.containsKey("ativa")) emp.setAtiva(Boolean.parseBoolean(data.get("ativa").toString()));
        if (data.containsKey("plano")) emp.setPlano(data.get("plano").toString());
        if (data.containsKey("fiscal_habilitado")) emp.setFiscalHabilitado(Boolean.parseBoolean(data.get("fiscal_habilitado").toString()));
        empresaRepo.save(emp);
        return ResponseEntity.ok(serializeEmpresa(emp));
    }

    @PutMapping("/empresas/{id}/modulos")
    public ResponseEntity<?> atualizarModulos(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        ResponseEntity<?> check = checkSuperAdmin();
        if (check != null) return check;
        Empresa emp = empresaRepo.findById(id).orElse(null);
        if (emp == null) return ResponseEntity.notFound().build();
        if (data.containsKey("fiscal_habilitado"))
            emp.setFiscalHabilitado(Boolean.parseBoolean(data.get("fiscal_habilitado").toString()));
        empresaRepo.save(emp);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/impersonar")
    public ResponseEntity<?> impersonar(@RequestBody Map<String, Object> data) {
        Usuario user = tenantService.currentUser();
        if (!tenantService.isSuperAdmin()) return ResponseEntity.status(403).body(Map.of("error","Acesso negado."));
        SuperAdmin sa = user.getSuperAdmin();
        if (!Boolean.TRUE.equals(sa.getPodeImpersonar())) {
            return ResponseEntity.status(403).body(Map.of("error","Você não tem permissão para impersonar."));
        }
        Long empresaId = Long.parseLong(data.get("empresa_id").toString());
        Empresa emp = empresaRepo.findById(empresaId).orElse(null);
        if (emp == null) return ResponseEntity.badRequest().body(Map.of("error","Empresa não encontrada."));
        // Gera token para o dono da empresa
        String token = tokenProvider.generateToken(emp.getDono().getUsername());
        return ResponseEntity.ok(Map.of("token", token, "empresa", serializeEmpresa(emp)));
    }

    @GetMapping("/plataforma")
    public ResponseEntity<?> getPlataforma() {
        ResponseEntity<?> check = checkSuperAdmin();
        if (check != null) return check;
        PlataformaConfig cfg = plataformaRepo.findAll().stream().findFirst().orElse(new PlataformaConfig());
        return ResponseEntity.ok(Map.of(
            "manutencao_ativa", cfg.getManutencaoAtiva(),
            "mensagem_manutencao", cfg.getMensagemManutencao(),
            "nome_plataforma", cfg.getNomePlataforma()
        ));
    }

    @PutMapping("/manutencao")
    public ResponseEntity<?> setManutencao(@RequestBody Map<String, Object> data) {
        ResponseEntity<?> check = checkSuperAdmin();
        if (check != null) return check;
        PlataformaConfig cfg = plataformaRepo.findAll().stream().findFirst().orElse(new PlataformaConfig());
        if (data.containsKey("manutencao_ativa")) cfg.setManutencaoAtiva(Boolean.parseBoolean(data.get("manutencao_ativa").toString()));
        if (data.containsKey("mensagem_manutencao")) cfg.setMensagemManutencao(data.get("mensagem_manutencao").toString());
        plataformaRepo.save(cfg);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios() {
        ResponseEntity<?> check = checkSuperAdmin();
        if (check != null) return check;
        return ResponseEntity.ok(ueRepo.findAll().stream().map(ue -> Map.of(
            "id", ue.getId(),
            "username", ue.getUser().getUsername(),
            "nome", ue.getUser().getFullName(),
            "empresa", ue.getEmpresa().getNome(),
            "papel", ue.getPapel(),
            "ativo", ue.getAtivo()
        )).toList());
    }

    @GetMapping("/logs")
    public ResponseEntity<?> logs() {
        ResponseEntity<?> check = checkSuperAdmin();
        if (check != null) return check;
        return ResponseEntity.ok(Map.of("message","Logs disponíveis na auditoria de cada empresa."));
    }

    @GetMapping("/superadmins")
    public ResponseEntity<?> listarSuperAdmins() {
        ResponseEntity<?> check = checkSuperAdmin();
        if (check != null) return check;
        return ResponseEntity.ok(superAdminRepo.findAll().stream().map(sa -> Map.of(
            "id", sa.getId(),
            "username", sa.getUser().getUsername(),
            "nome", sa.getUser().getFullName(),
            "nivel", sa.getNivel(),
            "ativo", sa.getAtivo(),
            "pode_impersonar", sa.getPodeImpersonar()
        )).toList());
    }

    private Map<String, Object> serializeEmpresa(Empresa e) {
        return Map.of(
            "id", e.getId(),
            "nome", e.getNome(),
            "cnpj", e.getCnpj() != null ? e.getCnpj() : "",
            "slug", e.getSlug(),
            "ativa", e.getAtiva(),
            "plano", e.getPlano(),
            "fiscal_habilitado", e.getFiscalHabilitado(),
            "data_criacao", e.getDataCriacao().toString()
        );
    }
}
