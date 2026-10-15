package com.rks.application.administradores;

import com.rks.application.auditoria.AuditoriaService;
import com.rks.application.erro.ErroDeRegra;
import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gestão de administradores (FR-41, AD-15). O teto de 3 conta só administradores ativos; os
 * desativados ficam para auditoria. Administrador nunca é excluído e o último ativo nunca é
 * desativado. Criar e (des)ativar rodam sob {@code pg_advisory_xact_lock}, então dois pedidos
 * simultâneos não furam o teto.
 */
@Service
public class AdministradorService {

    public static final int LIMITE_DE_ADMINISTRADORES_ATIVOS = 3;

    /** Chave arbitrária e fixa da trava de administradores. */
    private static final long CHAVE_DA_TRAVA = 0x524B53_41444D49L;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public record Administrador(UUID id, String email, boolean ativo, boolean trocarSenha, Instant criadoEm) {
    }

    public record Criado(Administrador administrador, String senhaTemporaria) {
    }

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final AuditoriaService auditoria;

    public AdministradorService(UsuarioRepositorio usuarios, PasswordEncoder codificador, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Administrador> listar() {
        return usuarios.findAllByOrderByCriadoEmAsc().stream().map(AdministradorService::paraDto).toList();
    }

    /** Cria o administrador com senha temporária (mostrada uma única vez) e troca obrigatória. */
    @Transactional
    public Criado criar(String autor, String email) {
        String limpo = email == null ? "" : email.strip();
        if (!EMAIL.matcher(limpo).matches()) {
            throw ErroDeRegra.regra("EMAIL_INVALIDO", "Informe um e-mail válido.");
        }
        usuarios.travar(CHAVE_DA_TRAVA);
        if (usuarios.countByAtivoTrue() >= LIMITE_DE_ADMINISTRADORES_ATIVOS) {
            throw ErroDeRegra.regra("LIMITE_DE_ADMINISTRADORES",
                    "O limite é de " + LIMITE_DE_ADMINISTRADORES_ATIVOS + " administradores ativos.");
        }
        if (usuarios.buscarPorEmail(limpo).isPresent()) {
            throw ErroDeRegra.conflito("EMAIL_JA_CADASTRADO", "Já existe um administrador com este e-mail.");
        }
        String temporaria = senhaTemporaria();
        UsuarioEntidade novo = usuarios.save(new UsuarioEntidade(limpo, codificador.encode(temporaria), true));
        auditoria.registrar(autor, "usuario", novo.getId(), "CRIAR", null, Map.of("email", novo.getEmail()));
        return new Criado(paraDto(novo), temporaria);
    }

    @Transactional
    public Administrador definirAtivo(String autor, UUID id, boolean ativo) {
        usuarios.travar(CHAVE_DA_TRAVA);
        UsuarioEntidade alvo = usuarios.findById(id)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("ADMINISTRADOR_NAO_ENCONTRADO", "Administrador não encontrado."));
        if (alvo.isAtivo() == ativo) {
            return paraDto(alvo);
        }
        long ativos = usuarios.countByAtivoTrue();
        if (!ativo && ativos <= 1) {
            throw ErroDeRegra.regra("ULTIMO_ADMINISTRADOR", "Não é possível desativar o último administrador ativo.");
        }
        if (ativo && ativos >= LIMITE_DE_ADMINISTRADORES_ATIVOS) {
            throw ErroDeRegra.regra("LIMITE_DE_ADMINISTRADORES",
                    "O limite é de " + LIMITE_DE_ADMINISTRADORES_ATIVOS + " administradores ativos.");
        }
        alvo.setAtivo(ativo);
        usuarios.save(alvo);
        auditoria.registrar(autor, "usuario", alvo.getId(), ativo ? "REATIVAR" : "DESATIVAR",
                Map.of("ativo", !ativo), Map.of("ativo", ativo));
        return paraDto(alvo);
    }

    private static Administrador paraDto(UsuarioEntidade u) {
        return new Administrador(u.getId(), u.getEmail(), u.isAtivo(), u.isTrocarSenha(), u.getCriadoEm());
    }

    private static String senhaTemporaria() {
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            senha.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return senha.toString();
    }
}
