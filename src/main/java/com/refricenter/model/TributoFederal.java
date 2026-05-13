package com.refricenter.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tributo_federal")
public class TributoFederal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 20)
    private String categoria;

    @Column(length = 30)
    private String competencia = "";

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento_programada")
    private LocalDate dataPagamentoProgramada;

    @Column(name = "data_pagamento_realizado")
    private LocalDate dataPagamentoRealizado;

    @Column(name = "boleto_path")
    private String boletoPath;

    @Column(name = "comprovante_path")
    private String comprovantePath;

    @Column(columnDefinition = "TEXT")
    private String observacoes = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lancamento_financeiro_id")
    private LancamentoFinanceiro lancamentoFinanceiro;

    public String getStatus() {
        LocalDate hoje = LocalDate.now();
        if (dataPagamentoRealizado != null) return "pago";
        if (dataPagamentoProgramada != null && !dataPagamentoProgramada.isBefore(hoje)) return "agendado";
        if (dataVencimento.isBefore(hoje)) return "vencido";
        return "pendente";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getCompetencia() { return competencia; }
    public void setCompetencia(String competencia) { this.competencia = competencia; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }
    public LocalDate getDataPagamentoProgramada() { return dataPagamentoProgramada; }
    public void setDataPagamentoProgramada(LocalDate dataPagamentoProgramada) { this.dataPagamentoProgramada = dataPagamentoProgramada; }
    public LocalDate getDataPagamentoRealizado() { return dataPagamentoRealizado; }
    public void setDataPagamentoRealizado(LocalDate dataPagamentoRealizado) { this.dataPagamentoRealizado = dataPagamentoRealizado; }
    public String getBoletoPath() { return boletoPath; }
    public void setBoletoPath(String boletoPath) { this.boletoPath = boletoPath; }
    public String getComprovantePath() { return comprovantePath; }
    public void setComprovantePath(String comprovantePath) { this.comprovantePath = comprovantePath; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
    public LancamentoFinanceiro getLancamentoFinanceiro() { return lancamentoFinanceiro; }
    public void setLancamentoFinanceiro(LancamentoFinanceiro lancamentoFinanceiro) { this.lancamentoFinanceiro = lancamentoFinanceiro; }
}
