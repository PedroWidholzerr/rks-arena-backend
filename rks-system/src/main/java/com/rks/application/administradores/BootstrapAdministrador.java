package com.rks.application.administradores;

import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cria o primeiro administrador a partir do ambiente (AD-15, FR-41), sem cadastro aberto.
 * Nunca sobrescreve administrador existente: com algum ativo, ADMIN_EMAIL e ADMIN_INITIAL_PASSWORD
 * são ignorados. A única exceção é ADMIN_RESET_ON_BOOT=true, que redefine a senha do único
 * administrador ativo por uma subida (ver docs/runbook-admin.md).
 */
@Component
@ConditionalOnProperty(name = "app.admin.bootstrap", havingValue = "true", matchIfMissing = true)
public class BootstrapAdministrador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdministrador.class);

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final String email;
    private final String senhaInicial;
    private final boolean redefinirNaSubida;

    public BootstrapAdministrador(
            UsuarioRepositorio usuarios,
            PasswordEncoder codificador,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.initial-password:}") String senhaInicial,
            @Value("${app.admin.reset-on-boot:false}") boolean redefinirNaSubida) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.email = email == null ? "" : email.strip();
        this.senhaInicial = senhaInicial == null ? "" : senhaInicial;
        this.redefinirNaSubida = redefinirNaSubida;
    }

    @Override
    public void run(ApplicationArguments args) {
        long ativos = usuarios.countByAtivoTrue();
        if (ativos == 0) {
            criarPrimeiro();
        } else if (redefinirNaSubida) {
            redefinirSenhaDoUnico();
        } else {
            log.info("Já existe administrador ativo; ADMIN_EMAIL e ADMIN_INITIAL_PASSWORD foram ignorados.");
        }
    }

    private void criarPrimeiro() {
        if (email.isEmpty() || senhaInicial.length() < 10) {
            log.warn("Não há administrador ativo e ADMIN_EMAIL ou ADMIN_INITIAL_PASSWORD (mínimo 10 caracteres) "
                    + "não foram definidos: ninguém conseguirá entrar.");
            return;
        }
        if (usuarios.buscarPorEmail(email).isPresent()) {
            log.warn("Não há administrador ativo, mas o e-mail de ADMIN_EMAIL já existe desativado; "
                    + "nada foi criado. Reative-o por outro administrador ou use ADMIN_RESET_ON_BOOT.");
            return;
        }
        usuarios.save(new UsuarioEntidade(email, codificador.encode(senhaInicial), true));
        log.info("Primeiro administrador criado a partir do ambiente; a troca de senha é obrigatória no primeiro login.");
    }

    private void redefinirSenhaDoUnico() {
        List<UsuarioEntidade> ativos = usuarios.findByAtivoTrue();
        if (ativos.size() != 1 || senhaInicial.length() < 10) {
            log.warn("ADMIN_RESET_ON_BOOT ignorado: exige exatamente 1 administrador ativo e ADMIN_INITIAL_PASSWORD "
                    + "com pelo menos 10 caracteres.");
            return;
        }
        UsuarioEntidade unico = ativos.get(0);
        unico.setSenhaHash(codificador.encode(senhaInicial));
        unico.setTrocarSenha(true);
        usuarios.save(unico);
        log.warn("Senha do único administrador redefinida por ADMIN_RESET_ON_BOOT; remova a variável depois do uso.");
    }
}
