package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.TenantService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RelatorioController {

    private final OrdemServicoRepository osRepo;
    private final ProdutoRepository produtoRepo;
    private final MovimentacaoEstoqueRepository movRepo;
    private final LancamentoFinanceiroRepository lancRepo;
    private final ClienteRepository clienteRepo;
    private final TenantService tenantService;

    public RelatorioController(OrdemServicoRepository osRepo, ProdutoRepository produtoRepo,
                               MovimentacaoEstoqueRepository movRepo,
                               LancamentoFinanceiroRepository lancRepo,
                               ClienteRepository clienteRepo,
                               TenantService tenantService) {
        this.osRepo = osRepo;
        this.produtoRepo = produtoRepo;
        this.movRepo = movRepo;
        this.lancRepo = lancRepo;
        this.clienteRepo = clienteRepo;
        this.tenantService = tenantService;
    }

    @GetMapping("/relatorio-os-periodo")
    public ResponseEntity<?> relatorioPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_fim,
            @RequestParam(required = false) String status) {

        Long empresaId = tenantService.getEmpresaId();
        LocalDateTime inicio = data_inicio.atStartOfDay();
        LocalDateTime fim = data_fim.atTime(23, 59, 59);

        List<OrdemServico> ordens = osRepo.findByEmpresaId(empresaId).stream()
            .filter(o -> o.getDataAbertura() != null
                && !o.getDataAbertura().isBefore(inicio)
                && !o.getDataAbertura().isAfter(fim))
            .filter(o -> status == null || status.isBlank() || status.equals(o.getStatus()))
            .toList();

        Map<String, Long> statusCounts = ordens.stream()
            .collect(Collectors.groupingBy(OrdemServico::getStatus, Collectors.counting()));

        BigDecimal totalReceita = ordens.stream()
            .filter(o -> List.of("faturado","entregue").contains(o.getStatus()))
            .map(OrdemServico::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> resultado = ordens.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("numero", o.getNumero());
            m.put("cliente", o.getCliente() != null ? o.getCliente().getNome() : "");
            m.put("status", o.getStatus());
            m.put("prioridade", o.getPrioridade());
            m.put("valor_total", o.getValorTotal());
            m.put("data_abertura", o.getDataAbertura() != null ? o.getDataAbertura().toString() : null);
            m.put("data_conclusao", o.getDataConclusao() != null ? o.getDataConclusao().toString() : null);
            m.put("data_faturamento", o.getDataFaturamento() != null ? o.getDataFaturamento().toString() : null);
            m.put("forma_pagamento", o.getFormaPagamento());
            m.put("tecnico", o.getTecnicoResponsavel() != null ? o.getTecnicoResponsavel().getFullName() : "");
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of(
            "ordens", resultado,
            "total", ordens.size(),
            "status_counts", statusCounts,
            "total_receita", totalReceita
        ));
    }

    @GetMapping("/relatorio-estoque")
    public ResponseEntity<?> relatorioEstoque() {
        Long empresaId = tenantService.getEmpresaId();
        List<Produto> produtos = produtoRepo.findByEmpresaId(empresaId);
        List<MovimentacaoEstoque> movs = movRepo.findByProdutoEmpresaIdOrderByDataDesc(empresaId);

        Map<Long, List<MovimentacaoEstoque>> movPorProduto = movs.stream()
            .filter(m -> m.getProduto() != null)
            .collect(Collectors.groupingBy(m -> m.getProduto().getId()));

        List<Map<String, Object>> resultado = produtos.stream().map(p -> {
            List<MovimentacaoEstoque> pMovs = movPorProduto.getOrDefault(p.getId(), List.of());
            int entradas = pMovs.stream().filter(m -> "entrada".equals(m.getTipo()))
                .mapToInt(MovimentacaoEstoque::getQuantidade).sum();
            int saidas = pMovs.stream().filter(m -> "saida".equals(m.getTipo()))
                .mapToInt(MovimentacaoEstoque::getQuantidade).sum();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("codigo", p.getCodigo());
            m.put("nome", p.getNome());
            m.put("categoria", p.getCategoria() != null ? p.getCategoria().getNome() : "");
            m.put("marca", p.getMarca() != null ? p.getMarca().getNome() : "");
            m.put("estoque_atual", p.getEstoqueAtual());
            m.put("estoque_minimo", p.getEstoqueMinimo());
            m.put("estoque_baixo", p.isEstoqueBaixo());
            m.put("preco_custo", p.getPrecoCusto());
            m.put("preco_venda", p.getPrecoVenda());
            m.put("total_entradas", entradas);
            m.put("total_saidas", saidas);
            return m;
        }).toList();

        int totalProdutos = produtos.size();
        int produtosBaixoEstoque = (int) produtos.stream().filter(Produto::isEstoqueBaixo).count();
        BigDecimal valorTotalEstoque = produtos.stream()
            .map(p -> p.getPrecoCusto().multiply(BigDecimal.valueOf(p.getEstoqueAtual())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
            "produtos", resultado,
            "total_produtos", totalProdutos,
            "produtos_baixo_estoque", produtosBaixoEstoque,
            "valor_total_estoque", valorTotalEstoque
        ));
    }

    @GetMapping("/relatorio-financeiro-detalhado")
    public ResponseEntity<?> relatorioFinanceiroDetalhado(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_fim) {

        Long empresaId = tenantService.getEmpresaId();
        List<LancamentoFinanceiro> lancamentos = lancRepo.findByEmpresaIdAndDataVencimentoBetween(
            empresaId, data_inicio, data_fim);

        BigDecimal totalReceitas = lancamentos.stream()
            .filter(l -> "receita".equals(l.getTipo()) && "pago".equals(l.getStatus()))
            .map(LancamentoFinanceiro::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDespesas = lancamentos.stream()
            .filter(l -> "despesa".equals(l.getTipo()) && "pago".equals(l.getStatus()))
            .map(LancamentoFinanceiro::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> porCategoria = lancamentos.stream()
            .filter(l -> "pago".equals(l.getStatus()))
            .collect(Collectors.groupingBy(
                l -> l.getCategoria() != null ? l.getCategoria() : "outros",
                Collectors.reducing(BigDecimal.ZERO, LancamentoFinanceiro::getValor, BigDecimal::add)
            ));

        DateTimeFormatter mesFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Map<String, BigDecimal>> porMes = new TreeMap<>();
        for (LancamentoFinanceiro l : lancamentos) {
            if (!"pago".equals(l.getStatus())) continue;
            String mes = l.getDataVencimento().format(mesFormatter);
            porMes.computeIfAbsent(mes, k -> new HashMap<>());
            String tipo = l.getTipo() != null ? l.getTipo() : "outros";
            porMes.get(mes).merge(tipo, l.getValor(), BigDecimal::add);
        }

        List<Map<String, Object>> detalhes = lancamentos.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("descricao", l.getDescricao());
            m.put("tipo", l.getTipo());
            m.put("categoria", l.getCategoria());
            m.put("valor", l.getValor());
            m.put("data_vencimento", l.getDataVencimento() != null ? l.getDataVencimento().toString() : null);
            m.put("data_pagamento", l.getDataPagamento() != null ? l.getDataPagamento().toString() : null);
            m.put("status", l.getStatus());
            m.put("forma_pagamento", l.getFormaPagamento());
            m.put("os_numero", l.getOrdemServico() != null ? l.getOrdemServico().getNumero() : null);
            m.put("cliente", l.getCliente() != null ? l.getCliente().getNome() : null);
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of(
            "lancamentos", detalhes,
            "total_receitas", totalReceitas,
            "total_despesas", totalDespesas,
            "lucro_liquido", totalReceitas.subtract(totalDespesas),
            "por_categoria", porCategoria,
            "por_mes", porMes
        ));
    }

    @GetMapping("/relatorio-historico-cliente")
    public ResponseEntity<?> relatorioHistoricoCliente(@RequestParam Long cliente_id) {
        Long empresaId = tenantService.getEmpresaId();
        Cliente cliente = clienteRepo.findById(cliente_id)
            .filter(c -> empresaId == null || empresaId.equals(c.getEmpresa() != null ? c.getEmpresa().getId() : null))
            .orElse(null);
        if (cliente == null) return ResponseEntity.notFound().build();

        List<OrdemServico> ordens = osRepo.findByEmpresaId(empresaId).stream()
            .filter(o -> o.getCliente() != null && o.getCliente().getId().equals(cliente_id))
            .toList();

        BigDecimal totalGasto = ordens.stream()
            .filter(o -> List.of("faturado","entregue").contains(o.getStatus()))
            .map(OrdemServico::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> ordensResult = ordens.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("numero", o.getNumero());
            m.put("status", o.getStatus());
            m.put("valor_total", o.getValorTotal());
            m.put("data_abertura", o.getDataAbertura() != null ? o.getDataAbertura().toString() : null);
            m.put("data_conclusao", o.getDataConclusao() != null ? o.getDataConclusao().toString() : null);
            m.put("modelo", o.getModelo());
            m.put("defeito_relatado", o.getDefeitoRelatado());
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cliente", Map.of(
            "id", cliente.getId(),
            "nome", cliente.getNome(),
            "telefone", cliente.getTelefone() != null ? cliente.getTelefone() : "",
            "email", cliente.getEmail() != null ? cliente.getEmail() : ""
        ));
        result.put("ordens", ordensResult);
        result.put("total_os", ordens.size());
        result.put("total_gasto", totalGasto);
        return ResponseEntity.ok(result);
    }
}
