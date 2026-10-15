package com.rks.api;

import com.rks.application.erro.ErroDeRegra;
import com.rks.domain.rules.PlacarInvalidoException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Traduz erros para application/problem+json (RFC 9457) com um {@code codigo} estável em maiúsculas:
 * 400 só para formato, 401 credencial, 404 não encontrado, 409 conflito de estado ou de versão,
 * 422 regra violada. O {@code detail} é sempre em português e nunca carrega corpo de requisição.
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static ResponseEntity<ProblemDetail> resposta(HttpStatus status, String codigo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setType(URI.create("about:blank"));
        problema.setTitle(codigo);
        problema.setProperty("codigo", codigo);
        return ResponseEntity.status(status).contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON).body(problema);
    }

    @ExceptionHandler(ErroDeRegra.class)
    ResponseEntity<ProblemDetail> regra(ErroDeRegra erro) {
        HttpStatus status = switch (erro.tipo()) {
            case NAO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CONFLITO -> HttpStatus.CONFLICT;
            case REGRA -> HttpStatus.UNPROCESSABLE_ENTITY;
            case NAO_AUTENTICADO -> HttpStatus.UNAUTHORIZED;
        };
        return resposta(status, erro.codigo(), erro.getMessage());
    }

    @ExceptionHandler(PlacarInvalidoException.class)
    ResponseEntity<ProblemDetail> placar(PlacarInvalidoException erro) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, erro.codigo(), erro.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> versao(OptimisticLockingFailureException erro) {
        return resposta(HttpStatus.CONFLICT, "VERSAO_DESATUALIZADA",
                "Os dados mudaram desde que você os abriu. Recarregue e tente de novo.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException erro) {
        String campos = erro.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", "Campos inválidos: " + campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> corpoIlegivel(HttpMessageNotReadableException erro) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", "O corpo da requisição não está no formato esperado.");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ProblemDetail> cabecalhoAusente(MissingRequestHeaderException erro) {
        return resposta(HttpStatus.BAD_REQUEST, "CABECALHO_OBRIGATORIO",
                "O cabeçalho " + erro.getHeaderName() + " é obrigatório.");
    }
}
