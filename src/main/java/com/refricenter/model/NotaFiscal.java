package com.refricenter.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "nota_fiscal")
public class NotaFiscal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id")
    private OrdemServico ordemServico;

    @Column(nullable = false, length = 5)
    private String tipo = "nfce";

    @Column(nullable = false, length = 20)
    private String status = "rascunho";

    @Column(length = 20)
    private String numero = "";

    @Column(length = 5)
    private String serie = "1";

    @Column(length = 50)
    private String chave = "";

    @Column(name = "numero_ref", length = 100)
    private String numeroRef = "";

    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String descricao = "";

    @Column(name = "resposta_focus", columnDefinition = "jsonb")
    private String respostaFocus;

    @Column(name = "caminho_xml", length = 500)
    private String caminhoXml = "";

    @Column(name = "caminho_danfe", length = 500)
    private String caminhoDanfe = "";

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro = "";

    @Column(name = "data_emissao", nullable = false, updatable = false)
    private LocalDateTime dataEmissao = LocalDateTime.now();

    @Column(name = "data_autorizacao")
    private LocalDateTime dataAutorizacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public OrdemServico getOrdemServico() { return ordemServico; }
    public void setOrdemServico(OrdemServico ordemServico) { this.ordemServico = ordemServico; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }
    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
    public String getNumeroRef() { return numeroRef; }
    public void setNumeroRef(String numeroRef) { this.numeroRef = numeroRef; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getRespostaFocus() { return respostaFocus; }
    public void setRespostaFocus(String respostaFocus) { this.respostaFocus = respostaFocus; }
    public String getCaminhoXml() { return caminhoXml; }
    public void setCaminhoXml(String caminhoXml) { this.caminhoXml = caminhoXml; }
    public String getCaminhoDanfe() { return caminhoDanfe; }
    public void setCaminhoDanfe(String caminhoDanfe) { this.caminhoDanfe = caminhoDanfe; }
    public String getMensagemErro() { return mensagemErro; }
    public void setMensagemErro(String mensagemErro) { this.mensagemErro = mensagemErro; }
    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }
    public LocalDateTime getDataAutorizacao() { return dataAutorizacao; }
    public void setDataAutorizacao(LocalDateTime dataAutorizacao) { this.dataAutorizacao = dataAutorizacao; }
}
