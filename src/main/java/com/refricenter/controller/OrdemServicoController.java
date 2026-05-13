package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.OrdemServicoService;
import com.refricenter.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class OrdemServicoController {

    private final OrdemServicoRepository osRepo;
    private final ItemOrdemServicoRepository itemRepo;
    private final ClienteRepository clienteRepo;
    private final OrdemServicoService osService;
    private final TenantService tenantService;

    public OrdemServicoController(OrdemServicoRepository osRepo, ItemOrdemServicoRepository itemRepo,
                                   ClienteRepository clienteRepo, OrdemServicoService osService,
                                   TenantService tenantService) {
        this.osRepo = osRepo;
        this.itemRepo = itemRepo;
        this.clienteRepo = clienteRepo;
        this.osService = osService;
        this.tenantService = tenantService;
    }

    @GetMapping("/ordens-servico")
    public ResponseEntity<?> listar(@RequestParam(required = false) String status,
                                     @RequestParam(required = false) String search) {
        Long empresaId = tenantService.getEmpresaId();
        List<OrdemServico> ordens = empresaId != null
            ? osRepo.findByEmpresaId(empresaId) : osRepo.findAll();

        if (status != null && !status.isBlank()) {
            ordens = ordens.stream().filter(o -> status.equals(o.getStatus())).toList();
        }
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            ordens = ordens.stream().filter(o ->
                (o.getNumero() != null && o.getNumero().toLowerCase().contains(q)) ||
                (o.getCliente() != null && o.getCliente().getNome().toLowerCase().contains(q))
            ).toList();
        }
        return ResponseEntity.ok(ordens.stream().map(this::serialize).toList());
    }

    @PostMapping("/ordens-servico")
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null && !tenantService.isSuperAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error","Sem vínculo com empresa."));
        }
        Long clienteId = data.get("cliente") != null ? Long.parseLong(data.get("cliente").toString()) : null;
        if (clienteId == null) return ResponseEntity.badRequest().body(Map.of("error","Cliente é obrigatório."));

        Cliente cliente = clienteRepo.findById(clienteId).orElse(null);
        if (cliente == null) return ResponseEntity.badRequest().body(Map.of("error","Cliente não encontrado."));

        OrdemServico os = new OrdemServico();
        os.setCliente(cliente);
        if (empresaId != null) {
            Empresa emp = new Empresa(); emp.setId(empresaId); os.setEmpresa(emp);
        }
        fromData(data, os);
        OrdemServico saved = osService.criarOrdemServico(empresaId, os);
        return ResponseEntity.status(201).body(serialize(saved));
    }

    @GetMapping("/ordens-servico/{id}")
    public ResponseEntity<?> detalhe(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        return osRepo.findById(id)
            .filter(o -> empresaId == null || empresaId.equals(o.getEmpresa() != null ? o.getEmpresa().getId() : null))
            .map(o -> ResponseEntity.ok(serializeDetalhado(o)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/ordens-servico/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        OrdemServico os = osRepo.findById(id)
            .filter(o -> empresaId == null || empresaId.equals(o.getEmpresa() != null ? o.getEmpresa().getId() : null))
            .orElse(null);
        if (os == null) return ResponseEntity.notFound().build();
        fromData(data, os);
        osRepo.save(os);
        return ResponseEntity.ok(serializeDetalhado(os));
    }

    @PatchMapping("/ordens-servico/{id}")
    public ResponseEntity<?> patch(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return atualizar(id, data);
    }

    @DeleteMapping("/ordens-servico/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        OrdemServico os = osRepo.findById(id)
            .filter(o -> empresaId == null || empresaId.equals(o.getEmpresa() != null ? o.getEmpresa().getId() : null))
            .orElse(null);
        if (os == null) return ResponseEntity.notFound().build();
        os.setStatus("cancelada");
        osRepo.save(os);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/adicionar-item-os")
    public ResponseEntity<?> adicionarItem(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null && !tenantService.isSuperAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error","Sem vínculo com empresa."));
        }
        Long osId = Long.parseLong(data.get("ordem_servico_id").toString());
        Long produtoId = Long.parseLong(data.get("produto_id").toString());
        int qtd = Integer.parseInt(data.getOrDefault("quantidade", 1).toString());
        try {
            ItemOrdemServico item = osService.adicionarItem(empresaId, osId, produtoId, qtd, tenantService.currentUser());
            return ResponseEntity.status(201).body(serializeItem(item));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/os-pagamento-prazo")
    public ResponseEntity<?> osPagamentoPrazo() {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        var ordens = osRepo.findByEmpresaIdAndPagamentoPrazoTrueAndValorRestantePrazoGreaterThan(
            empresaId, BigDecimal.ZERO);
        java.time.LocalDate hoje = java.time.LocalDate.now();
        List<Map<String, Object>> result = ordens.stream().map(os -> {
            java.time.LocalDate venc = os.getDataVencimentoPrazo();
            String alerta = "sem_data";
            if (venc != null) {
                long delta = java.time.temporal.ChronoUnit.DAYS.between(hoje, venc);
                alerta = delta < 0 ? "atrasado" : delta == 0 ? "hoje" : delta <= 3 ? "proximo" : "ok";
            }
            return Map.of(
                "id", os.getId(), "numero", os.getNumero(),
                "cliente", os.getCliente() != null ? os.getCliente().getNome() : "",
                "valor_restante", os.getValorRestantePrazo(),
                "data_vencimento", venc != null ? venc.toString() : null,
                "alerta", alerta, "status", os.getStatus()
            );
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pendencias-suprimentos")
    public ResponseEntity<?> pendenciasSuprimentos() {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));

        List<ItemOrdemServico> itensAguardando = itemRepo.findByOrdemServicoEmpresaIdAndAguardandoEstoqueTrue(empresaId);
        Set<Long> osIds = new HashSet<>();
        itensAguardando.forEach(i -> osIds.add(i.getOrdemServico().getId()));

        List<OrdemServico> ordens = osRepo.findByEmpresaIdAndStatusIn(empresaId, List.of("aguardando_peca"));
        ordens.forEach(o -> osIds.add(o.getId()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long osId : osIds) {
            osRepo.findById(osId).ifPresent(os -> {
                List<Map<String, Object>> itensPendentes = os.getItens().stream()
                    .filter(ItemOrdemServico::getAguardandoEstoque)
                    .filter(i -> i.getProduto() != null)
                    .map(i -> Map.of(
                        "item_id", i.getId(),
                        "produto_id", i.getProduto().getId(),
                        "produto_nome", i.getProduto().getNome(),
                        "produto_codigo", i.getProduto().getCodigo(),
                        "quantidade", i.getQuantidade(),
                        "estoque_atual", i.getProduto().getEstoqueAtual(),
                        "status_compra", i.getStatusCompra()
                    )).toList();

                result.add(Map.of(
                    "os_id", os.getId(), "numero", os.getNumero(),
                    "cliente", os.getCliente() != null ? os.getCliente().getNome() : "",
                    "status", os.getStatus(),
                    "data_abertura", os.getDataAbertura() != null ? os.getDataAbertura().toLocalDate().toString() : "",
                    "itens_pendentes", itensPendentes,
                    "total_para_pedido", itensPendentes.stream().filter(i -> "pendente".equals(i.get("status_compra"))).count(),
                    "total_em_transito", itensPendentes.stream().filter(i -> "em_transito".equals(i.get("status_compra"))).count()
                ));
            });
        }
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/item-os/{itemId}/status-compra")
    public ResponseEntity<?> atualizarStatusCompra(@PathVariable Long itemId, @RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        String novoStatus = data.get("status_compra") != null ? data.get("status_compra").toString() : null;
        if (!List.of("pendente","em_transito","recebido").contains(novoStatus)) {
            return ResponseEntity.badRequest().body(Map.of("error","status_compra inválido."));
        }
        ItemOrdemServico item = itemRepo.findById(itemId)
            .filter(i -> empresaId == null || empresaId.equals(
                i.getOrdemServico().getEmpresa() != null ? i.getOrdemServico().getEmpresa().getId() : null))
            .orElse(null);
        if (item == null) return ResponseEntity.notFound().build();
        item.setStatusCompra(novoStatus);
        if ("recebido".equals(novoStatus)) item.setAguardandoEstoque(false);
        itemRepo.save(item);
        return ResponseEntity.ok(Map.of("ok", true, "item_id", item.getId(), "status_compra", item.getStatusCompra()));
    }

    private void fromData(Map<String, Object> data, OrdemServico os) {
        if (data.containsKey("status")) os.setStatus(data.get("status").toString());
        if (data.containsKey("prioridade")) os.setPrioridade(data.get("prioridade").toString());
        if (data.containsKey("tipo_ferramenta")) os.setTipoFerramenta(data.get("tipo_ferramenta").toString());
        if (data.containsKey("marca")) os.setMarca(data.get("marca").toString());
        if (data.containsKey("modelo")) os.setModelo(data.get("modelo").toString());
        if (data.containsKey("codigo_identificacao")) os.setCodigoIdentificacao(data.get("codigo_identificacao").toString());
        if (data.containsKey("defeito_relatado")) os.setDefeitoRelatado(data.get("defeito_relatado").toString());
        if (data.containsKey("descricao_servico")) os.setDescricaoServico(data.get("descricao_servico").toString());
        if (data.containsKey("solucao_aplicada")) os.setSolucaoAplicada(data.get("solucao_aplicada").toString());
        if (data.containsKey("mao_obra")) os.setMaoObra(new BigDecimal(data.get("mao_obra").toString()));
        if (data.containsKey("taxa_analise")) os.setTaxaAnalise(new BigDecimal(data.get("taxa_analise").toString()));
        if (data.containsKey("desconto")) os.setDesconto(new BigDecimal(data.get("desconto").toString()));
        if (data.containsKey("observacoes")) os.setObservacoes(data.get("observacoes").toString());
        if (data.containsKey("forma_pagamento")) os.setFormaPagamento(data.get("forma_pagamento").toString());
        if (data.containsKey("pago_50_percent")) os.setPago50Percent(Boolean.parseBoolean(data.get("pago_50_percent").toString()));
        if (data.containsKey("pagamento_prazo")) os.setPagamentoPrazo(Boolean.parseBoolean(data.get("pagamento_prazo").toString()));
        if (data.containsKey("valor_restante_prazo")) os.setValorRestantePrazo(new BigDecimal(data.get("valor_restante_prazo").toString()));
    }

    private Map<String, Object> serialize(OrdemServico os) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", os.getId());
        m.put("numero", os.getNumero());
        m.put("status", os.getStatus());
        m.put("prioridade", os.getPrioridade());
        m.put("cliente", os.getCliente() != null ? Map.of("id", os.getCliente().getId(), "nome", os.getCliente().getNome()) : null);
        m.put("tipo_ferramenta", os.getTipoFerramenta());
        m.put("marca", os.getMarca());
        m.put("modelo", os.getModelo());
        m.put("mao_obra", os.getMaoObra());
        m.put("taxa_analise", os.getTaxaAnalise());
        m.put("desconto", os.getDesconto());
        m.put("valor_total", os.getValorTotal());
        m.put("data_abertura", os.getDataAbertura() != null ? os.getDataAbertura().toString() : null);
        m.put("empresa", os.getEmpresa() != null ? os.getEmpresa().getId() : null);
        return m;
    }

    private Map<String, Object> serializeDetalhado(OrdemServico os) {
        Map<String, Object> m = serialize(os);
        m.put("defeito_relatado", os.getDefeitoRelatado());
        m.put("descricao_servico", os.getDescricaoServico());
        m.put("solucao_aplicada", os.getSolucaoAplicada());
        m.put("observacoes", os.getObservacoes());
        m.put("codigo_identificacao", os.getCodigoIdentificacao());
        m.put("forma_pagamento", os.getFormaPagamento());
        m.put("pago_50_percent", os.getPago50Percent());
        m.put("pagamento_prazo", os.getPagamentoPrazo());
        m.put("valor_restante_prazo", os.getValorRestantePrazo());
        m.put("data_vencimento_prazo", os.getDataVencimentoPrazo() != null ? os.getDataVencimentoPrazo().toString() : null);
        m.put("data_prevista", os.getDataPrevista() != null ? os.getDataPrevista().toString() : null);
        m.put("data_conclusao", os.getDataConclusao() != null ? os.getDataConclusao().toString() : null);
        m.put("data_entrega", os.getDataEntrega() != null ? os.getDataEntrega().toString() : null);
        m.put("itens", os.getItens().stream().map(this::serializeItem).toList());
        m.put("valor_produtos", os.getValorProdutos());
        return m;
    }

    private Map<String, Object> serializeItem(ItemOrdemServico item) {
        return Map.of(
            "id", item.getId(),
            "produto", item.getProduto() != null ? Map.of(
                "id", item.getProduto().getId(),
                "nome", item.getProduto().getNome(),
                "codigo", item.getProduto().getCodigo()) : null,
            "quantidade", item.getQuantidade(),
            "preco_unitario", item.getPrecoUnitario(),
            "subtotal", item.getSubtotal(),
            "aguardando_estoque", item.getAguardandoEstoque(),
            "status_compra", item.getStatusCompra()
        );
    }
}
