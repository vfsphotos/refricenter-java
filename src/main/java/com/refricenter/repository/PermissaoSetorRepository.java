package com.refricenter.repository;

import com.refricenter.model.PermissaoSetor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PermissaoSetorRepository extends JpaRepository<PermissaoSetor, Long> {
    Optional<PermissaoSetor> findBySetorIdAndModulo(Long setorId, String modulo);
}
