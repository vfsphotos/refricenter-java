package com.refricenter.repository;

import com.refricenter.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {
    List<Cliente> findByEmpresaIdAndAtivoTrue(Long empresaId);
    List<Cliente> findByEmpresaId(Long empresaId);
    long countByEmpresaId(Long empresaId);
}
