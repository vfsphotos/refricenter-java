package com.refricenter.controller;

import com.refricenter.repository.AuditoriaLogRepository;
import com.refricenter.service.TenantService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaLogRepository auditoriaRepo;
    private final TenantService tenantService;

    public AuditoriaController(AuditoriaLogRepository auditoriaRepo, TenantService tenantService) {
        this.auditoriaRepo = auditoriaRepo;
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "50") int size) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        var logs = auditoriaRepo.findByEmpresaIdOrderByDataDesc(empresaId, PageRequest.of(page, size));
        return ResponseEntity.ok(logs.stream().map(l -> Map.of(
            "id", l.getId(),
            "acao", l.getAcao(),
            "modelo", l.getModelo() != null ? l.getModelo() : "",
            "objeto_id", l.getObjetoId() != null ? l.getObjetoId() : "",
            "descricao", l.getDescricao(),
            "usuario", l.getUsuario() != null ? l.getUsuario().getUsername() : null,
            "ip", l.getIp() != null ? l.getIp() : "",
            "data", l.getData().toString()
        )).toList());
    }
}
