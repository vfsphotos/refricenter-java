package com.refricenter.repository;

import com.refricenter.model.ConfiguracaoSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, Long> {
    Optional<ConfiguracaoSistema> findByEmpresaId(Long empresaId);
}
