package com.refricenter.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipamento")
public class Equipamento {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marca_id")
    private Marca marca;

    @Column(nullable = false, length = 200)
    private String modelo;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie = "";

    @Column(length = 50)
    private String potencia = "";

    @Column(length = 10)
    private String tensao = "";

    @Column(name = "defeito_reclamado", nullable = false, columnDefinition = "TEXT")
    private String defeitoReclamado;

    @Column(columnDefinition = "TEXT")
    private String acessorios = "";

    @Column(name = "estado_conserto", length = 20)
    private String estadoConserto = "recebido";

    @Column(name = "observacoes_tecnicas", columnDefinition = "TEXT")
    private String observacoesTecnicas = "";

    @Column(name = "data_recebimento", nullable = false)
    private LocalDateTime dataRecebimento = LocalDateTime.now();

    @Column(name = "data_prevista_entrega")
    private LocalDateTime dataPrevistaEntrega;

    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    @Column(name = "foto_path")
    private String fotoPath;

    @Column(nullable = false)
    private Boolean ativo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Marca getMarca() { return marca; }
    public void setMarca(Marca marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public String getPotencia() { return potencia; }
    public void setPotencia(String potencia) { this.potencia = potencia; }
    public String getTensao() { return tensao; }
    public void setTensao(String tensao) { this.tensao = tensao; }
    public String getDefeitoReclamado() { return defeitoReclamado; }
    public void setDefeitoReclamado(String defeitoReclamado) { this.defeitoReclamado = defeitoReclamado; }
    public String getAcessorios() { return acessorios; }
    public void setAcessorios(String acessorios) { this.acessorios = acessorios; }
    public String getEstadoConserto() { return estadoConserto; }
    public void setEstadoConserto(String estadoConserto) { this.estadoConserto = estadoConserto; }
    public String getObservacoesTecnicas() { return observacoesTecnicas; }
    public void setObservacoesTecnicas(String observacoesTecnicas) { this.observacoesTecnicas = observacoesTecnicas; }
    public LocalDateTime getDataRecebimento() { return dataRecebimento; }
    public void setDataRecebimento(LocalDateTime dataRecebimento) { this.dataRecebimento = dataRecebimento; }
    public LocalDateTime getDataPrevistaEntrega() { return dataPrevistaEntrega; }
    public void setDataPrevistaEntrega(LocalDateTime dataPrevistaEntrega) { this.dataPrevistaEntrega = dataPrevistaEntrega; }
    public LocalDateTime getDataEntrega() { return dataEntrega; }
    public void setDataEntrega(LocalDateTime dataEntrega) { this.dataEntrega = dataEntrega; }
    public String getFotoPath() { return fotoPath; }
    public void setFotoPath(String fotoPath) { this.fotoPath = fotoPath; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
