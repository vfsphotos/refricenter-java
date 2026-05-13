package com.refricenter.controller;

import com.refricenter.dto.LoginRequest;
import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.AuthService;
import com.refricenter.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final List<Map<String, String>> MODULOS = List.of(
        Map.of("key","dashboard","label","Dashboard"),
        Map.of("key","clientes","label","Clientes"),
        Map.of("key","ordens_servico","label","Ordens de Serviço"),
        Map.of("key","estoque","label","Estoque"),
        Map.of("key","financeiro","label","Financeiro"),
        Map.of("key","whatsapp","label","WhatsApp"),
        Map.of("key","relatorios","label","Relatórios"),
        Map.of("key","backup","label","Backup"),
        Map.of("key","configuracoes","label","Configurações")
    );

    private final AuthService authService;
    private final TenantService tenantService;
    private final UsuarioEmpresaRepository ueRepo;
    private final SetorRepository setorRepo;
    private final PermissaoSetorRepository permissaoSetorRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthService authService, TenantService tenantService,
                          UsuarioEmpresaRepository ueRepo, SetorRepository setorRepo,
                          PermissaoSetorRepository permissaoSetorRepo,
                          PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.tenantService = tenantService;
        this.ueRepo = ueRepo;
        this.setorRepo = setorRepo;
        this.permissaoSetorRepo = permissaoSetorRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank() ||
            req.getPassword() == null || req.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Usuário e senha são obrigatórios."));
        }
        try {
            Map<String, Object> resp = authService.login(req.getUsername().trim(), req.getPassword());
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("MANUTENCAO:")) {
                return ResponseEntity.status(503).body(Map.of(
                    "error", msg.substring(11), "manutencao", true));
            }
            if (msg != null && msg.startsWith("EMPRESA_BLOQUEADA:")) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", msg.substring(18), "empresa_bloqueada", true));
            }
            return ResponseEntity.status(401).body(Map.of("error", msg != null ? msg : "Erro no login."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout realizado com sucesso."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Usuario user = tenantService.currentUser();
        UsuarioEmpresa ue = tenantService.currentUsuarioEmpresa().orElse(null);
        Map<String, Object> resp = authService.buildUserResponse(user, ue);
        if (user.getSuperAdmin() != null && Boolean.TRUE.equals(user.getSuperAdmin().getAtivo())) {
            resp.put("is_superadmin", true);
            resp.put("saas_nivel", user.getSuperAdmin().getNivel());
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios() {
        UsuarioEmpresa ue = tenantService.currentUsuarioEmpresa().orElse(null);
        if (ue == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo com empresa."));
        if (!ue.isDono() && !"gerente".equals(ue.getPapel())) {
            return ResponseEntity.status(403).body(Map.of("error","Acesso negado."));
        }
        List<Map<String, Object>> resultado = ueRepo.findByEmpresa(ue.getEmpresa()).stream()
                .map(this::serializeUsuario).toList();
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> criarUsuario(@RequestBody Map<String, Object> data) {
        UsuarioEmpresa ue = tenantService.currentUsuarioEmpresa().orElse(null);
        if (ue == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo com empresa."));
        try {
            UsuarioEmpresa novo = authService.criarUsuario(ue, data);
            return ResponseEntity.status(201).body(serializeUsuario(novo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<?> detalheUsuario(@PathVariable Long id) {
        UsuarioEmpresa solicitante = tenantService.currentUsuarioEmpresa().orElse(null);
        if (solicitante == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        return ueRepo.findById(id)
                .filter(u -> u.getEmpresa().getId().equals(solicitante.getEmpresa().getId()))
                .map(u -> ResponseEntity.ok(serializeUsuario(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> atualizarUsuario(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        UsuarioEmpresa solicitante = tenantService.currentUsuarioEmpresa().orElse(null);
        if (solicitante == null || (!solicitante.isDono() && !"gerente".equals(solicitante.getPapel()))) {
            return ResponseEntity.status(403).body(Map.of("error","Acesso negado."));
        }
        UsuarioEmpresa alvo = ueRepo.findById(id)
                .filter(u -> u.getEmpresa().getId().equals(solicitante.getEmpresa().getId()))
                .orElse(null);
        if (alvo == null) return ResponseEntity.notFound().build();

        if (data.containsKey("papel")) alvo.setPapel(data.get("papel").toString());
        if (data.containsKey("ativo")) {
            boolean ativo = Boolean.parseBoolean(data.get("ativo").toString());
            alvo.setAtivo(ativo);
            alvo.getUser().setIsActive(ativo);
        }
        if (data.containsKey("nome")) {
            String[] parts = data.get("nome").toString().trim().split(" ", 2);
            alvo.getUser().setFirstName(parts[0]);
            alvo.getUser().setLastName(parts.length > 1 ? parts[1] : "");
        }
        if (data.containsKey("nova_senha") && data.get("nova_senha") != null) {
            alvo.getUser().setPassword(passwordEncoder.encode(data.get("nova_senha").toString()));
        }
        ueRepo.save(alvo);
        return ResponseEntity.ok(serializeUsuario(alvo));
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> excluirUsuario(@PathVariable Long id) {
        UsuarioEmpresa solicitante = tenantService.currentUsuarioEmpresa().orElse(null);
        if (solicitante == null || !solicitante.isDono()) {
            return ResponseEntity.status(403).body(Map.of("error","Apenas o dono pode excluir usuários."));
        }
        UsuarioEmpresa alvo = ueRepo.findById(id)
                .filter(u -> u.getEmpresa().getId().equals(solicitante.getEmpresa().getId()))
                .orElse(null);
        if (alvo == null) return ResponseEntity.notFound().build();
        if (alvo.isDono()) return ResponseEntity.badRequest().body(Map.of("error","Não é possível excluir o dono."));
        ueRepo.delete(alvo);
        return ResponseEntity.ok(Map.of("message","Usuário excluído com sucesso."));
    }

    @GetMapping("/setores")
    public ResponseEntity<?> listarSetores() {
        UsuarioEmpresa ue = tenantService.currentUsuarioEmpresa().orElse(null);
        if (ue == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        List<Map<String, Object>> resultado = setorRepo.findByEmpresaId(ue.getEmpresa().getId()).stream()
                .map(this::serializeSetor).toList();
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/setores")
    public ResponseEntity<?> criarSetor(@RequestBody Map<String, Object> data) {
        UsuarioEmpresa ue = tenantService.currentUsuarioEmpresa().orElse(null);
        if (ue == null || (!ue.isDono() && !"gerente".equals(ue.getPapel()))) {
            return ResponseEntity.status(403).body(Map.of("error","Acesso negado."));
        }
        if (data.get("nome") == null) return ResponseEntity.badRequest().body(Map.of("error","Nome é obrigatório."));

        Setor setor = new Setor();
        setor.setEmpresa(ue.getEmpresa());
        setor.setNome(data.get("nome").toString());
        setor.setDescricao(data.getOrDefault("descricao","").toString());
        setor.setCor(data.getOrDefault("cor","#3b82f6").toString());
        setorRepo.save(setor);

        List<?> modulosLiberados = data.containsKey("modulos") ? (List<?>) data.get("modulos") : List.of();
        for (Map<String, String> m : MODULOS) {
            String key = m.get("key");
            boolean liberado = modulosLiberados.contains(key);
            PermissaoSetor p = new PermissaoSetor();
            p.setSetor(setor);
            p.setModulo(key);
            p.setPodeVisualizar(liberado);
            p.setPodeCriar(liberado);
            p.setPodeEditar(liberado);
            p.setPodeDeletar(false);
            permissaoSetorRepo.save(p);
        }
        return ResponseEntity.status(201).body(serializeSetor(setorRepo.findById(setor.getId()).orElse(setor)));
    }

    @GetMapping("/setores/{id}")
    public ResponseEntity<?> detalheSetor(@PathVariable Long id) {
        UsuarioEmpresa ue = tenantService.currentUsuarioEmpresa().orElse(null);
        if (ue == null) return ResponseEntity.status(403).body(Map.of("error","Sem vínculo."));
        return setorRepo.findByIdAndEmpresaId(id, ue.getEmpresa().getId())
                .map(s -> ResponseEntity.ok(serializeSetor(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/setores/{id}")
    public ResponseEntity<?> atualizarSetor(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        UsuarioEmpresa ue = tenantService.currentUsuarioEmpresa().orElse(null);
        if (ue == null || (!ue.isDono() && !"gerente".equals(ue.getPapel()))) {
            return ResponseEntity.status(403).body(Map.of("error","Acesso negado."));
        }
        Setor setor = setorRepo.findByIdAndEmpresaId(id, ue.getEmpresa().getId()).orElse(null);
        if (setor == null) return ResponseEntity.notFound().build();

        if (data.containsKey("nome")) setor.setNome(data.get("nome").toString());
        if (data.containsKey("cor")) setor.setCor(data.get("cor").toString());
        if (data.containsKey("descricao")) setor.setDescricao(data.get("descricao").toString());
        setorRepo.save(setor);

        if (data.containsKey("permissoes")) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Boolean>> perms = (Map<String, Map<String, Boolean>>) data.get("permissoes");
            for (var entry : perms.entrySet()) {
                String moduloKey = entry.getKey();
                Map<String, Boolean> p = entry.getValue();
                PermissaoSetor perm = permissaoSetorRepo.findBySetorIdAndModulo(setor.getId(), moduloKey)
                        .orElse(new PermissaoSetor());
                perm.setSetor(setor);
                perm.setModulo(moduloKey);
                perm.setPodeVisualizar(Boolean.TRUE.equals(p.get("visualizar")));
                perm.setPodeCriar(Boolean.TRUE.equals(p.get("criar")));
                perm.setPodeEditar(Boolean.TRUE.equals(p.get("editar")));
                perm.setPodeDeletar(Boolean.TRUE.equals(p.get("deletar")));
                permissaoSetorRepo.save(perm);
            }
        }
        return ResponseEntity.ok(serializeSetor(setor));
    }

    @DeleteMapping("/setores/{id}")
    public ResponseEntity<?> desativarSetor(@PathVariable Long id) {
        UsuarioEmpresa ue = tenantService.currentUsuarioEmpresa().orElse(null);
        if (ue == null || (!ue.isDono() && !"gerente".equals(ue.getPapel()))) {
            return ResponseEntity.status(403).body(Map.of("error","Acesso negado."));
        }
        Setor setor = setorRepo.findByIdAndEmpresaId(id, ue.getEmpresa().getId()).orElse(null);
        if (setor == null) return ResponseEntity.notFound().build();
        setor.setAtivo(false);
        setorRepo.save(setor);
        return ResponseEntity.ok(Map.of("message","Setor desativado."));
    }

    @GetMapping("/modulos")
    public ResponseEntity<?> modulos() {
        return ResponseEntity.ok(MODULOS);
    }

    private Map<String, Object> serializeUsuario(UsuarioEmpresa ue) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ue.getId());
        m.put("user_id", ue.getUser().getId());
        m.put("username", ue.getUser().getUsername());
        m.put("nome", ue.getUser().getFullName());
        m.put("email", ue.getUser().getEmail());
        m.put("papel", ue.getPapel());
        m.put("ativo", ue.getAtivo());
        m.put("setor", ue.getSetor() != null ? Map.of(
            "id", ue.getSetor().getId(), "nome", ue.getSetor().getNome(), "cor", ue.getSetor().getCor()) : null);
        m.put("data_criacao", ue.getDataCriacao().toString());
        return m;
    }

    private Map<String, Object> serializeSetor(Setor setor) {
        Map<String, Map<String, Boolean>> perms = new LinkedHashMap<>();
        for (PermissaoSetor p : setor.getPermissoes()) {
            perms.put(p.getModulo(), Map.of(
                "visualizar", p.getPodeVisualizar(), "criar", p.getPodeCriar(),
                "editar", p.getPodeEditar(), "deletar", p.getPodeDeletar()));
        }
        long totalUsuarios = setor.getUsuarios().stream().filter(u -> Boolean.TRUE.equals(u.getAtivo())).count();
        return Map.of(
            "id", setor.getId(), "nome", setor.getNome(),
            "descricao", setor.getDescricao() != null ? setor.getDescricao() : "",
            "cor", setor.getCor(), "ativo", setor.getAtivo(),
            "permissoes", perms, "total_usuarios", totalUsuarios
        );
    }
}
