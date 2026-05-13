package com.refricenter.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordem_servico", indexes = {
    @Index(columnList = "status"),
    @Index(columnList = "data_abertura"),
    @Index(columnList = "numero"),
    @Index(columnList = "cliente_id"),
    @Index(columnList = "empresa_id")
})
public class OrdemServico {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(unique = true, nullable = false, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id")
    private Equipamento equipamento;

    @Column(name = "tipo_ferramenta", length = 100)
    private String tipoFerramenta = "";

    @Column(length = 100)
    private String marca = "";

    @Column(length = 100)
    private String modelo = "";

    @Column(name = "codigo_identificacao", length = 100)
    private String codigoIdentificacao = "";

    @Column(name = "defeito_relatado", columnDefinition = "TEXT")
    private String defeitoRelatado = "";

    @Column(nullable = false, length = 20)
    private String status = "orcamento";

    @Column(nullable = false, length = 10)
    private String prioridade = "media";

    @Column(name = "descricao_servico", columnDefinition = "TEXT")
    private String descricaoServico = "";

    @Column(name = "solucao_aplicada", columnDefinition = "TEXT")
    private String solucaoAplicada = "";

    @Column(name = "mao_obra", precision = 10, scale = 2)
    private BigDecimal maoObra = BigDecimal.ZERO;

    @Column(name = "taxa_analise", precision = 10, scale = 2)
    private BigDecimal taxaAnalise = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String observacoes = "";

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura = LocalDateTime.now();

    @Column(name = "data_prevista")
    private LocalDateTime dataPrevista;

    @Column(name = "data_aprovacao")
    private LocalDateTime dataAprovacao;

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    @Column(name = "data_saida")
    private LocalDateTime dataSaida;

    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    @Column(name = "data_faturamento")
    private LocalDateTime dataFaturamento;

    @Column(name = "pago_50_percent")
    private Boolean pago50Percent = false;

    @Column(name = "data_pagamento_50")
    private LocalDateTime dataPagamento50;

    @Column(name = "lancamento_faturamento")
    private Integer lancamentoFaturamento;

    @Column(name = "data_faturamento_total")
    private LocalDateTime dataFaturamentoTotal;

    @Column(name = "data_retirada")
    private LocalDateTime dataRetirada;

    @Column(name = "forma_pagamento", length = 20)
    private String formaPagamento = "";

    @Column(name = "pagamento_prazo")
    private Boolean pagamentoPrazo = false;

    @Column(name = "data_vencimento_prazo")
    private LocalDate dataVencimentoPrazo;

    @Column(name = "valor_restante_prazo", precision = 10, scale = 2)
    private BigDecimal valorRestantePrazo = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_responsavel_id")
    private Usuario tecnicoResponsavel;

    @OneToMany(mappedBy = "ordemServico", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ItemOrdemServico> itens = new ArrayList<>();

    public BigDecimal getValorTotal() {
        BigDecimal totalProdutos = itens.stream()
            .map(ItemOrdemServico::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalProdutos.add(maoObra).add(taxaAnalise).subtract(desconto);
    }

    public BigDecimal getValorProdutos() {
        return itens.stream().map(ItemOrdemServico::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Equipamento getEquipamento() { return equipamento; }
    public void setEquipamento(Equipamento equipamento) { this.equipamento = equipamento; }
    public String getTipoFerramenta() { return tipoFerramenta; }
    public void setTipoFerramenta(String tipoFerramenta) { this.tipoFerramenta = tipoFerramenta; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getCodigoIdentificacao() { return codigoIdentificacao; }
    public void setCodigoIdentificacao(String codigoIdentificacao) { this.codigoIdentificacao = codigoIdentificacao; }
    public String getDefeitoRelatado() { return defeitoRelatado; }
    public void setDefeitoRelatado(String defeitoRelatado) { this.defeitoRelatado = defeitoRelatado; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }
    public String getDescricaoServico() { return descricaoServico; }
    public void setDescricaoServico(String descricaoServico) { this.descricaoServico = descricaoServico; }
    public String getSolucaoAplicada() { return solucaoAplicada; }
    public void setSolucaoAplicada(String solucaoAplicada) { this.solucaoAplicada = solucaoAplicada; }
    public BigDecimal getMaoObra() { return maoObra; }
    public void setMaoObra(BigDecimal maoObra) { this.maoObra = maoObra; }
    public BigDecimal getTaxaAnalise() { return taxaAnalise; }
    public void setTaxaAnalise(BigDecimal taxaAnalise) { this.taxaAnalise = taxaAnalise; }
    public BigDecimal getDesconto() { return desconto; }
    public void setDesconto(BigDecimal desconto) { this.desconto = desconto; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDateTime dataAbertura) { this.dataAbertura = dataAbertura; }
    public LocalDateTime getDataPrevista() { return dataPrevista; }
    public void setDataPrevista(LocalDateTime dataPrevista) { this.dataPrevista = dataPrevista; }
    public LocalDateTime getDataAprovacao() { return dataAprovacao; }
    public void setDataAprovacao(LocalDateTime dataAprovacao) { this.dataAprovacao = dataAprovacao; }
    public LocalDateTime getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(LocalDateTime dataConclusao) { this.dataConclusao = dataConclusao; }
    public LocalDateTime getDataSaida() { return dataSaida; }
    public void setDataSaida(LocalDateTime dataSaida) { this.dataSaida = dataSaida; }
    public LocalDateTime getDataEntrega() { return dataEntrega; }
    public void setDataEntrega(LocalDateTime dataEntrega) { this.dataEntrega = dataEntrega; }
    public LocalDateTime getDataFaturamento() { return dataFaturamento; }
    public void setDataFaturamento(LocalDateTime dataFaturamento) { this.dataFaturamento = dataFaturamento; }
    public Boolean getPago50Percent() { return pago50Percent; }
    public void setPago50Percent(Boolean pago50Percent) { this.pago50Percent = pago50Percent; }
    public LocalDateTime getDataPagamento50() { return dataPagamento50; }
    public void setDataPagamento50(LocalDateTime dataPagamento50) { this.dataPagamento50 = dataPagamento50; }
    public Integer getLancamentoFaturamento() { return lancamentoFaturamento; }
    public void setLancamentoFaturamento(Integer lancamentoFaturamento) { this.lancamentoFaturamento = lancamentoFaturamento; }
    public LocalDateTime getDataFaturamentoTotal() { return dataFaturamentoTotal; }
    public void setDataFaturamentoTotal(LocalDateTime dataFaturamentoTotal) { this.dataFaturamentoTotal = dataFaturamentoTotal; }
    public LocalDateTime getDataRetirada() { return dataRetirada; }
    public void setDataRetirada(LocalDateTime dataRetirada) { this.dataRetirada = dataRetirada; }
    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }
    public Boolean getPagamentoPrazo() { return pagamentoPrazo; }
    public void setPagamentoPrazo(Boolean pagamentoPrazo) { this.pagamentoPrazo = pagamentoPrazo; }
    public LocalDate getDataVencimentoPrazo() { return dataVencimentoPrazo; }
    public void setDataVencimentoPrazo(LocalDate dataVencimentoPrazo) { this.dataVencimentoPrazo = dataVencimentoPrazo; }
    public BigDecimal getValorRestantePrazo() { return valorRestantePrazo; }
    public void setValorRestantePrazo(BigDecimal valorRestantePrazo) { this.valorRestantePrazo = valorRestantePrazo; }
    public Usuario getTecnicoResponsavel() { return tecnicoResponsavel; }
    public void setTecnicoResponsavel(Usuario tecnicoResponsavel) { this.tecnicoResponsavel = tecnicoResponsavel; }
    public List<ItemOrdemServico> getItens() { return itens; }
    public void setItens(List<ItemOrdemServico> itens) { this.itens = itens; }
}
