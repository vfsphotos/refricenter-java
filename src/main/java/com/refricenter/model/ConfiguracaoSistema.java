package com.refricenter.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracao_sistema")
public class ConfiguracaoSistema {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", unique = true)
    private Empresa empresa;

    @Column(name = "nome_empresa", length = 200)
    private String nomeEmpresa = "MagnoosSystem";

    @Column(name = "cnpj_empresa", length = 18)
    private String cnpjEmpresa = "";

    @Column(length = 300)
    private String endereco = "";

    @Column(name = "numero_endereco", length = 10)
    private String numeroEndereco = "";

    @Column(length = 100)
    private String bairro = "";

    @Column(length = 100)
    private String cidade = "";

    @Column(length = 2)
    private String estado = "";

    @Column(length = 10)
    private String cep = "";

    @Column(length = 20)
    private String telefone = "";

    @Column(length = 254)
    private String email = "";

    @Column(name = "horario_funcionamento", length = 200)
    private String horarioFuncionamento = "";

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "observacoes_nota", columnDefinition = "TEXT")
    private String observacoesNota = "";

    @Column(name = "mensagem_boas_vindas", columnDefinition = "TEXT")
    private String mensagemBoasVindas = "Bem-vindo ao MagnoosSystem!";

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao = LocalDateTime.now();

    @Column(name = "numero_inicial_os")
    private Integer numeroInicialOs = 1;

    @Column(name = "ultima_os")
    private Integer ultimaOs = 0;

    @Column(name = "caixa_inicial", precision = 10, scale = 2)
    private BigDecimal caixaInicial = BigDecimal.ZERO;

    public String getTelefoneFormatado() {
        if (telefone == null || telefone.isEmpty()) return "";
        String digits = telefone.replaceAll("\\D", "");
        if (digits.length() == 11) return "(" + digits.substring(0,2) + ")" + digits.substring(2,7) + "-" + digits.substring(7);
        if (digits.length() == 10) return "(" + digits.substring(0,2) + ")" + digits.substring(2,6) + "-" + digits.substring(6);
        return telefone;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getNomeEmpresa() { return nomeEmpresa; }
    public void setNomeEmpresa(String nomeEmpresa) { this.nomeEmpresa = nomeEmpresa; }
    public String getCnpjEmpresa() { return cnpjEmpresa; }
    public void setCnpjEmpresa(String cnpjEmpresa) { this.cnpjEmpresa = cnpjEmpresa; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getNumeroEndereco() { return numeroEndereco; }
    public void setNumeroEndereco(String numeroEndereco) { this.numeroEndereco = numeroEndereco; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getHorarioFuncionamento() { return horarioFuncionamento; }
    public void setHorarioFuncionamento(String horarioFuncionamento) { this.horarioFuncionamento = horarioFuncionamento; }
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
    public String getObservacoesNota() { return observacoesNota; }
    public void setObservacoesNota(String observacoesNota) { this.observacoesNota = observacoesNota; }
    public String getMensagemBoasVindas() { return mensagemBoasVindas; }
    public void setMensagemBoasVindas(String mensagemBoasVindas) { this.mensagemBoasVindas = mensagemBoasVindas; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
    public Integer getNumeroInicialOs() { return numeroInicialOs; }
    public void setNumeroInicialOs(Integer numeroInicialOs) { this.numeroInicialOs = numeroInicialOs; }
    public Integer getUltimaOs() { return ultimaOs; }
    public void setUltimaOs(Integer ultimaOs) { this.ultimaOs = ultimaOs; }
    public BigDecimal getCaixaInicial() { return caixaInicial; }
    public void setCaixaInicial(BigDecimal caixaInicial) { this.caixaInicial = caixaInicial; }
}
