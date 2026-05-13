package com.refricenter.service;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AuthService {

    private static final String[] MODULOS = {
        "dashboard","clientes","ordens_servico","estoque","financeiro",
        "whatsapp","relatorios","backup","configuracoes"
    };

    private final AuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioEmpresaRepository ueRepo;
    private final PlataformaConfigRepository configRepo;
    private final UsuarioRepository usuarioRepo;
    private final SetorRepository setorRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authManager, JwtTokenProvider tokenProvider,
                       UsuarioEmpresaRepository ueRepo, PlataformaConfigRepository configRepo,
                       UsuarioRepository usuarioRepo, SetorRepository setorRepo,
                       PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.tokenProvider = tokenProvider;
        this.ueRepo = ueRepo;
        this.configRepo = configRepo;
        this.usuarioRepo = usuarioRepo;
        this.setorRepo = setorRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> login(String username, String password) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            throw new RuntimeException("Usuário ou senha incorretos.");
        }

        Usuario user = usuarioRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Conta desativada. Contate o administrador.");
        }

        boolean isSuperadmin = user.getSuperAdmin() != null && Boolean.TRUE.equals(user.getSuperAdmin().getAtivo());

        PlataformaConfig plataforma = configRepo.findAll().stream().findFirst()
                .orElse(new PlataformaConfig());
        if (Boolean.TRUE.equals(plataforma.getManutencaoAtiva()) && !isSuperadmin) {
            throw new RuntimeException("MANUTENCAO:" + (plataforma.getMensagemManutencao() != null
                ? plataforma.getMensagemManutencao() : "Sistema em manutenção."));
        }

        UsuarioEmpresa ue = null;
        if (!isSuperadmin) {
            ue = ueRepo.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não vinculado a nenhuma empresa."));
            if (!Boolean.TRUE.equals(ue.getAtivo())) {
                throw new RuntimeException("Seu acesso foi desativado pelo administrador.");
            }
            if (!Boolean.TRUE.equals(ue.getEmpresa().getAtiva())) {
                throw new RuntimeException("EMPRESA_BLOQUEADA:O acesso da sua empresa foi suspenso.");
            }
        }

        String token = tokenProvider.generateToken(user.getUsername());
        Map<String, Object> resp = buildUserResponse(user, ue);
        resp.put("token", token);
        if (isSuperadmin) {
            SuperAdmin sa = user.getSuperAdmin();
            resp.put("is_superadmin", true);
            resp.put("saas_nivel", sa.getNivel());
            resp.put("pode_impersonar", sa.getPodeImpersonar());
        }
        return resp;
    }

    public Map<String, Object> buildUserResponse(Usuario user, UsuarioEmpresa ue) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", user.getId());
        resp.put("username", user.getUsername());
        resp.put("nome", user.getFullName());
        resp.put("email", user.getEmail());

        if (ue != null) {
            Map<String, Map<String, Boolean>> permissoes = ue.getPermissoes();
            boolean fiscalHabilitado = Boolean.TRUE.equals(ue.getEmpresa().getFiscalHabilitado());
            permissoes.put("fiscal", Map.of("visualizar", fiscalHabilitado, "criar", fiscalHabilitado,
                    "editar", fiscalHabilitado, "deletar", fiscalHabilitado));
            resp.put("permissoes", permissoes);
            resp.put("is_dono", ue.isDono());
            resp.put("papel", ue.getPapel());
            resp.put("empresa", Map.of(
                "id", ue.getEmpresa().getId(),
                "nome", ue.getEmpresa().getNome(),
                "cnpj", ue.getEmpresa().getCnpj() != null ? ue.getEmpresa().getCnpj() : "",
                "slug", ue.getEmpresa().getSlug(),
                "plano", ue.getEmpresa().getPlano(),
                "fiscal_habilitado", fiscalHabilitado
            ));
            if (ue.getSetor() != null) {
                resp.put("setor", Map.of(
                    "id", ue.getSetor().getId(),
                    "nome", ue.getSetor().getNome(),
                    "cor", ue.getSetor().getCor()
                ));
            } else {
                resp.put("setor", null);
            }
        } else {
            Map<String, Map<String, Boolean>> permissoes = new LinkedHashMap<>();
            for (String m : MODULOS) {
                permissoes.put(m, Map.of("visualizar", true, "criar", true, "editar", true, "deletar", true));
            }
            permissoes.put("fiscal", Map.of("visualizar", true, "criar", true, "editar", true, "deletar", true));
            resp.put("permissoes", permissoes);
            resp.put("is_dono", true);
            resp.put("papel", "dono");
            resp.put("empresa", null);
            resp.put("setor", null);
        }
        return resp;
    }

    @Transactional
    public UsuarioEmpresa criarUsuario(UsuarioEmpresa solicitante, Map<String, Object> data) {
        if (!solicitante.isDono() && !"gerente".equals(solicitante.getPapel())) {
            throw new RuntimeException("Acesso negado.");
        }
        String username = (String) data.get("username");
        String password = (String) data.get("password");
        String nome = (String) data.get("nome");
        if (username == null || password == null || nome == null) {
            throw new RuntimeException("Campos obrigatórios: username, password, nome");
        }
        if (usuarioRepo.existsByUsername(username)) {
            throw new RuntimeException("Nome de usuário já existe.");
        }
        String[] nameParts = nome.trim().split(" ", 2);
        Usuario user = new Usuario();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(nameParts[0]);
        user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        user.setEmail(data.getOrDefault("email", "").toString());
        user.setIsActive(true);
        usuarioRepo.save(user);

        Setor setor = null;
        if (data.get("setor_id") != null) {
            Long setorId = Long.parseLong(data.get("setor_id").toString());
            setor = setorRepo.findByIdAndEmpresaId(setorId, solicitante.getEmpresa().getId()).orElse(null);
        }

        UsuarioEmpresa ue = new UsuarioEmpresa();
        ue.setUser(user);
        ue.setEmpresa(solicitante.getEmpresa());
        ue.setSetor(setor);
        ue.setPapel(data.getOrDefault("papel", "tecnico").toString());
        return ueRepo.save(ue);
    }
}
