package com.refricenter.repository;

import com.refricenter.model.ConfiguracaoWhatsApp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfiguracaoWhatsAppRepository extends JpaRepository<ConfiguracaoWhatsApp, Long> {
    Optional<ConfiguracaoWhatsApp> findByEmpresaId(Long empresaId);
}
