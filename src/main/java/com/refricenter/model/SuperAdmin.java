package com.refricenter.model;

import jakarta.persistence.*;

@Entity
@Table(name = "super_admin")
public class SuperAdmin {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private Usuario user;

    @Column(nullable = false, length = 20)
    private String nivel = "suporte";

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "pode_impersonar", nullable = false)
    private Boolean podeImpersonar = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUser() { return user; }
    public void setUser(Usuario user) { this.user = user; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public Boolean getPodeImpersonar() { return podeImpersonar; }
    public void setPodeImpersonar(Boolean podeImpersonar) { this.podeImpersonar = podeImpersonar; }
}
