package com.refricenter.repository;

import com.refricenter.model.CategoriaProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoriaProdutoRepository extends JpaRepository<CategoriaProduto, Long> {
    Optional<CategoriaProduto> findByNome(String nome);
}
