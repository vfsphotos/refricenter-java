package com.refricenter.service;

import com.refricenter.model.Usuario;
import com.refricenter.model.UsuarioEmpresa;
import com.refricenter.repository.UsuarioEmpresaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TenantService {

    private final UsuarioEmpresaRepository usuarioEmpresaRepository;

    public TenantService(UsuarioEmpresaRepository usuarioEmpresaRepository) {
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
    }

    public Usuario currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Usuario) auth.getPrincipal();
    }

    public Optional<UsuarioEmpresa> currentUsuarioEmpresa() {
        return usuarioEmpresaRepository.findByUserId(currentUser().getId());
    }

    /** Retorna empresa_id do usuário logado. Null se superadmin sem empresa. -1 se erro. */
    public Long getEmpresaId() {
        return currentUsuarioEmpresa().map(ue -> ue.getEmpresa().getId()).orElse(null);
    }

    public boolean isSuperAdmin() {
        Usuario u = currentUser();
        return u.getSuperAdmin() != null && Boolean.TRUE.equals(u.getSuperAdmin().getAtivo());
    }
}
