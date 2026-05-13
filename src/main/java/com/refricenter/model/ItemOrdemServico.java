package com.refricenter.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "item_ordem_servico")
public class ItemOrdemServico {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade = 1;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "aguardando_estoque")
    private Boolean aguardandoEstoque = false;

    @Column(name = "status_compra", length = 15)
    private String statusCompra = "pendente";

    public BigDecimal getSubtotal() {
        if (quantidade == null || precoUnitario == null) return BigDecimal.ZERO;
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrdemServico getOrdemServico() { return ordemServico; }
    public void setOrdemServico(OrdemServico ordemServico) { this.ordemServico = ordemServico; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
    public Boolean getAguardandoEstoque() { return aguardandoEstoque; }
    public void setAguardandoEstoque(Boolean aguardandoEstoque) { this.aguardandoEstoque = aguardandoEstoque; }
    public String getStatusCompra() { return statusCompra; }
    public void setStatusCompra(String statusCompra) { this.statusCompra = statusCompra; }
}
