package com.refricenter.repository;

import com.refricenter.model.ConfiguracaoFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfiguracaoFiscalRepository extends JpaRepository<ConfiguracaoFiscal, Long> {
    Optional<ConfiguracaoFiscal> findByEmpresaId(Long empresaId);
}
