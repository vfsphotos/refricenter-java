package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.OrdemServicoService;
import com.refricenter.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class FinanceiroController {

    private final LancamentoFinanceiroRepository lancRepo;
    private final AberturaCaixaRepository caixaRepo;
    private final TributoFederalRepository tributoRepo;
    private final OrdemServicoService osService;
    private final TenantService tenantService;

    public FinanceiroController(LancamentoFinanceiroRepository lancRepo, AberturaCaixaRepository caixaRepo,
                                 TributoFederalRepository tributoRepo, OrdemServicoService osService,
                                 TenantService tenantService) {
        this.lancRepo = lancRepo;
        this.caixaRepo = caixaRepo;
        this.tributoRepo = tributoRepo;
        this.osService = osService;
        this.tenantService = tenantService;
    }

    // ── Lançamentos ──────────────────────────────────────────────────────────────

    @GetMapping("/lancamentos-financeiros")
    public ResponseEntity<?> listar(@RequestParam(required = false) String tipo,
                                     @RequestParam(required = false) String status) {
        Long empresaId = tenantService.getEmpresaId();
        List<LancamentoFinanceiro> lancs = empresaId != null ? lancRepo.findByEmpresaId(empresaId) : lancRepo.findAll();
        if (tipo != null) lancs = lancs.stream().filter(l -> tipo.equals(l.getTipo())).toList();
        if (status != null) lancs = lancs.stream().filter(l -> status.equals(l.getStatus())).toList();
        return ResponseEntity.ok(lancs.stream().map(this::serializeLanc).toList());
    }

    @PostMapping("/lancamentos-financeiros")
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        Empresa emp = new Empresa(); emp.setId(empresaId);

        LancamentoFinanceiro base = fromData(data, new LancamentoFinanceiro());
        base.setEmpresa(emp);
        base.setUsuario(tenantService.currentUser());

        int numeroParcelas = 1;
        if (Boolean.TRUE.equals(base.getParcelado()) && data.containsKey("numero_parcelas")) {
            numeroParcelas = Integer.parseInt(data.get("numero_parcelas").toString());
            base.setNumeroParcelas(numeroParcelas);
            base.setDescricao(base.getDescricao() + " (1/" + numeroParcelas + ")");
        }

        List<LancamentoFinanceiro> criados = osService.criarLancamento(base, numeroParcelas);
        return ResponseEntity.status(201).body(criados.stream().map(this::serializeLanc).toList());
    }

    @GetMapping("/lancamentos-financeiros/{id}")
    public ResponseEntity<?> detalhe(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        return lancRepo.findById(id)
            .filter(l -> empresaId == null || empresaId.equals(l.getEmpresa() != null ? l.getEmpresa().getId() : null))
            .map(l -> ResponseEntity.ok(serializeLanc(l)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/lancamentos-financeiros/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        LancamentoFinanceiro l = lancRepo.findById(id)
            .filter(lf -> empresaId == null || empresaId.equals(lf.getEmpresa() != null ? lf.getEmpresa().getId() : null))
            .orElse(null);
        if (l == null) return ResponseEntity.notFound().build();
        fromData(data, l);
        lancRepo.save(l);
        return ResponseEntity.ok(serializeLanc(l));
    }

    @PatchMapping("/lancamentos-financeiros/{id}")
    public ResponseEntity<?> patch(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return atualizar(id, data);
    }

    @DeleteMapping("/lancamentos-financeiros/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        LancamentoFinanceiro l = lancRepo.findById(id)
            .filter(lf -> empresaId == null || empresaId.equals(lf.getEmpresa() != null ? lf.getEmpresa().getId() : null))
            .orElse(null);
        if (l == null) return ResponseEntity.notFound().build();
        lancRepo.delete(l);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/relatorio-financeiro")
    public ResponseEntity<?> relatorioFinanceiro(
            @RequestParam(required = false) String data_inicio,
            @RequestParam(required = false) String data_fim) {
        Long empresaId = tenantService.getEmpresaId();
        LocalDate inicio = data_inicio != null ? LocalDate.parse(data_inicio) : LocalDate.now().withDayOfMonth(1);
        LocalDate fim = data_fim != null ? LocalDate.parse(data_fim) : LocalDate.now();

        BigDecimal receitas = lancRepo.sumByEmpresaAndTipoAndPeriodo(empresaId, "receita", inicio, fim);
        BigDecimal despesas = lancRepo.sumByEmpresaAndTipoAndPeriodo(empresaId, "despesa", inicio, fim);
        return ResponseEntity.ok(Map.of(
            "receitas", receitas, "despesas", despesas, "lucro", receitas.subtract(despesas),
            "periodo", Map.of("inicio", inicio.toString(), "fim", fim.toString())
        ));
    }

    // ── Caixa ────────────────────────────────────────────────────────────────────

    @GetMapping("/caixa/status")
    public ResponseEntity<?> statusCaixa() {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        var caixa = caixaRepo.findFirstByEmpresaIdAndDataFechamentoIsNullOrderByDataAberturaDesc(empresaId);
        if (caixa.isEmpty()) return ResponseEntity.ok(Map.of("aberto", false));
        AberturaCaixa c = caixa.get();
        return ResponseEntity.ok(Map.of(
            "aberto", true,
            "id", c.getId(),
            "data_abertura", c.getDataAbertura().toString(),
            "saldo_abertura", c.getSaldoAbertura()
        ));
    }

    @PostMapping("/caixa/abrir")
    public ResponseEntity<?> abrirCaixa(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        if (caixaRepo.findFirstByEmpresaIdAndDataFechamentoIsNullOrderByDataAberturaDesc(empresaId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error","Caixa já está aberto."));
        }
        Empresa emp = new Empresa(); emp.setId(empresaId);
        AberturaCaixa c = new AberturaCaixa();
        c.setEmpresa(emp);
        c.setSaldoAbertura(data.containsKey("saldo_abertura") ? new BigDecimal(data.get("saldo_abertura").toString()) : BigDecimal.ZERO);
        c.setObservacoes(data.getOrDefault("observacoes","").toString());
        c.setUsuarioAbertura(tenantService.currentUser());
        caixaRepo.save(c);
        return ResponseEntity.status(201).body(Map.of("ok", true, "id", c.getId()));
    }

    @PostMapping("/caixa/fechar")
    public ResponseEntity<?> fecharCaixa(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        AberturaCaixa c = caixaRepo.findFirstByEmpresaIdAndDataFechamentoIsNullOrderByDataAberturaDesc(empresaId)
            .orElse(null);
        if (c == null) return ResponseEntity.badRequest().body(Map.of("error","Nenhum caixa aberto."));
        c.setDataFechamento(LocalDateTime.now());
        c.setUsuarioFechamento(tenantService.currentUser());
        if (data.containsKey("saldo_fechamento")) c.setSaldoFechamento(new BigDecimal(data.get("saldo_fechamento").toString()));
        if (data.containsKey("observacoes")) c.setObservacoes(data.get("observacoes").toString());
        caixaRepo.save(c);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/caixa/historico")
    public ResponseEntity<?> historicoCaixa() {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        return ResponseEntity.ok(caixaRepo.findByEmpresaIdOrderByDataAberturaDesc(empresaId).stream().map(c -> Map.of(
            "id", c.getId(),
            "data_abertura", c.getDataAbertura().toString(),
            "data_fechamento", c.getDataFechamento() != null ? c.getDataFechamento().toString() : null,
            "saldo_abertura", c.getSaldoAbertura(),
            "saldo_fechamento", c.getSaldoFechamento(),
            "aberto", c.isEstaAberto()
        )).toList());
    }

    // ── Tributos ─────────────────────────────────────────────────────────────────

    @GetMapping("/tributos-federais")
    public ResponseEntity<?> listarTributos() {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        return ResponseEntity.ok(tributoRepo.findByEmpresaIdOrderByDataVencimento(empresaId).stream()
            .map(this::serializeTributo).toList());
    }

    @PostMapping("/tributos-federais")
    public ResponseEntity<?> criarTributo(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        TributoFederal t = new TributoFederal();
        Empresa emp = new Empresa(); emp.setId(empresaId); t.setEmpresa(emp);
        t.setCategoria(data.get("categoria").toString());
        t.setCompetencia(data.getOrDefault("competencia","").toString());
        t.setValor(new BigDecimal(data.get("valor").toString()));
        t.setDataVencimento(LocalDate.parse(data.get("data_vencimento").toString()));
        if (data.containsKey("observacoes")) t.setObservacoes(data.get("observacoes").toString());
        t.setUsuario(tenantService.currentUser());
        tributoRepo.save(t);
        return ResponseEntity.status(201).body(serializeTributo(t));
    }

    @PutMapping("/tributos-federais/{id}")
    public ResponseEntity<?> atualizarTributo(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        TributoFederal t = tributoRepo.findById(id)
            .filter(tr -> empresaId == null || empresaId.equals(tr.getEmpresa().getId()))
            .orElse(null);
        if (t == null) return ResponseEntity.notFound().build();

        boolean eraPago = t.getDataPagamentoRealizado() != null;

        if (data.containsKey("valor")) t.setValor(new BigDecimal(data.get("valor").toString()));
        if (data.containsKey("data_vencimento")) t.setDataVencimento(LocalDate.parse(data.get("data_vencimento").toString()));
        if (data.containsKey("data_pagamento_programada") && data.get("data_pagamento_programada") != null)
            t.setDataPagamentoProgramada(LocalDate.parse(data.get("data_pagamento_programada").toString()));
        if (data.containsKey("data_pagamento_realizado")) {
            Object val = data.get("data_pagamento_realizado");
            t.setDataPagamentoRealizado(val != null && !val.toString().isBlank() ? LocalDate.parse(val.toString()) : null);
        }
        if (data.containsKey("observacoes")) t.setObservacoes(data.get("observacoes").toString());
        tributoRepo.save(t);

        boolean agora_pago = t.getDataPagamentoRealizado() != null;
        if (eraPago != agora_pago) {
            osService.sincronizarLancamentoTributo(t, agora_pago, empresaId);
        }

        return ResponseEntity.ok(serializeTributo(t));
    }

    @PatchMapping("/tributos-federais/{id}")
    public ResponseEntity<?> patchTributo(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return atualizarTributo(id, data);
    }

    @DeleteMapping("/tributos-federais/{id}")
    public ResponseEntity<?> excluirTributo(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        TributoFederal t = tributoRepo.findById(id)
            .filter(tr -> empresaId == null || empresaId.equals(tr.getEmpresa().getId()))
            .orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        if (t.getLancamentoFinanceiro() != null) {
            LancamentoFinanceiro old = t.getLancamentoFinanceiro();
            t.setLancamentoFinanceiro(null);
            tributoRepo.save(t);
            lancRepo.delete(old);
        }
        tributoRepo.delete(t);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private LancamentoFinanceiro fromData(Map<String, Object> data, LancamentoFinanceiro l) {
        if (data.containsKey("descricao")) l.setDescricao(data.get("descricao").toString());
        if (data.containsKey("tipo")) l.setTipo(data.get("tipo").toString());
        if (data.containsKey("categoria")) l.setCategoria(data.get("categoria").toString());
        if (data.containsKey("valor")) l.setValor(new BigDecimal(data.get("valor").toString()));
        if (data.containsKey("data_vencimento")) l.setDataVencimento(LocalDate.parse(data.get("data_vencimento").toString()));
        if (data.containsKey("data_pagamento") && data.get("data_pagamento") != null && !data.get("data_pagamento").toString().isBlank())
            l.setDataPagamento(LocalDate.parse(data.get("data_pagamento").toString()));
        if (data.containsKey("forma_pagamento")) l.setFormaPagamento(data.get("forma_pagamento").toString());
        if (data.containsKey("status")) l.setStatus(data.get("status").toString());
        if (data.containsKey("observacoes")) l.setObservacoes(data.get("observacoes").toString());
        if (data.containsKey("parcelado")) l.setParcelado(Boolean.parseBoolean(data.get("parcelado").toString()));
        if (data.containsKey("numero_parcelas")) l.setNumeroParcelas(Integer.parseInt(data.get("numero_parcelas").toString()));
        return l;
    }

    private Map<String, Object> serializeLanc(LancamentoFinanceiro l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("descricao", l.getDescricao());
        m.put("tipo", l.getTipo());
        m.put("categoria", l.getCategoria());
        m.put("valor", l.getValor());
        m.put("data_vencimento", l.getDataVencimento() != null ? l.getDataVencimento().toString() : null);
        m.put("data_pagamento", l.getDataPagamento() != null ? l.getDataPagamento().toString() : null);
        m.put("forma_pagamento", l.getFormaPagamento());
        m.put("status", l.getStatus());
        m.put("observacoes", l.getObservacoes());
        m.put("parcelado", l.getParcelado());
        m.put("numero_parcelas", l.getNumeroParcelas());
        m.put("numero_parcela", l.getNumeroParcela());
        m.put("ordem_servico", l.getOrdemServico() != null ? l.getOrdemServico().getId() : null);
        m.put("cliente", l.getCliente() != null ? Map.of("id", l.getCliente().getId(), "nome", l.getCliente().getNome()) : null);
        return m;
    }

    private Map<String, Object> serializeTributo(TributoFederal t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("categoria", t.getCategoria());
        m.put("competencia", t.getCompetencia() != null ? t.getCompetencia() : "");
        m.put("valor", t.getValor());
        m.put("data_vencimento", t.getDataVencimento().toString());
        m.put("data_pagamento_programada", t.getDataPagamentoProgramada() != null ? t.getDataPagamentoProgramada().toString() : null);
        m.put("data_pagamento_realizado", t.getDataPagamentoRealizado() != null ? t.getDataPagamentoRealizado().toString() : null);
        m.put("status", t.getStatus());
        m.put("observacoes", t.getObservacoes() != null ? t.getObservacoes() : "");
        return m;
    }
}
