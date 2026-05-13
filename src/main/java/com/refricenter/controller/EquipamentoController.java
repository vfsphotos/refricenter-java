package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/equipamentos")
public class EquipamentoController {

    private final EquipamentoRepository equipamentoRepo;
    private final ClienteRepository clienteRepo;
    private final MarcaRepository marcaRepo;
    private final TenantService tenantService;

    public EquipamentoController(EquipamentoRepository equipamentoRepo,
                                 ClienteRepository clienteRepo,
                                 MarcaRepository marcaRepo,
                                 TenantService tenantService) {
        this.equipamentoRepo = equipamentoRepo;
        this.clienteRepo = clienteRepo;
        this.marcaRepo = marcaRepo;
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Long cliente_id) {
        Long empresaId = tenantService.getEmpresaId();
        List<Equipamento> lista;
        if (cliente_id != null) {
            lista = equipamentoRepo.findByClienteId(cliente_id);
        } else {
            lista = equipamentoRepo.findByClienteEmpresaId(empresaId);
        }
        return ResponseEntity.ok(lista.stream().map(this::serialize).toList());
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> data) {
        Long clienteId = Long.parseLong(data.get("cliente_id").toString());
        Long empresaId = tenantService.getEmpresaId();
        Cliente cliente = clienteRepo.findById(clienteId)
            .filter(c -> empresaId == null || empresaId.equals(c.getEmpresa() != null ? c.getEmpresa().getId() : null))
            .orElse(null);
        if (cliente == null) return ResponseEntity.badRequest().body(Map.of("error", "Cliente não encontrado."));

        Equipamento e = fromData(data, new Equipamento());
        e.setCliente(cliente);
        if (data.containsKey("marca_id") && data.get("marca_id") != null) {
            marcaRepo.findById(Long.parseLong(data.get("marca_id").toString()))
                .ifPresent(e::setMarca);
        }
        equipamentoRepo.save(e);
        return ResponseEntity.status(201).body(serialize(e));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalhe(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        return equipamentoRepo.findById(id)
            .filter(e -> empresaId == null || (e.getCliente() != null && e.getCliente().getEmpresa() != null
                && empresaId.equals(e.getCliente().getEmpresa().getId())))
            .map(e -> ResponseEntity.ok(serialize(e)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        Equipamento e = equipamentoRepo.findById(id)
            .filter(eq -> empresaId == null || (eq.getCliente() != null && eq.getCliente().getEmpresa() != null
                && empresaId.equals(eq.getCliente().getEmpresa().getId())))
            .orElse(null);
        if (e == null) return ResponseEntity.notFound().build();
        fromData(data, e);
        if (data.containsKey("marca_id") && data.get("marca_id") != null) {
            marcaRepo.findById(Long.parseLong(data.get("marca_id").toString()))
                .ifPresent(e::setMarca);
        }
        equipamentoRepo.save(e);
        return ResponseEntity.ok(serialize(e));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return atualizar(id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        Equipamento e = equipamentoRepo.findById(id)
            .filter(eq -> empresaId == null || (eq.getCliente() != null && eq.getCliente().getEmpresa() != null
                && empresaId.equals(eq.getCliente().getEmpresa().getId())))
            .orElse(null);
        if (e == null) return ResponseEntity.notFound().build();
        e.setAtivo(false);
        equipamentoRepo.save(e);
        return ResponseEntity.noContent().build();
    }

    private Equipamento fromData(Map<String, Object> data, Equipamento e) {
        if (data.containsKey("modelo")) e.setModelo(data.get("modelo").toString());
        if (data.containsKey("numero_serie")) e.setNumeroSerie(data.get("numero_serie").toString());
        if (data.containsKey("potencia")) e.setPotencia(data.get("potencia").toString());
        if (data.containsKey("tensao")) e.setTensao(data.get("tensao").toString());
        if (data.containsKey("defeito_reclamado")) e.setDefeitoReclamado(data.get("defeito_reclamado").toString());
        if (data.containsKey("acessorios")) e.setAcessorios(data.get("acessorios").toString());
        if (data.containsKey("estado_conserto")) e.setEstadoConserto(data.get("estado_conserto").toString());
        if (data.containsKey("observacoes_tecnicas")) e.setObservacoesTecnicas(data.get("observacoes_tecnicas").toString());
        if (data.containsKey("data_prevista_entrega") && data.get("data_prevista_entrega") != null)
            e.setDataPrevistaEntrega(LocalDateTime.parse(data.get("data_prevista_entrega").toString()));
        if (data.containsKey("ativo")) e.setAtivo(Boolean.parseBoolean(data.get("ativo").toString()));
        return e;
    }

    private Map<String, Object> serialize(Equipamento e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("cliente_id", e.getCliente() != null ? e.getCliente().getId() : null);
        m.put("cliente_nome", e.getCliente() != null ? e.getCliente().getNome() : "");
        m.put("marca", e.getMarca() != null ? Map.of("id", e.getMarca().getId(), "nome", e.getMarca().getNome()) : null);
        m.put("modelo", e.getModelo());
        m.put("numero_serie", e.getNumeroSerie() != null ? e.getNumeroSerie() : "");
        m.put("potencia", e.getPotencia() != null ? e.getPotencia() : "");
        m.put("tensao", e.getTensao() != null ? e.getTensao() : "");
        m.put("defeito_reclamado", e.getDefeitoReclamado() != null ? e.getDefeitoReclamado() : "");
        m.put("acessorios", e.getAcessorios() != null ? e.getAcessorios() : "");
        m.put("estado_conserto", e.getEstadoConserto() != null ? e.getEstadoConserto() : "recebido");
        m.put("observacoes_tecnicas", e.getObservacoesTecnicas() != null ? e.getObservacoesTecnicas() : "");
        m.put("data_recebimento", e.getDataRecebimento() != null ? e.getDataRecebimento().toString() : null);
        m.put("data_prevista_entrega", e.getDataPrevistaEntrega() != null ? e.getDataPrevistaEntrega().toString() : null);
        m.put("data_entrega", e.getDataEntrega() != null ? e.getDataEntrega().toString() : null);
        m.put("ativo", e.getAtivo());
        return m;
    }
}
