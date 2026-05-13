package com.refricenter.repository;

import com.refricenter.model.AuditoriaLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Long> {
    Page<AuditoriaLog> findByEmpresaIdOrderByDataDesc(Long empresaId, Pageable pageable);
}
