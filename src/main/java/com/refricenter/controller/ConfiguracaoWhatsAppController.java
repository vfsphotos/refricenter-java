package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/config-whatsapp")
public class ConfiguracaoWhatsAppController {

    private final ConfiguracaoWhatsAppRepository configRepo;
    private final TenantService tenantService;

    public ConfiguracaoWhatsAppController(ConfiguracaoWhatsAppRepository configRepo,
                                           TenantService tenantService) {
        this.configRepo = configRepo;
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<?> get() {
        Long empresaId = tenantService.getEmpresaId();
        ConfiguracaoWhatsApp cfg = configRepo.findByEmpresaId(empresaId).orElseGet(() -> {
            ConfiguracaoWhatsApp nova = new ConfiguracaoWhatsApp();
            Empresa emp = new Empresa(); emp.setId(empresaId);
            nova.setEmpresa(emp);
            nova.setAtivo(false);
            return configRepo.save(nova);
        });
        return ResponseEntity.ok(Map.of("id", cfg.getId(), "ativo", cfg.getAtivo()));
    }

    @PutMapping
    public ResponseEntity<?> atualizar(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        ConfiguracaoWhatsApp cfg = configRepo.findByEmpresaId(empresaId).orElseGet(() -> {
            ConfiguracaoWhatsApp nova = new ConfiguracaoWhatsApp();
            Empresa emp = new Empresa(); emp.setId(empresaId);
            nova.setEmpresa(emp);
            return nova;
        });
        if (data.containsKey("ativo")) cfg.setAtivo(Boolean.parseBoolean(data.get("ativo").toString()));
        configRepo.save(cfg);
        return ResponseEntity.ok(Map.of("id", cfg.getId(), "ativo", cfg.getAtivo()));
    }

    @PatchMapping
    public ResponseEntity<?> patch(@RequestBody Map<String, Object> data) {
        return atualizar(data);
    }
}
