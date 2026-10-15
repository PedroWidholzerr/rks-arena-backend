package com.rks.api.admin;

import com.rks.application.administradores.AdministradorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdministradoresController {

    public record NovoAdministradorRequest(@NotBlank String email) {
    }

    public record AtivoRequest(@NotNull Boolean ativo) {
    }

    private final AdministradorService administradores;

    public AdministradoresController(AdministradorService administradores) {
        this.administradores = administradores;
    }

    @GetMapping
    public List<AdministradorService.Administrador> listar() {
        return administradores.listar();
    }

    /** Devolve a senha temporária uma única vez; ela nunca é gravada em texto nem registrada em log. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdministradorService.Criado criar(Authentication quem, @Valid @RequestBody NovoAdministradorRequest pedido) {
        return administradores.criar(quem.getName(), pedido.email());
    }

    @PatchMapping("/{id}/active")
    public AdministradorService.Administrador definirAtivo(
            Authentication quem, @PathVariable UUID id, @Valid @RequestBody AtivoRequest pedido) {
        return administradores.definirAtivo(quem.getName(), id, pedido.ativo());
    }
}
