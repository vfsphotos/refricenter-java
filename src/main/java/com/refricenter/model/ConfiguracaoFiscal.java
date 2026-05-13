package com.refricenter.model;

import jakarta.persistence.*;

@Entity
@Table(name = "configuracao_fiscal")
public class ConfiguracaoFiscal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", unique = true)
    private Empresa empresa;

    @Column(length = 300)
    private String token = "";

    @Column(length = 1)
    private String ambiente = "2";

    @Column(name = "cnpj_emitente", length = 18)
    private String cnpjEmitente = "";

    @Column(name = "razao_social", length = 200)
    private String razaoSocial = "";

    @Column(name = "nome_fantasia", length = 200)
    private String nomeFantasia = "";

    @Column(name = "inscricao_estadual", length = 30)
    private String inscricaoEstadual = "";

    @Column(name = "inscricao_municipal", length = 30)
    private String inscricaoMunicipal = "";

    @Column(name = "regime_tributario", length = 1)
    private String regimeTributario = "1";

    @Column(name = "csc_id", length = 10)
    private String cscId = "";

    @Column(name = "csc_token", length = 100)
    private String cscToken = "";

    @Column(nullable = false)
    private Boolean ativo = false;

    public String getBaseUrl() {
        return "1".equals(ambiente) ? "https://api.focusnfe.com.br" : "https://homologacao.focusnfe.com.br";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getAmbiente() { return ambiente; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }
    public String getCnpjEmitente() { return cnpjEmitente; }
    public void setCnpjEmitente(String cnpjEmitente) { this.cnpjEmitente = cnpjEmitente; }
    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }
    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }
    public String getInscricaoEstadual() { return inscricaoEstadual; }
    public void setInscricaoEstadual(String inscricaoEstadual) { this.inscricaoEstadual = inscricaoEstadual; }
    public String getInscricaoMunicipal() { return inscricaoMunicipal; }
    public void setInscricaoMunicipal(String inscricaoMunicipal) { this.inscricaoMunicipal = inscricaoMunicipal; }
    public String getRegimeTributario() { return regimeTributario; }
    public void setRegimeTributario(String regimeTributario) { this.regimeTributario = regimeTributario; }
    public String getCscId() { return cscId; }
    public void setCscId(String cscId) { this.cscId = cscId; }
    public String getCscToken() { return cscToken; }
    public void setCscToken(String cscToken) { this.cscToken = cscToken; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
