package com.refricenter.model;

import jakarta.persistence.*;

@Entity
@Table(name = "permissao_setor", uniqueConstraints = @UniqueConstraint(columnNames = {"setor_id", "modulo"}))
public class PermissaoSetor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id", nullable = false)
    private Setor setor;

    @Column(nullable = false, length = 30)
    private String modulo;

    @Column(name = "pode_visualizar", nullable = false)
    private Boolean podeVisualizar = true;

    @Column(name = "pode_criar", nullable = false)
    private Boolean podeCriar = false;

    @Column(name = "pode_editar", nullable = false)
    private Boolean podeEditar = false;

    @Column(name = "pode_deletar", nullable = false)
    private Boolean podeDeletar = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Setor getSetor() { return setor; }
    public void setSetor(Setor setor) { this.setor = setor; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public Boolean getPodeVisualizar() { return podeVisualizar; }
    public void setPodeVisualizar(Boolean podeVisualizar) { this.podeVisualizar = podeVisualizar; }
    public Boolean getPodeCriar() { return podeCriar; }
    public void setPodeCriar(Boolean podeCriar) { this.podeCriar = podeCriar; }
    public Boolean getPodeEditar() { return podeEditar; }
    public void setPodeEditar(Boolean podeEditar) { this.podeEditar = podeEditar; }
    public Boolean getPodeDeletar() { return podeDeletar; }
    public void setPodeDeletar(Boolean podeDeletar) { this.podeDeletar = podeDeletar; }
}
