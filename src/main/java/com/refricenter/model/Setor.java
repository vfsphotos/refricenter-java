package com.refricenter.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "setor", uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "nome"}))
public class Setor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 200)
    private String descricao = "";

    @Column(length = 7)
    private String cor = "#3b82f6";

    @Column(nullable = false)
    private Boolean ativo = true;

    @OneToMany(mappedBy = "setor", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PermissaoSetor> permissoes = new ArrayList<>();

    @OneToMany(mappedBy = "setor", fetch = FetchType.LAZY)
    private List<UsuarioEmpresa> usuarios = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public List<PermissaoSetor> getPermissoes() { return permissoes; }
    public void setPermissoes(List<PermissaoSetor> permissoes) { this.permissoes = permissoes; }
    public List<UsuarioEmpresa> getUsuarios() { return usuarios; }
}
