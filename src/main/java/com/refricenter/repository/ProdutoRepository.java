package com.refricenter.repository;

import com.refricenter.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {
    List<Produto> findByEmpresaId(Long empresaId);
    List<Produto> findByEmpresaIdAndAtivoTrue(Long empresaId);
    Optional<Produto> findByIdAndEmpresaId(Long id, Long empresaId);
    boolean existsByCodigoAndEmpresaId(String codigo, Long empresaId);
    Optional<Produto> findByCodigoAndEmpresaId(String codigo, Long empresaId);
    long countByEmpresaId(Long empresaId);
    long countByEmpresaIdAndAtivoTrue(Long empresaId);
}
