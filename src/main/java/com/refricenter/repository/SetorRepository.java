package com.refricenter.repository;

import com.refricenter.model.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SetorRepository extends JpaRepository<Setor, Long> {
    List<Setor> findByEmpresaId(Long empresaId);
    Optional<Setor> findByIdAndEmpresaId(Long id, Long empresaId);
}
