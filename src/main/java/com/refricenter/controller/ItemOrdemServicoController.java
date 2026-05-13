package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/itens-os")
public class ItemOrdemServicoController {

    private final ItemOrdemServicoRepository itemRepo;
    private final OrdemServicoRepository osRepo;
    private final ProdutoRepository produtoRepo;
    private final MovimentacaoEstoqueRepository movRepo;
    private final TenantService tenantService;

    public ItemOrdemServicoController(ItemOrdemServicoRepository itemRepo,
                                      OrdemServicoRepository osRepo,
                                      ProdutoRepository produtoRepo,
                                      MovimentacaoEstoqueRepository movRepo,
                                      TenantService tenantService) {
        this.itemRepo = itemRepo;
        this.osRepo = osRepo;
        this.produtoRepo = produtoRepo;
        this.movRepo = movRepo;
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Long ordem_servico_id) {
        Long empresaId = tenantService.getEmpresaId();
        List<ItemOrdemServico> itens;
        if (ordem_servico_id != null) {
            itens = itemRepo.findByOrdemServicoId(ordem_servico_id);
        } else {
            itens = itemRepo.findByOrdemServicoEmpresaId(empresaId);
        }
        return ResponseEntity.ok(itens.stream().map(this::serialize).toList());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        Long osId = Long.parseLong(data.get("ordem_servico_id").toString());
        int quantidade = data.containsKey("quantidade") ? Integer.parseInt(data.get("quantidade").toString()) : 1;

        OrdemServico os = osRepo.findByIdAndEmpresaId(osId, empresaId).orElse(null);
        if (os == null) return ResponseEntity.badRequest().body(Map.of("error", "OS não encontrada."));

        ItemOrdemServico item = new ItemOrdemServico();
        item.setOrdemServico(os);
        item.setQuantidade(quantidade);

        if (data.containsKey("produto_id") && data.get("produto_id") != null) {
            Long produtoId = Long.parseLong(data.get("produto_id").toString());
            Produto produto = produtoRepo.findByIdAndEmpresaId(produtoId, empresaId).orElse(null);
            if (produto == null) return ResponseEntity.badRequest().body(Map.of("error", "Produto não encontrado."));

            boolean aguardandoEstoque = data.containsKey("aguardando_estoque")
                && Boolean.parseBoolean(data.get("aguardando_estoque").toString());

            if (!aguardandoEstoque) {
                if (produto.getEstoqueAtual() < quantidade)
                    return ResponseEntity.badRequest().body(Map.of("error",
                        "Estoque insuficiente. Disponível: " + produto.getEstoqueAtual()));
                produto.setEstoqueAtual(produto.getEstoqueAtual() - quantidade);
                produtoRepo.save(produto);
                registrarMovimentacao(produto, "saida", quantidade, os);
            }

            item.setProduto(produto);
            item.setPrecoUnitario(produto.getPrecoVenda());
            item.setAguardandoEstoque(aguardandoEstoque);
        } else {
            BigDecimal preco = data.containsKey("preco_unitario")
                ? new BigDecimal(data.get("preco_unitario").toString()) : BigDecimal.ZERO;
            item.setPrecoUnitario(preco);
        }

        itemRepo.save(item);
        return ResponseEntity.status(201).body(serialize(item));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        ItemOrdemServico item = itemRepo.findById(id)
            .filter(i -> i.getOrdemServico() != null
                && empresaId.equals(i.getOrdemServico().getEmpresa() != null ? i.getOrdemServico().getEmpresa().getId() : null))
            .orElse(null);
        if (item == null) return ResponseEntity.notFound().build();

        if (data.containsKey("quantidade")) item.setQuantidade(Integer.parseInt(data.get("quantidade").toString()));
        if (data.containsKey("preco_unitario")) item.setPrecoUnitario(new BigDecimal(data.get("preco_unitario").toString()));
        if (data.containsKey("aguardando_estoque")) item.setAguardandoEstoque(Boolean.parseBoolean(data.get("aguardando_estoque").toString()));
        if (data.containsKey("status_compra")) item.setStatusCompra(data.get("status_compra").toString());
        itemRepo.save(item);
        return ResponseEntity.ok(serialize(item));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        ItemOrdemServico item = itemRepo.findById(id)
            .filter(i -> i.getOrdemServico() != null
                && empresaId.equals(i.getOrdemServico().getEmpresa() != null ? i.getOrdemServico().getEmpresa().getId() : null))
            .orElse(null);
        if (item == null) return ResponseEntity.notFound().build();

        if (item.getProduto() != null && !Boolean.TRUE.equals(item.getAguardandoEstoque())) {
            Produto produto = item.getProduto();
            produto.setEstoqueAtual(produto.getEstoqueAtual() + item.getQuantidade());
            produtoRepo.save(produto);
            registrarMovimentacao(produto, "entrada", item.getQuantidade(), item.getOrdemServico());
        }
        itemRepo.delete(item);
        return ResponseEntity.noContent().build();
    }

    private void registrarMovimentacao(Produto produto, String tipo, int quantidade, OrdemServico os) {
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setProduto(produto);
        mov.setTipo(tipo);
        mov.setQuantidade(quantidade);
        mov.setOrdemServico(os);
        mov.setObservacao(("saida".equals(tipo) ? "Saída para OS " : "Devolução de OS ") + os.getNumero());
        mov.setUsuario(tenantService.currentUser());
        movRepo.save(mov);
    }

    private Map<String, Object> serialize(ItemOrdemServico i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("ordem_servico_id", i.getOrdemServico() != null ? i.getOrdemServico().getId() : null);
        m.put("produto", i.getProduto() != null
            ? Map.of("id", i.getProduto().getId(), "nome", i.getProduto().getNome(), "codigo", i.getProduto().getCodigo())
            : null);
        m.put("quantidade", i.getQuantidade());
        m.put("preco_unitario", i.getPrecoUnitario());
        m.put("subtotal", i.getSubtotal());
        m.put("aguardando_estoque", i.getAguardandoEstoque());
        m.put("status_compra", i.getStatusCompra() != null ? i.getStatusCompra() : "pendente");
        return m;
    }
}
