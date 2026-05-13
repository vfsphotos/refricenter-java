package com.refricenter.controller;

import com.refricenter.model.Cliente;
import com.refricenter.model.Empresa;
import com.refricenter.model.UsuarioEmpresa;
import com.refricenter.repository.ClienteRepository;
import com.refricenter.service.TenantService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteRepository clienteRepo;
    private final TenantService tenantService;

    public ClienteController(ClienteRepository clienteRepo, TenantService tenantService) {
        this.clienteRepo = clienteRepo;
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String search,
                                     @RequestParam(required = false) Boolean ativo) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null && !tenantService.isSuperAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error","Sem vínculo com empresa."));
        }
        List<Cliente> clientes = empresaId != null
            ? clienteRepo.findByEmpresaId(empresaId)
            : clienteRepo.findAll();

        if (Boolean.TRUE.equals(ativo)) clientes = clientes.stream().filter(c -> Boolean.TRUE.equals(c.getAtivo())).toList();
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            clientes = clientes.stream().filter(c ->
                (c.getNome() != null && c.getNome().toLowerCase().contains(q)) ||
                (c.getCpfCnpj() != null && c.getCpfCnpj().contains(q)) ||
                (c.getTelefone() != null && c.getTelefone().contains(q))
            ).toList();
        }
        return ResponseEntity.ok(clientes.stream().map(this::serialize).toList());
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null && !tenantService.isSuperAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error","Sem vínculo com empresa."));
        }
        if (data.get("nome") == null || data.get("nome").toString().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error","Nome é obrigatório."));
        }
        Cliente c = fromData(data, new Cliente());
        if (empresaId != null) {
            com.refricenter.model.Empresa emp = new com.refricenter.model.Empresa();
            emp.setId(empresaId);
            c.setEmpresa(emp);
        }
        c.setCriadoPor(tenantService.currentUser());
        clienteRepo.save(c);
        return ResponseEntity.status(201).body(serialize(c));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalhe(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        return clienteRepo.findById(id)
            .filter(c -> empresaId == null || empresaId.equals(c.getEmpresa() != null ? c.getEmpresa().getId() : null))
            .map(c -> ResponseEntity.ok(serialize(c)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        Cliente c = clienteRepo.findById(id)
            .filter(cl -> empresaId == null || empresaId.equals(cl.getEmpresa() != null ? cl.getEmpresa().getId() : null))
            .orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        fromData(data, c);
        clienteRepo.save(c);
        return ResponseEntity.ok(serialize(c));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return atualizar(id, data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        Long empresaId = tenantService.getEmpresaId();
        Cliente c = clienteRepo.findById(id)
            .filter(cl -> empresaId == null || empresaId.equals(cl.getEmpresa() != null ? cl.getEmpresa().getId() : null))
            .orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        c.setAtivo(false);
        clienteRepo.save(c);
        return ResponseEntity.noContent().build();
    }

    private Cliente fromData(Map<String, Object> data, Cliente c) {
        if (data.containsKey("nome")) c.setNome(data.get("nome").toString());
        if (data.containsKey("tipo")) c.setTipo(data.get("tipo").toString());
        if (data.containsKey("cpf_cnpj")) c.setCpfCnpj(data.getOrDefault("cpf_cnpj","").toString());
        if (data.containsKey("telefone")) c.setTelefone(data.getOrDefault("telefone","").toString());
        if (data.containsKey("whatsapp")) c.setWhatsapp(data.getOrDefault("whatsapp","").toString());
        if (data.containsKey("email")) c.setEmail(data.getOrDefault("email","").toString());
        if (data.containsKey("endereco")) c.setEndereco(data.getOrDefault("endereco","").toString());
        if (data.containsKey("numero")) c.setNumero(data.getOrDefault("numero","").toString());
        if (data.containsKey("complemento")) c.setComplemento(data.getOrDefault("complemento","").toString());
        if (data.containsKey("bairro")) c.setBairro(data.getOrDefault("bairro","").toString());
        if (data.containsKey("cidade")) c.setCidade(data.getOrDefault("cidade","").toString());
        if (data.containsKey("estado")) c.setEstado(data.getOrDefault("estado","").toString());
        if (data.containsKey("cep")) c.setCep(data.getOrDefault("cep","").toString());
        if (data.containsKey("observacoes")) c.setObservacoes(data.getOrDefault("observacoes","").toString());
        if (data.containsKey("ativo")) c.setAtivo(Boolean.parseBoolean(data.get("ativo").toString()));
        return c;
    }

    @PostMapping("/importar-clientes")
    public ResponseEntity<?> importarClientes(@RequestParam("arquivo") MultipartFile arquivo) {
        Long empresaId = tenantService.getEmpresaId();
        if (empresaId == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));

        int criados = 0, atualizados = 0, erros = 0;
        List<String> mensagensErro = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String nome = cellStr(row.getCell(0));
                    if (nome.isBlank()) continue;

                    Cliente c = new Cliente();
                    Empresa emp = new Empresa(); emp.setId(empresaId);
                    c.setEmpresa(emp);
                    c.setNome(nome);
                    c.setTipo(cellStr(row.getCell(1)).isBlank() ? "PF" : cellStr(row.getCell(1)));
                    c.setCpfCnpj(cellStr(row.getCell(2)));
                    c.setTelefone(cellStr(row.getCell(3)));
                    c.setWhatsapp(cellStr(row.getCell(4)));
                    c.setEmail(cellStr(row.getCell(5)));
                    c.setCidade(cellStr(row.getCell(6)));
                    c.setEstado(cellStr(row.getCell(7)));
                    c.setObservacoes(cellStr(row.getCell(8)));
                    clienteRepo.save(c);
                    criados++;
                } catch (Exception e) {
                    erros++;
                    mensagensErro.add("Linha " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao ler arquivo: " + e.getMessage()));
        }

        return ResponseEntity.ok(Map.of("criados", criados, "atualizados", atualizados, "erros", erros, "mensagens", mensagensErro));
    }

    @GetMapping("/modelo-importacao-clientes")
    public ResponseEntity<byte[]> modeloImportacao() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Clientes");
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont(); font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {"nome","tipo (PF/PJ)","cpf_cnpj","telefone","whatsapp","email","cidade","estado","observacoes"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            Row exemplo = sheet.createRow(1);
            exemplo.createCell(0).setCellValue("João da Silva");
            exemplo.createCell(1).setCellValue("PF");
            exemplo.createCell(2).setCellValue("123.456.789-00");
            exemplo.createCell(3).setCellValue("(11) 99999-9999");
            exemplo.createCell(4).setCellValue("(11) 99999-9999");
            exemplo.createCell(5).setCellValue("joao@email.com");
            exemplo.createCell(6).setCellValue("São Paulo");
            exemplo.createCell(7).setCellValue("SP");
            exemplo.createCell(8).setCellValue("");

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"modelo_clientes.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
        }
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private Map<String, Object> serialize(Cliente c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("nome", c.getNome());
        m.put("tipo", c.getTipo());
        m.put("cpf_cnpj", c.getCpfCnpj());
        m.put("telefone", c.getTelefone());
        m.put("whatsapp", c.getWhatsapp());
        m.put("email", c.getEmail());
        m.put("endereco", c.getEndereco());
        m.put("numero", c.getNumero());
        m.put("complemento", c.getComplemento());
        m.put("bairro", c.getBairro());
        m.put("cidade", c.getCidade());
        m.put("estado", c.getEstado());
        m.put("cep", c.getCep());
        m.put("observacoes", c.getObservacoes());
        m.put("ativo", c.getAtivo());
        m.put("data_cadastro", c.getDataCadastro() != null ? c.getDataCadastro().toString() : null);
        m.put("empresa", c.getEmpresa() != null ? c.getEmpresa().getId() : null);
        return m;
    }
}
