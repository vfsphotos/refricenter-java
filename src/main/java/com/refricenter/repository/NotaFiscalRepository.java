package com.refricenter.repository;

import com.refricenter.model.NotaFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Long> {
    List<NotaFiscal> findByEmpresaIdOrderByDataEmissaoDesc(Long empresaId);
}
