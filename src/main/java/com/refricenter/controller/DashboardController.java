package com.refricenter.controller;

import com.refricenter.repository.*;
import com.refricenter.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final OrdemServicoRepository osRepo;
    private final ClienteRepository clienteRepo;
    private final ProdutoRepository produtoRepo;
    private final LancamentoFinanceiroRepository lancRepo;
    private final TenantService tenantService;

    public DashboardController(OrdemServicoRepository osRepo, ClienteRepository clienteRepo,
                                ProdutoRepository produtoRepo, LancamentoFinanceiroRepository lancRepo,
                                TenantService tenantService) {
        this.osRepo = osRepo;
        this.clienteRepo = clienteRepo;
        this.produtoRepo = produtoRepo;
        this.lancRepo = lancRepo;
        this.tenantService = tenantService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null && !tenantService.isSuperAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error","Sem vínculo com empresa."));
        }

        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDateTime inicioMesDt = inicioMes.atStartOfDay();

        long totalOs = empresaId != null ? osRepo.countByEmpresaId(empresaId) : osRepo.count();
        long osEmAndamento = empresaId != null ? osRepo.countByEmpresaIdAndStatus(empresaId, "em_andamento") : 0;
        long osProntas = empresaId != null ? osRepo.countByEmpresaIdAndStatus(empresaId, "pronta") : 0;
        long osOrcamento = empresaId != null ? osRepo.countByEmpresaIdAndStatus(empresaId, "orcamento") : 0;
        long totalClientes = empresaId != null ? clienteRepo.countByEmpresaId(empresaId) : clienteRepo.count();
        long totalProdutos = empresaId != null ? produtoRepo.countByEmpresaIdAndAtivoTrue(empresaId) : produtoRepo.count();

        BigDecimal receitaMes = empresaId != null
            ? lancRepo.sumByEmpresaAndTipoAndPeriodo(empresaId, "receita", inicioMes, hoje)
            : BigDecimal.ZERO;
        BigDecimal despesasMes = empresaId != null
            ? lancRepo.sumByEmpresaAndTipoAndPeriodo(empresaId, "despesa", inicioMes, hoje)
            : BigDecimal.ZERO;
        BigDecimal despesasPendentes = empresaId != null
            ? lancRepo.sumDespesasPendentes(empresaId)
            : BigDecimal.ZERO;

        return ResponseEntity.ok(Map.of(
            "total_os", totalOs,
            "os_em_andamento", osEmAndamento,
            "os_prontas", osProntas,
            "os_orcamento", osOrcamento,
            "total_clientes", totalClientes,
            "total_produtos", totalProdutos,
            "receita_mes", receitaMes,
            "despesas_mes", despesasMes,
            "despesas_pendentes", despesasPendentes,
            "lucro_mes", receitaMes.subtract(despesasMes)
        ));
    }
}
