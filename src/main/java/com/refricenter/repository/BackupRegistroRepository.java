package com.refricenter.repository;

import com.refricenter.model.BackupRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BackupRegistroRepository extends JpaRepository<BackupRegistro, Long> {
    List<BackupRegistro> findByEmpresaIdOrderByDataCriacaoDesc(Long empresaId);
}
