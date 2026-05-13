package com.refricenter.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "abertura_caixa")
public class AberturaCaixa {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura = LocalDateTime.now();

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    @Column(name = "saldo_abertura", precision = 10, scale = 2)
    private BigDecimal saldoAbertura = BigDecimal.ZERO;

    @Column(name = "saldo_fechamento", precision = 10, scale = 2)
    private BigDecimal saldoFechamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_abertura_id")
    private Usuario usuarioAbertura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_fechamento_id")
    private Usuario usuarioFechamento;

    @Column(columnDefinition = "TEXT")
    private String observacoes = "";

    public boolean isEstaAberto() {
        return dataFechamento == null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDateTime dataAbertura) { this.dataAbertura = dataAbertura; }
    public LocalDateTime getDataFechamento() { return dataFechamento; }
    public void setDataFechamento(LocalDateTime dataFechamento) { this.dataFechamento = dataFechamento; }
    public BigDecimal getSaldoAbertura() { return saldoAbertura; }
    public void setSaldoAbertura(BigDecimal saldoAbertura) { this.saldoAbertura = saldoAbertura; }
    public BigDecimal getSaldoFechamento() { return saldoFechamento; }
    public void setSaldoFechamento(BigDecimal saldoFechamento) { this.saldoFechamento = saldoFechamento; }
    public Usuario getUsuarioAbertura() { return usuarioAbertura; }
    public void setUsuarioAbertura(Usuario usuarioAbertura) { this.usuarioAbertura = usuarioAbertura; }
    public Usuario getUsuarioFechamento() { return usuarioFechamento; }
    public void setUsuarioFechamento(Usuario usuarioFechamento) { this.usuarioFechamento = usuarioFechamento; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
