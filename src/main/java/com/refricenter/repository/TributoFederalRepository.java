package com.refricenter.repository;

import com.refricenter.model.TributoFederal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TributoFederalRepository extends JpaRepository<TributoFederal, Long> {
    List<TributoFederal> findByEmpresaIdOrderByDataVencimento(Long empresaId);
    List<TributoFederal> findByIdAndEmpresaId(Long id, Long empresaId);
}
