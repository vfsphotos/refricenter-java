package com.refricenter.repository;

import com.refricenter.model.Equipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {
    List<Equipamento> findByClienteId(Long clienteId);
    List<Equipamento> findByClienteEmpresaId(Long empresaId);
}
