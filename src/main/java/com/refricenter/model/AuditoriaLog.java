package com.refricenter.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_log", indexes = {
    @Index(columnList = "empresa_id, data"),
    @Index(columnList = "modelo, acao")
})
public class AuditoriaLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String acao;

    @Column(length = 50)
    private String modelo = "";

    @Column(name = "objeto_id", length = 50)
    private String objetoId = "";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "dados_extras", columnDefinition = "jsonb")
    private String dadosExtras;

    @Column(length = 50)
    private String ip = "";

    @Column(nullable = false, updatable = false)
    private LocalDateTime data = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getObjetoId() { return objetoId; }
    public void setObjetoId(String objetoId) { this.objetoId = objetoId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getDadosExtras() { return dadosExtras; }
    public void setDadosExtras(String dadosExtras) { this.dadosExtras = dadosExtras; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }
}
