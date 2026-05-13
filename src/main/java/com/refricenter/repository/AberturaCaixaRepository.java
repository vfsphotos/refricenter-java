package com.refricenter.repository;

import com.refricenter.model.AberturaCaixa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AberturaCaixaRepository extends JpaRepository<AberturaCaixa, Long> {
    Optional<AberturaCaixa> findFirstByEmpresaIdAndDataFechamentoIsNullOrderByDataAberturaDesc(Long empresaId);
    List<AberturaCaixa> findByEmpresaIdOrderByDataAberturaDesc(Long empresaId);
}
