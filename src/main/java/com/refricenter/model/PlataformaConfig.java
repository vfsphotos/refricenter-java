package com.refricenter.model;

import jakarta.persistence.*;

@Entity
@Table(name = "plataforma_config")
public class PlataformaConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manutencao_ativa", nullable = false)
    private Boolean manutencaoAtiva = false;

    @Column(name = "mensagem_manutencao", columnDefinition = "TEXT")
    private String mensagemManutencao = "Sistema em manutenção.";

    @Column(name = "nome_plataforma", length = 100)
    private String nomePlataforma = "RefriCenter SaaS";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Boolean getManutencaoAtiva() { return manutencaoAtiva; }
    public void setManutencaoAtiva(Boolean manutencaoAtiva) { this.manutencaoAtiva = manutencaoAtiva; }
    public String getMensagemManutencao() { return mensagemManutencao; }
    public void setMensagemManutencao(String mensagemManutencao) { this.mensagemManutencao = mensagemManutencao; }
    public String getNomePlataforma() { return nomePlataforma; }
    public void setNomePlataforma(String nomePlataforma) { this.nomePlataforma = nomePlataforma; }
}
