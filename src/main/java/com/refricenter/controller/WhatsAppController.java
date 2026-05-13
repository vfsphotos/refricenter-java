package com.refricenter.controller;

import com.refricenter.model.*;
import com.refricenter.repository.*;
import com.refricenter.service.TenantService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

@RestController
@RequestMapping("/api")
public class WhatsAppController {

    private final HistoricoWhatsAppRepository historicoRepo;
    private final ClienteRepository clienteRepo;
    private final TenantService tenantService;
    private final WebClient waClient;

    public WhatsAppController(HistoricoWhatsAppRepository historicoRepo, ClienteRepository clienteRepo,
                               TenantService tenantService,
                               @Value("${app.whatsapp-service-url}") String waUrl) {
        this.historicoRepo = historicoRepo;
        this.clienteRepo = clienteRepo;
        this.tenantService = tenantService;
        this.waClient = WebClient.builder().baseUrl(waUrl).build();
    }

    @PostMapping("/enviar-whatsapp")
    public ResponseEntity<?> enviarWhatsApp(@RequestBody Map<String, Object> data) {
        Long empresaId = tenantService.getEmpresaId();
        String telefone = (String) data.get("telefone");
        String mensagem = (String) data.get("mensagem");
        if (telefone == null || mensagem == null) {
            return ResponseEntity.badRequest().body(Map.of("error","Telefone e mensagem são obrigatórios."));
        }
        try {
            Map<?, ?> resp = waClient.post().uri("/send")
                .bodyValue(Map.of("telefone", telefone, "mensagem", mensagem))
                .retrieve().bodyToMono(Map.class).block();

            if (data.get("cliente_id") != null) {
                Long clienteId = Long.parseLong(data.get("cliente_id").toString());
                clienteRepo.findById(clienteId).ifPresent(c -> {
                    HistoricoWhatsApp h = new HistoricoWhatsApp();
                    h.setCliente(c);
                    h.setTelefone(telefone);
                    h.setMensagem(mensagem);
                    h.setTipo("enviada");
                    h.setStatus("enviada");
                    historicoRepo.save(h);
                });
            }
            return ResponseEntity.ok(Map.of("ok", true, "resposta", resp));
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(502).body(Map.of("error","Erro ao enviar mensagem: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error","Serviço WhatsApp indisponível."));
        }
    }

    @GetMapping("/whatsapp-templates")
    public ResponseEntity<?> templates() {
        return ResponseEntity.ok(List.of(
            Map.of("id", "orcamento_aprovado", "nome", "Orçamento Aprovado",
                "template", "Olá {nome}! Seu orçamento OS {numero} foi aprovado. Valor: R$ {valor}."),
            Map.of("id", "pronto_retirada", "nome", "Pronto para Retirada",
                "template", "Olá {nome}! Seu equipamento está pronto para retirada. OS {numero}."),
            Map.of("id", "lembrete_prazo", "nome", "Lembrete de Prazo",
                "template", "Olá {nome}! Lembramos que há um valor pendente de R$ {valor} com vencimento em {data}.")
        ));
    }

    @GetMapping("/historico-whatsapp")
    public ResponseEntity<?> historico() {
        Long empresaId = tenantService.getEmpresaId();
        return ResponseEntity.ok(historicoRepo.findByClienteEmpresaIdOrderByDataEnvioDesc(empresaId).stream().map(h -> Map.of(
            "id", h.getId(),
            "cliente", h.getCliente() != null ? Map.of("id", h.getCliente().getId(), "nome", h.getCliente().getNome()) : null,
            "telefone", h.getTelefone(),
            "mensagem", h.getMensagem(),
            "tipo", h.getTipo(),
            "status", h.getStatus(),
            "data_envio", h.getDataEnvio().toString()
        )).toList());
    }

    @GetMapping("/wa-diagnose")
    public ResponseEntity<?> diagnose() {
        try {
            Map<?, ?> resp = waClient.get().uri("/status").retrieve().bodyToMono(Map.class).block();
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status","offline","erro", e.getMessage()));
        }
    }

    @RequestMapping(value = {"/wa", "/wa/{path}"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> waProxy(@PathVariable(required = false) String path,
                                      @RequestBody(required = false) Object body,
                                      HttpServletRequest request) {
        String uri = "/" + (path != null ? path : "");
        String query = request.getQueryString();
        if (query != null) uri += "?" + query;
        try {
            var spec = "POST".equals(request.getMethod())
                ? waClient.post().uri(uri).bodyValue(body != null ? body : "")
                : waClient.get().uri(uri);
            Object resp = spec.retrieve().bodyToMono(Object.class).block();
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }
}
