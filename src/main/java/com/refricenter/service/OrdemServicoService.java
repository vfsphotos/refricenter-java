package com.refricenter.service;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class OrdemServicoService {

    private final OrdemServicoRepository osRepo;
    private final ProdutoRepository produtoRepo;
    private final ItemOrdemServicoRepository itemRepo;
    private final MovimentacaoEstoqueRepository movRepo;
    private final ConfiguracaoSistemaRepository configRepo;
    private final LancamentoFinanceiroRepository lancRepo;
    private final TributoFederalRepository tributoRepo;

    public OrdemServicoService(OrdemServicoRepository osRepo, ProdutoRepository produtoRepo,
                               ItemOrdemServicoRepository itemRepo,
                               MovimentacaoEstoqueRepository movRepo,
                               ConfiguracaoSistemaRepository configRepo,
                               LancamentoFinanceiroRepository lancRepo,
                               TributoFederalRepository tributoRepo) {
        this.osRepo = osRepo;
        this.produtoRepo = produtoRepo;
        this.itemRepo = itemRepo;
        this.movRepo = movRepo;
        this.configRepo = configRepo;
        this.lancRepo = lancRepo;
        this.tributoRepo = tributoRepo;
    }

    @Transactional
    public String gerarNumeroOs(Long empresaId) {
        ConfiguracaoSistema config = configRepo.findByEmpresaId(empresaId).orElse(null);
        int proximo;
        if (config != null) {
            proximo = Math.max(config.getNumeroInicialOs(), config.getUltimaOs() + 1);
            config.setUltimaOs(proximo);
            configRepo.save(config);
        } else {
            proximo = (int) (osRepo.countByEmpresaId(empresaId) + 1);
        }
        return String.format("%05d", proximo);
    }

    // Generates a unique OS number retrying up to 10 times on collision
    @Transactional
    public OrdemServico criarOrdemServico(Long empresaId, OrdemServico os) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String numero = gerarNumeroOs(empresaId);
            os.setNumero(numero);
            try {
                return osRepo.saveAndFlush(os);
            } catch (DataIntegrityViolationException e) {
                // numero collision — try again
            }
        }
        throw new RuntimeException("Não foi possível gerar um número único para a OS após 10 tentativas.");
    }

    @Transactional
    public ItemOrdemServico adicionarItem(Long empresaId, Long osId, Long produtoId, int quantidade, boolean aguardando, Usuario usuario) {
        OrdemServico os = osRepo.findByIdAndEmpresaId(osId, empresaId)
                .orElseThrow(() -> new RuntimeException("Ordem de serviço não encontrada"));
        Produto produto = produtoRepo.findByIdAndEmpresaId(produtoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        ItemOrdemServico item = new ItemOrdemServico();
        item.setOrdemServico(os);
        item.setProduto(produto);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(produto.getPrecoVenda());
        item.setAguardandoEstoque(aguardando);

        if (!aguardando) {
            if (produto.getEstoqueAtual() < quantidade) {
                throw new RuntimeException("Estoque insuficiente. Disponível: " + produto.getEstoqueAtual());
            }
            produto.setEstoqueAtual(produto.getEstoqueAtual() - quantidade);
            produtoRepo.save(produto);

            MovimentacaoEstoque mov = new MovimentacaoEstoque();
            mov.setProduto(produto);
            mov.setTipo("saida");
            mov.setQuantidade(quantidade);
            mov.setOrdemServico(os);
            mov.setObservacao("Saída para OS " + os.getNumero());
            mov.setUsuario(usuario);
            movRepo.save(mov);
        }

        return itemRepo.save(item);
    }

    // When stock enters for a product, auto-liberate waiting OS items if stock now covers them
    @Transactional
    public void liberarItensAguardandoEstoque(Produto produto, Usuario usuario) {
        List<ItemOrdemServico> itensAguardando = itemRepo.findByProdutoIdAndAguardandoEstoqueTrue(produto.getId());
        for (ItemOrdemServico item : itensAguardando) {
            if (produto.getEstoqueAtual() >= item.getQuantidade()) {
                produto.setEstoqueAtual(produto.getEstoqueAtual() - item.getQuantidade());
                produtoRepo.save(produto);
                item.setAguardandoEstoque(false);
                item.setStatusCompra("recebido");
                itemRepo.save(item);

                MovimentacaoEstoque mov = new MovimentacaoEstoque();
                mov.setProduto(produto);
                mov.setTipo("saida");
                mov.setQuantidade(item.getQuantidade());
                mov.setOrdemServico(item.getOrdemServico());
                mov.setObservacao("Liberação automática para OS " + item.getOrdemServico().getNumero());
                mov.setUsuario(usuario);
                movRepo.save(mov);
            }
        }
    }

    // Creates a LancamentoFinanceiro with optional parcelamento
    @Transactional
    public List<LancamentoFinanceiro> criarLancamento(LancamentoFinanceiro base, int numeroParcelas) {
        if (numeroParcelas <= 1) {
            base.setParcelado(false);
            lancRepo.save(base);
            return List.of(base);
        }

        base.setParcelado(true);
        base.setNumeroParcelas(numeroParcelas);
        base.setNumeroParcela(1);
        BigDecimal valorParcela = base.getValor().divide(BigDecimal.valueOf(numeroParcelas), 2, java.math.RoundingMode.HALF_UP);
        base.setValor(valorParcela);
        lancRepo.save(base);

        for (int i = 2; i <= numeroParcelas; i++) {
            LancamentoFinanceiro parcela = new LancamentoFinanceiro();
            parcela.setEmpresa(base.getEmpresa());
            parcela.setDescricao(base.getDescricao() + " (" + i + "/" + numeroParcelas + ")");
            parcela.setTipo(base.getTipo());
            parcela.setCategoria(base.getCategoria());
            parcela.setValor(valorParcela);
            parcela.setDataVencimento(addMonths(base.getDataVencimento(), i - 1));
            parcela.setFormaPagamento(base.getFormaPagamento());
            parcela.setStatus("pendente");
            parcela.setOrdemServico(base.getOrdemServico());
            parcela.setCliente(base.getCliente());
            parcela.setObservacoes(base.getObservacoes());
            parcela.setUsuario(base.getUsuario());
            parcela.setParcelado(true);
            parcela.setNumeroParcelas(numeroParcelas);
            parcela.setNumeroParcela(i);
            parcela.setLancamentoPai(base);
            lancRepo.save(parcela);
        }
        return lancRepo.findByLancamentoPaiId(base.getId());
    }

    // Syncs LancamentoFinanceiro when TributoFederal payment status changes
    @Transactional
    public void sincronizarLancamentoTributo(TributoFederal tributo, boolean pago, Long empresaId) {
        if (pago && tributo.getLancamentoFinanceiro() == null) {
            LancamentoFinanceiro lanc = new LancamentoFinanceiro();
            Empresa emp = new Empresa(); emp.setId(empresaId);
            lanc.setEmpresa(emp);
            lanc.setDescricao("Tributo " + tributo.getCategoria() + " - " + tributo.getCompetencia());
            lanc.setTipo("despesa");
            lanc.setCategoria("impostos");
            lanc.setValor(tributo.getValor());
            lanc.setDataVencimento(tributo.getDataVencimento());
            lanc.setDataPagamento(tributo.getDataPagamentoRealizado());
            lanc.setStatus("pago");
            lancRepo.save(lanc);
            tributo.setLancamentoFinanceiro(lanc);
            tributoRepo.save(tributo);
        } else if (!pago && tributo.getLancamentoFinanceiro() != null) {
            LancamentoFinanceiro old = tributo.getLancamentoFinanceiro();
            tributo.setLancamentoFinanceiro(null);
            tributoRepo.save(tributo);
            lancRepo.delete(old);
        }
    }

    private LocalDate addMonths(LocalDate date, int months) {
        return date.plusMonths(months);
    }
}
