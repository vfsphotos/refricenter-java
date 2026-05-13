package com.refricenter.repository;

import com.refricenter.model.ItemOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemOrdemServicoRepository extends JpaRepository<ItemOrdemServico, Long> {
    List<ItemOrdemServico> findByOrdemServicoId(Long osId);
    List<ItemOrdemServico> findByAguardandoEstoqueTrue();
    List<ItemOrdemServico> findByOrdemServicoEmpresaIdAndAguardandoEstoqueTrue(Long empresaId);
    List<ItemOrdemServico> findByOrdemServicoEmpresaId(Long empresaId);
    List<ItemOrdemServico> findByProdutoIdAndAguardandoEstoqueTrue(Long produtoId);
}
