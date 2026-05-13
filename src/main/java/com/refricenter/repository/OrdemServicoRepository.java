package com.refricenter.repository;

import com.refricenter.model.OrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long>, JpaSpecificationExecutor<OrdemServico> {
    List<OrdemServico> findByEmpresaId(Long empresaId);
    Optional<OrdemServico> findByNumero(String numero);
    Optional<OrdemServico> findByIdAndEmpresaId(Long id, Long empresaId);
    long countByEmpresaIdAndStatus(Long empresaId, String status);
    long countByEmpresaId(Long empresaId);

    @Query("SELECT COUNT(o) FROM OrdemServico o WHERE o.empresa.id = :eId AND o.dataAbertura >= :inicio")
    long countByEmpresaIdAndDataAberturaAfter(@Param("eId") Long empresaId, @Param("inicio") LocalDateTime inicio);

    List<OrdemServico> findByEmpresaIdAndStatusIn(Long empresaId, List<String> statuses);
    List<OrdemServico> findByEmpresaIdAndPagamentoPrazoTrueAndValorRestantePrazoGreaterThan(
        Long empresaId, java.math.BigDecimal zero);
}
