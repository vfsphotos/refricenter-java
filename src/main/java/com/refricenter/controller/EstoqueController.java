package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.OrdemServicoService;
import com.refricenter.service.TenantService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class EstoqueController {

    private final ProdutoRepository produtoRepo;
    private final MovimentacaoEstoqueRepository movRepo;
    private final CategoriaProdutoRepository categoriaRepo;
    private final MarcaRepository marcaRepo;
    private final OrdemServicoService osService;
    private final TenantService tenantService;

    public EstoqueController(ProdutoRepository produtoRepo, MovimentacaoEstoqueRepository movRepo,
                              CategoriaProdutoRepository categoriaRepo, MarcaRepository marcaRepo,
                              OrdemServicoService osService, TenantService tenantService) {
        this.produtoRepo = produtoRepo;
        this.movRepo = movRepo;
        this.categoriaRepo = categoriaRepo;
        this.marcaRepo = marcaRepo;
        this.osService = osService;
        this.tenantService = tenantService;
    }

    @GetMapping("/produtos")
    public ResponseEntity<?> listarProdutos(@RequestParam(required = false) Boolean ativo) {
        Long empresaId = tenantService.getEmpresaId();
        List<Produto> produtos = empresaId != null ? produtoRepo.findByEmpresaId(empresaId) : produtoRepo.findAll();
        if (Boolean.TRUE.equals(ativo)) produtos = produtos.stream().filter(p -> Boolean.TRUE.equals(p.getAtivo())).toList();
        return ResponseEntity.ok(produtos.stream().map(this::serializeProduto).toList());
    }

    @PostMapping("/produtos")
    public ResponseEntity<?> criarProduto(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        if (data.get("nome") == null) return ResponseEntity.badRequest().body(Map.of("error","Nome é obrigatório."));
        if (data.get("codigo") == null) return ResponseEntity.badRequest().body(Map.of("error","Código é obrigatório."));

        Produto p = fromProdutoData(data, new Produto());
        if (empresaId != null) { Empresa emp = new Empresa(); emp.setId(empresaId); p.setEmpresa(emp); }
        setCategoriaEMarca(data, p, empresaId);
        produtoRepo.save(p);
        return ResponseEntity.status(201).body(serializeProduto(p));
    }

    @GetMapping("/produtos/{id}")
    public ResponseEntity<?> detalheProduto(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        return produtoRepo.findById(id)
            .filter(p -> empresaId == null || empresaId.equals(p.getEmpresa() != null ? p.getEmpresa().getId() : null))
            .map(p -> ResponseEntity.ok(serializeProduto(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/produtos/{id}")
    public ResponseEntity<?> atualizarProduto(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        Produto p = produtoRepo.findById(id)
            .filter(pr -> empresaId == null || empresaId.equals(pr.getEmpresa() != null ? pr.getEmpresa().getId() : null))
            .orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        fromProdutoData(data, p);
        setCategoriaEMarca(data, p, empresaId);
        produtoRepo.save(p);
        return ResponseEntity.ok(serializeProduto(p));
    }

    @PatchMapping("/produtos/{id}")
    public ResponseEntity<?> patchProduto(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return atualizarProduto(id, data);
    }

    @DeleteMapping("/produtos/{id}")
    public ResponseEntity<?> excluirProduto(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        Produto p = produtoRepo.findById(id)
            .filter(pr -> empresaId == null || empresaId.equals(pr.getEmpresa() != null ? pr.getEmpresa().getId() : null))
            .orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        p.setAtivo(false);
        produtoRepo.save(p);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/entrada-estoque")
    public ResponseEntity<?> entradaEstoque(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        Long produtoId = Long.parseLong(data.get("produto_id").toString());
        int quantidade = Integer.parseInt(data.get("quantidade").toString());
        String observacao = data.getOrDefault("observacao","").toString();
        String tipo = data.getOrDefault("tipo","entrada").toString();

        Produto produto = produtoRepo.findById(produtoId)
            .filter(p -> empresaId == null || empresaId.equals(p.getEmpresa() != null ? p.getEmpresa().getId() : null))
            .orElse(null);
        if (produto == null) return ResponseEntity.badRequest().body(Map.of("error","Produto não encontrado."));

        if ("saida".equals(tipo)) {
            if (produto.getEstoqueAtual() < quantidade)
                return ResponseEntity.badRequest().body(Map.of("error","Estoque insuficiente."));
            produto.setEstoqueAtual(produto.getEstoqueAtual() - quantidade);
        } else if ("ajuste".equals(tipo)) {
            produto.setEstoqueAtual(quantidade);
        } else {
            produto.setEstoqueAtual(produto.getEstoqueAtual() + quantidade);
        }
        produtoRepo.save(produto);

        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setProduto(produto);
        mov.setTipo(tipo);
        mov.setQuantidade(quantidade);
        mov.setObservacao(observacao);
        mov.setUsuario(tenantService.currentUser());
        movRepo.save(mov);

        if ("entrada".equals(tipo) || "ajuste".equals(tipo)) {
            osService.liberarItensAguardandoEstoque(produto, tenantService.currentUser());
        }

        return ResponseEntity.ok(Map.of("ok", true, "estoque_atual", produto.getEstoqueAtual()));
    }

    @PostMapping("/limpar-estoque")
    public ResponseEntity<?> limparEstoque() {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Acesso negado."));
        UsuarioEmpresa ue = tenantService.currentUser().getUsuarioEmpresa();
        if (ue == null || (!("dono".equals(ue.getPapel())) && !("gerente".equals(ue.getPapel()))))
            return ResponseEntity.status(403).body(Map.of("error","Apenas dono ou gerente pode zerar o estoque."));
        List<Produto> produtos = produtoRepo.findByEmpresaId(empresaId);
        produtos.forEach(p -> p.setEstoqueAtual(0));
        produtoRepo.saveAll(produtos);
        return ResponseEntity.ok(Map.of("ok", true, "produtos_zerados", produtos.size()));
    }

    @GetMapping("/movimentacoes-estoque")
    public ResponseEntity<?> movimentacoes() {
        Long empresaId = tenantService.getEmpresaId();
        List<MovimentacaoEstoque> movs = empresaId != null
            ? movRepo.findByProdutoEmpresaIdOrderByDataDesc(empresaId)
            : movRepo.findAll();
        return ResponseEntity.ok(movs.stream().map(m -> Map.of(
            "id", m.getId(),
            "produto", m.getProduto() != null ? Map.of("id", m.getProduto().getId(), "nome", m.getProduto().getNome()) : null,
            "tipo", m.getTipo(),
            "quantidade", m.getQuantidade(),
            "observacao", m.getObservacao() != null ? m.getObservacao() : "",
            "data", m.getData() != null ? m.getData().toString() : null
        )).toList());
    }

    // ── Excel ──────────────────────────────────────────────────────────────────

    @PostMapping("/importar-estoque")
    public ResponseEntity<?> importarEstoque(@RequestParam("arquivo") MultipartFile arquivo) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));

        int importados = 0, erros = 0;
        List<String> mensagensErro = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String codigo = cellStr(row.getCell(0));
                    String nome = cellStr(row.getCell(1));
                    if (codigo.isBlank() || nome.isBlank()) continue;

                    String categoriaNome = cellStr(row.getCell(2));
                    String marcaNome = cellStr(row.getCell(3));
                    BigDecimal precoCusto = cellDecimal(row.getCell(4));
                    BigDecimal precoVenda = cellDecimal(row.getCell(5));
                    int estoqueAtual = (int) cellDouble(row.getCell(6));
                    int estoqueMinimo = (int) cellDouble(row.getCell(7));
                    String localizacao = cellStr(row.getCell(8));

                    Produto p = produtoRepo.findByCodigoAndEmpresaId(codigo, empresaId).orElse(new Produto());
                    Empresa emp = new Empresa(); emp.setId(empresaId);
                    p.setEmpresa(emp);
                    p.setCodigo(codigo);
                    p.setNome(nome);
                    p.setPrecoCusto(precoCusto);
                    p.setPrecoVenda(precoVenda);
                    p.setEstoqueAtual(estoqueAtual);
                    p.setEstoqueMinimo(estoqueMinimo);
                    p.setLocalizacao(localizacao);

                    if (!categoriaNome.isBlank()) {
                        CategoriaProduto cat = categoriaRepo.findByNome(categoriaNome)
                            .orElseGet(() -> { CategoriaProduto nc = new CategoriaProduto(); nc.setNome(categoriaNome); return categoriaRepo.save(nc); });
                        p.setCategoria(cat);
                    }
                    if (!marcaNome.isBlank()) {
                        Marca marca = marcaRepo.findByNome(marcaNome)
                            .orElseGet(() -> { Marca nm = new Marca(); nm.setNome(marcaNome); return marcaRepo.save(nm); });
                        p.setMarca(marca);
                    }
                    produtoRepo.save(p);
                    importados++;
                } catch (Exception e) {
                    erros++;
                    mensagensErro.add("Linha " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao ler arquivo: " + e.getMessage()));
        }

        return ResponseEntity.ok(Map.of("importados", importados, "erros", erros, "mensagens", mensagensErro));
    }

    @GetMapping("/exportar-estoque")
    public ResponseEntity<byte[]> exportarEstoque() throws Exception {
        Long empresaId = tenantService.getEmpresaId();
        List<Produto> produtos = produtoRepo.findByEmpresaId(empresaId);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Estoque");
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {"Código","Nome","Categoria","Marca","Preço Custo","Preço Venda","Estoque Atual","Estoque Mínimo","Localização"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Produto p : produtos) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getCodigo() != null ? p.getCodigo() : "");
                row.createCell(1).setCellValue(p.getNome() != null ? p.getNome() : "");
                row.createCell(2).setCellValue(p.getCategoria() != null ? p.getCategoria().getNome() : "");
                row.createCell(3).setCellValue(p.getMarca() != null ? p.getMarca().getNome() : "");
                row.createCell(4).setCellValue(p.getPrecoCusto() != null ? p.getPrecoCusto().doubleValue() : 0);
                row.createCell(5).setCellValue(p.getPrecoVenda() != null ? p.getPrecoVenda().doubleValue() : 0);
                row.createCell(6).setCellValue(p.getEstoqueAtual());
                row.createCell(7).setCellValue(p.getEstoqueMinimo());
                row.createCell(8).setCellValue(p.getLocalizacao() != null ? p.getLocalizacao() : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"estoque.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
        }
    }

    @GetMapping("/modelo-importacao-estoque")
    public ResponseEntity<byte[]> modeloImportacao() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Modelo");
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont(); font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {"codigo","nome","categoria","marca","preco_custo","preco_venda","estoque_atual","estoque_minimo","localizacao"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            Row exemplo = sheet.createRow(1);
            exemplo.createCell(0).setCellValue("PROD001");
            exemplo.createCell(1).setCellValue("Compressor Embraco");
            exemplo.createCell(2).setCellValue("Compressores");
            exemplo.createCell(3).setCellValue("Embraco");
            exemplo.createCell(4).setCellValue(150.00);
            exemplo.createCell(5).setCellValue(280.00);
            exemplo.createCell(6).setCellValue(5);
            exemplo.createCell(7).setCellValue(2);
            exemplo.createCell(8).setCellValue("A1");

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"modelo_importacao_estoque.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
        }
    }

    // ── Marcas e Categorias ────────────────────────────────────────────────────

    @GetMapping("/marcas")
    public ResponseEntity<?> marcas() { return ResponseEntity.ok(marcaRepo.findAll()); }

    @PostMapping("/marcas")
    public ResponseEntity<?> criarMarca(@RequestBody Marca marca) {
        return ResponseEntity.status(201).body(marcaRepo.save(marca));
    }

    @GetMapping("/marcas/{id}")
    public ResponseEntity<?> detalheMarca(@PathVariable Long id) {
        return marcaRepo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/marcas/{id}")
    public ResponseEntity<?> atualizarMarca(@PathVariable Long id, @RequestBody Marca body) {
        return marcaRepo.findById(id).map(m -> { body.setId(m.getId()); return ResponseEntity.ok(marcaRepo.save(body)); })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/marcas/{id}")
    public ResponseEntity<?> excluirMarca(@PathVariable Long id) {
        return marcaRepo.findById(id).map(m -> { marcaRepo.delete(m); return ResponseEntity.noContent().<Void>build(); })
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categorias-produto")
    public ResponseEntity<?> categorias() { return ResponseEntity.ok(categoriaRepo.findAll()); }

    @PostMapping("/categorias-produto")
    public ResponseEntity<?> criarCategoria(@RequestBody CategoriaProduto cat) {
        return ResponseEntity.status(201).body(categoriaRepo.save(cat));
    }

    @PutMapping("/categorias-produto/{id}")
    public ResponseEntity<?> atualizarCategoria(@PathVariable Long id, @RequestBody CategoriaProduto body) {
        return categoriaRepo.findById(id).map(c -> { body.setId(c.getId()); return ResponseEntity.ok(categoriaRepo.save(body)); })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/categorias-produto/{id}")
    public ResponseEntity<?> excluirCategoria(@PathVariable Long id) {
        return categoriaRepo.findById(id).map(c -> { categoriaRepo.delete(c); return ResponseEntity.noContent().<Void>build(); })
            .orElse(ResponseEntity.notFound().build());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void setCategoriaEMarca(Map<String, Object> data, Produto p, Long empresaId) {
        if (data.containsKey("categoria_id") && data.get("categoria_id") != null) {
            categoriaRepo.findById(Long.parseLong(data.get("categoria_id").toString()))
                .ifPresent(p::setCategoria);
        }
        if (data.containsKey("marca_id") && data.get("marca_id") != null) {
            marcaRepo.findById(Long.parseLong(data.get("marca_id").toString()))
                .ifPresent(p::setMarca);
        }
    }

    private Produto fromProdutoData(Map<String, Object> data, Produto p) {
        if (data.containsKey("nome")) p.setNome(data.get("nome").toString());
        if (data.containsKey("codigo")) p.setCodigo(data.get("codigo").toString());
        if (data.containsKey("descricao")) p.setDescricao(data.get("descricao").toString());
        if (data.containsKey("preco_custo")) p.setPrecoCusto(new BigDecimal(data.get("preco_custo").toString()));
        if (data.containsKey("preco_venda")) p.setPrecoVenda(new BigDecimal(data.get("preco_venda").toString()));
        if (data.containsKey("estoque_minimo")) p.setEstoqueMinimo(Integer.parseInt(data.get("estoque_minimo").toString()));
        if (data.containsKey("estoque_atual")) p.setEstoqueAtual(Integer.parseInt(data.get("estoque_atual").toString()));
        if (data.containsKey("localizacao")) p.setLocalizacao(data.get("localizacao").toString());
        if (data.containsKey("ativo")) p.setAtivo(Boolean.parseBoolean(data.get("ativo").toString()));
        return p;
    }

    private Map<String, Object> serializeProduto(Produto p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("codigo", p.getCodigo());
        m.put("nome", p.getNome());
        m.put("descricao", p.getDescricao());
        m.put("preco_custo", p.getPrecoCusto());
        m.put("preco_venda", p.getPrecoVenda());
        m.put("estoque_minimo", p.getEstoqueMinimo());
        m.put("estoque_atual", p.getEstoqueAtual());
        m.put("estoque_baixo", p.isEstoqueBaixo());
        m.put("localizacao", p.getLocalizacao());
        m.put("ativo", p.getAtivo());
        m.put("categoria", p.getCategoria() != null ? Map.of("id", p.getCategoria().getId(), "nome", p.getCategoria().getNome()) : null);
        m.put("marca", p.getMarca() != null ? Map.of("id", p.getMarca().getId(), "nome", p.getMarca().getNome()) : null);
        return m;
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private BigDecimal cellDecimal(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
        try { return new BigDecimal(cell.getStringCellValue().trim().replace(",",".")); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }

    private double cellDouble(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        try { return Double.parseDouble(cell.getStringCellValue().trim()); }
        catch (Exception e) { return 0; }
    }
}
