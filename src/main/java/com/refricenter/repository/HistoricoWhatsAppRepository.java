package com.refricenter.repository;

import com.refricenter.model.HistoricoWhatsApp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoricoWhatsAppRepository extends JpaRepository<HistoricoWhatsApp, Long> {
    List<HistoricoWhatsApp> findByClienteEmpresaIdOrderByDataEnvioDesc(Long empresaId);
}
