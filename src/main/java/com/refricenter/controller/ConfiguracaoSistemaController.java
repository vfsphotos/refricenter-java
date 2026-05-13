package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ConfiguracaoSistemaController {

    private final ConfiguracaoSistemaRepository configRepo;
    private final TenantService tenantService;

    public ConfiguracaoSistemaController(ConfiguracaoSistemaRepository configRepo,
                                          TenantService tenantService) {
        this.configRepo = configRepo;
        this.tenantService = tenantService;
    }

    @GetMapping("/configuracoes-sistema")
    public ResponseEntity<?> get() {
        Long empresaId = tenantService.getEmpresaId();
        ConfiguracaoSistema cfg = configRepo.findByEmpresaId(empresaId).orElseGet(() -> {
            ConfiguracaoSistema nova = new ConfiguracaoSistema();
            Empresa emp = new Empresa();
            emp.setId(empresaId);
            nova.setEmpresa(emp);
            return configRepo.save(nova);
        });
        return ResponseEntity.ok(serialize(cfg));
    }

    @PutMapping("/configuracoes-sistema")
    public ResponseEntity<?> atualizar(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        ConfiguracaoSistema cfg = configRepo.findByEmpresaId(empresaId).orElseGet(() -> {
            ConfiguracaoSistema nova = new ConfiguracaoSistema();
            Empresa emp = new Empresa();
            emp.setId(empresaId);
            nova.setEmpresa(emp);
            return nova;
        });
        applyData(data, cfg);
        configRepo.save(cfg);
        return ResponseEntity.ok(serialize(cfg));
    }

    @PatchMapping("/configuracoes-sistema")
    public ResponseEntity<?> patch(@RequestBody Map<String, Object> data) {
        return atualizar(data);
    }

    private void applyData(Map<String, Object> data, ConfiguracaoSistema cfg) {
        if (data.containsKey("nome_empresa")) cfg.setNomeEmpresa(data.get("nome_empresa").toString());
        if (data.containsKey("cnpj_empresa")) cfg.setCnpjEmpresa(data.get("cnpj_empresa").toString());
        if (data.containsKey("endereco")) cfg.setEndereco(data.get("endereco").toString());
        if (data.containsKey("numero_endereco")) cfg.setNumeroEndereco(data.get("numero_endereco").toString());
        if (data.containsKey("bairro")) cfg.setBairro(data.get("bairro").toString());
        if (data.containsKey("cidade")) cfg.setCidade(data.get("cidade").toString());
        if (data.containsKey("estado")) cfg.setEstado(data.get("estado").toString());
        if (data.containsKey("cep")) cfg.setCep(data.get("cep").toString());
        if (data.containsKey("telefone")) cfg.setTelefone(data.get("telefone").toString());
        if (data.containsKey("email")) cfg.setEmail(data.get("email").toString());
        if (data.containsKey("horario_funcionamento")) cfg.setHorarioFuncionamento(data.get("horario_funcionamento").toString());
        if (data.containsKey("observacoes_nota")) cfg.setObservacoesNota(data.get("observacoes_nota").toString());
        if (data.containsKey("mensagem_boas_vindas")) cfg.setMensagemBoasVindas(data.get("mensagem_boas_vindas").toString());
        if (data.containsKey("numero_inicial_os")) cfg.setNumeroInicialOs(Integer.parseInt(data.get("numero_inicial_os").toString()));
        if (data.containsKey("caixa_inicial")) cfg.setCaixaInicial(new BigDecimal(data.get("caixa_inicial").toString()));
    }

    private Map<String, Object> serialize(ConfiguracaoSistema cfg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", cfg.getId());
        m.put("nome_empresa", cfg.getNomeEmpresa() != null ? cfg.getNomeEmpresa() : "");
        m.put("cnpj_empresa", cfg.getCnpjEmpresa() != null ? cfg.getCnpjEmpresa() : "");
        m.put("endereco", cfg.getEndereco() != null ? cfg.getEndereco() : "");
        m.put("numero_endereco", cfg.getNumeroEndereco() != null ? cfg.getNumeroEndereco() : "");
        m.put("bairro", cfg.getBairro() != null ? cfg.getBairro() : "");
        m.put("cidade", cfg.getCidade() != null ? cfg.getCidade() : "");
        m.put("estado", cfg.getEstado() != null ? cfg.getEstado() : "");
        m.put("cep", cfg.getCep() != null ? cfg.getCep() : "");
        m.put("telefone", cfg.getTelefone() != null ? cfg.getTelefone() : "");
        m.put("email", cfg.getEmail() != null ? cfg.getEmail() : "");
        m.put("horario_funcionamento", cfg.getHorarioFuncionamento() != null ? cfg.getHorarioFuncionamento() : "");
        m.put("logo_path", cfg.getLogoPath() != null ? cfg.getLogoPath() : "");
        m.put("observacoes_nota", cfg.getObservacoesNota() != null ? cfg.getObservacoesNota() : "");
        m.put("mensagem_boas_vindas", cfg.getMensagemBoasVindas() != null ? cfg.getMensagemBoasVindas() : "");
        m.put("numero_inicial_os", cfg.getNumeroInicialOs());
        m.put("ultima_os", cfg.getUltimaOs());
        m.put("caixa_inicial", cfg.getCaixaInicial());
        return m;
    }
}
