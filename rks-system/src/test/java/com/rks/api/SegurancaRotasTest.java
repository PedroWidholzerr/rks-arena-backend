package com.rks.api;

import com.rks.application.administradores.AdministradorService;
import com.rks.application.autenticacao.AutenticacaoService;
import com.rks.infra.persistencia.UsuarioRepositorio;
import com.rks.infra.persistencia.entidade.UsuarioEntidade;
import com.rks.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AD-14: duas áreas de API com negação por padrão. Roda sem banco: repositório e serviços são simulados. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SegurancaRotasTest {

    private static final String EMAIL = "admin@rks.com";

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwt;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mapeamentos;

    @MockitoBean
    UsuarioRepositorio usuarios;

    @MockitoBean
    AdministradorService administradores;

    @MockitoBean
    AutenticacaoService autenticacao;

    private String tokenDeAdmin(boolean trocarSenha) {
        UsuarioEntidade usuario = new UsuarioEntidade(EMAIL, "hash", trocarSenha);
        when(usuarios.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario));
        return "Bearer " + jwt.generateToken(EMAIL);
    }

    @Test
    void areaPublicaAceitaGetSemToken() throws Exception {
        mvc.perform(get("/api/public/status")).andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true));
        mvc.perform(get("/readyz")).andExpect(status().is(org.hamcrest.Matchers.anyOf(
                org.hamcrest.Matchers.is(200), org.hamcrest.Matchers.is(503))));
    }

    @Test
    void areaPublicaNaoAceitaEscrita() throws Exception {
        mvc.perform(post("/api/public/status")).andExpect(status().isUnauthorized());
    }

    @Test
    void areaAdminSemTokenRetorna401() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void areaAdminComTokenDeAdminRetorna200() throws Exception {
        String token = tokenDeAdmin(false);
        when(administradores.listar()).thenReturn(List.of(
                new AdministradorService.Administrador(UUID.randomUUID(), EMAIL, "Pedro", true, false, Instant.now())));

        mvc.perform(get("/api/admin/users").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(EMAIL));
    }

    @Test
    void tokenSemPapelDeAdminRetorna403() throws Exception {
        String token = tokenDeAdmin(true); // ainda precisa trocar a senha: só tem ROLE_SENHA_PENDENTE

        mvc.perform(get("/api/admin/users").header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    void quemPrecisaTrocarASenhaSoAcessaATroca() throws Exception {
        String token = tokenDeAdmin(true);
        when(autenticacao.trocarSenha(anyString(), anyString(), anyString()))
                .thenReturn(new AutenticacaoService.Sessao("novo-token", false, Instant.now(), "Pedro"));

        mvc.perform(post("/api/auth/password").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"antiga-senha-1\",\"novaSenha\":\"nova-senha-forte\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("novo-token"));
        mvc.perform(patch("/api/admin/users/" + UUID.randomUUID() + "/active").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void trocarASenhaSemTokenRetorna401() throws Exception {
        mvc.perform(post("/api/auth/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"x\",\"novaSenha\":\"y\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEhLivre() throws Exception {
        when(autenticacao.entrar(anyString(), anyString()))
                .thenReturn(new AutenticacaoService.Sessao("tok", true, Instant.now(), null));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"senha\":\"qualquer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trocarSenha").value(true));
    }

    @Test
    void cadastroAbertoNaoExiste() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").header("Authorization", tokenDeAdmin(false))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rotaDesconhecidaEhNegadaPorPadrao() throws Exception {
        mvc.perform(get("/api/qualquer-coisa")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/qualquer-coisa").header("Authorization", tokenDeAdmin(false)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenAdulteradoRetorna401() throws Exception {
        String valido = tokenDeAdmin(false);
        String adulterado = valido.substring(0, valido.length() - 6) + "AAAAAA"; // troca os bytes finais da assinatura
        mvc.perform(get("/api/admin/users").header("Authorization", adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoRetorna401() throws Exception {
        tokenDeAdmin(false);
        JwtService expirado = new JwtService();
        campo(expirado, "secretKey", "segredo-somente-para-testes-com-mais-de-32-bytes");
        campo(expirado, "jwtExpiration", -60_000L);
        expirado.init();

        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + expirado.generateToken(EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administradorDesativadoPerdeOAcessoNaHora() throws Exception {
        UsuarioEntidade desativado = new UsuarioEntidade(EMAIL, "hash", false);
        desativado.setAtivo(false);
        when(usuarios.buscarPorEmail(EMAIL)).thenReturn(Optional.of(desativado));

        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + jwt.generateToken(EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadaRotaRegistradaCaiEmExatamenteUmaArea() {
        Set<String> permitidosEmAuth = Set.of("/api/auth/login", "/api/auth/password");
        Set<String> infra = Set.of("/healthz", "/livez", "/readyz", "/error");

        for (RequestMappingInfo info : mapeamentos.getHandlerMethods().keySet()) {
            Set<String> padroes = info.getPathPatternsCondition().getPatternValues();
            Set<RequestMethod> metodos = info.getMethodsCondition().getMethods();
            for (String padrao : padroes) {
                if (infra.contains(padrao)) {
                    assertTrue(metodos.isEmpty() || metodos.equals(Set.of(RequestMethod.GET)) || padrao.equals("/error"),
                            "Rota de infraestrutura só aceita GET: " + padrao + " " + metodos);
                } else if (padrao.startsWith("/api/public/")) {
                    assertEquals(Set.of(RequestMethod.GET), metodos,
                            "A área pública é só leitura (Inscrição entra na O3): " + padrao + " " + metodos);
                } else if (padrao.startsWith("/api/admin/")) {
                    assertTrue(!metodos.isEmpty(), "Declare o método HTTP em " + padrao);
                } else if (padrao.startsWith("/api/auth/")) {
                    assertTrue(permitidosEmAuth.contains(padrao), "Rota em /api/auth fora da lista: " + padrao);
                } else {
                    throw new AssertionError("Rota fora das áreas permitidas (AD-14): " + padrao + " " + metodos);
                }
            }
        }
    }

    private static void campo(Object alvo, String nome, Object valor) {
        try {
            Field f = alvo.getClass().getDeclaredField(nome);
            f.setAccessible(true);
            f.set(alvo, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
