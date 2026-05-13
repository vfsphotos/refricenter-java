package com.refricenter.repository;

import com.refricenter.model.LancamentoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro, Long>, JpaSpecificationExecutor<LancamentoFinanceiro> {
    List<LancamentoFinanceiro> findByEmpresaId(Long empresaId);

    @Query("SELECT COALESCE(SUM(l.valor),0) FROM LancamentoFinanceiro l WHERE l.empresa.id = :eId AND l.tipo = :tipo AND l.status = 'pago' AND l.dataPagamento BETWEEN :inicio AND :fim")
    BigDecimal sumByEmpresaAndTipoAndPeriodo(@Param("eId") Long empresaId, @Param("tipo") String tipo, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(l.valor),0) FROM LancamentoFinanceiro l WHERE l.empresa.id = :eId AND l.status = 'pendente' AND l.tipo = 'despesa'")
    BigDecimal sumDespesasPendentes(@Param("eId") Long empresaId);

    List<LancamentoFinanceiro> findByEmpresaIdAndDataVencimentoBetween(Long empresaId, LocalDate inicio, LocalDate fim);
    List<LancamentoFinanceiro> findByLancamentoPaiId(Long lancamentoPaiId);
}
