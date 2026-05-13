package com.refricenter.repository;

import com.refricenter.model.Empresa;
import com.refricenter.model.UsuarioEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long> {
    Optional<UsuarioEmpresa> findByUserId(Long userId);
    List<UsuarioEmpresa> findByEmpresa(Empresa empresa);
    List<UsuarioEmpresa> findByEmpresaId(Long empresaId);
}
