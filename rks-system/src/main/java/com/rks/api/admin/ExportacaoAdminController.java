package com.rks.api.admin;

import com.rks.application.exportacao.ExportacaoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/** Exportação completa para o administrador guardar fora do sistema (história 2.32). */
@RestController
@RequestMapping("/api/admin")
public class ExportacaoAdminController {

    private final ExportacaoService exportacao;

    public ExportacaoAdminController(ExportacaoService exportacao) {
        this.exportacao = exportacao;
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportar() {
        ContentDisposition anexo = ContentDisposition.attachment().filename("rks-arena-" + LocalDate.now() + ".json").build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, anexo.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(exportacao.exportar());
    }
}
