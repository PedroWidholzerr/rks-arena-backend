package com.rks.api.publico;

import com.rks.config.VersaoBuild;
import com.rks.infra.persistencia.VerificadorBanco;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final VerificadorBanco banco;
    private final VersaoBuild versao;

    public HealthController(VerificadorBanco banco, VersaoBuild versao) {
        this.banco = banco;
        this.versao = versao;
    }

    @GetMapping({"/healthz", "/livez"})
    public ResponseEntity<Map<String, String>> viva() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /** Testa o banco e devolve a versão do build. */
    @GetMapping("/readyz")
    public ResponseEntity<Map<String, String>> pronta() {
        if (!banco.disponivel()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "DOWN", "versao", versao.valor()));
        }
        return ResponseEntity.ok(Map.of("status", "UP", "versao", versao.valor()));
    }

    /** Serve ao front para detectar que o backend acordou. */
    @GetMapping("/api/public/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("ok", true, "versao", versao.valor()));
    }
}
