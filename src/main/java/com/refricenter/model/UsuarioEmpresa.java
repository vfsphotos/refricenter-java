package com.refricenter.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "usuario_empresa")
public class UsuarioEmpresa {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private Usuario user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    private Setor setor;

    @Column(nullable = false, length = 20)
    private String papel = "tecnico";

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    public boolean isDono() {
        return "dono".equals(papel) || (user != null && empresa != null && user.getId().equals(empresa.getDono().getId()));
    }

    public Map<String, Map<String, Boolean>> getPermissoes() {
        Map<String, Map<String, Boolean>> perms = new HashMap<>();
        if (isDono()) {
            String[] modulos = {"dashboard","clientes","ordens_servico","estoque","financeiro","whatsapp","relatorios","backup","configuracoes"};
            for (String m : modulos) {
                perms.put(m, Map.of("visualizar", true, "criar", true, "editar", true, "deletar", true));
            }
            return perms;
        }
        if (setor == null) return perms;
        for (PermissaoSetor p : setor.getPermissoes()) {
            perms.put(p.getModulo(), Map.of(
                "visualizar", p.getPodeVisualizar(),
                "criar",      p.getPodeCriar(),
                "editar",     p.getPodeEditar(),
                "deletar",    p.getPodeDeletar()
            ));
        }
        return perms;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUser() { return user; }
    public void setUser(Usuario user) { this.user = user; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Setor getSetor() { return setor; }
    public void setSetor(Setor setor) { this.setor = setor; }
    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
}
