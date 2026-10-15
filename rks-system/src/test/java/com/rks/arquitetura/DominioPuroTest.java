package com.rks.arquitetura;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garante a regra do AD-1: o pacote com.rks.domain não importa framework nem infraestrutura.
 * A varredura lê o código-fonte, sem dependência de biblioteca de arquitetura.
 */
class DominioPuroTest {

    private static final List<String> IMPORTS_PROIBIDOS = List.of(
            "org.springframework",
            "jakarta",
            "com.rks.infra",
            "com.rks.application");

    private static final Path DOMINIO = Path.of("src", "main", "java", "com", "rks", "domain");

    @Test
    void dominioNaoImportaFrameworkNemInfraestrutura() throws IOException {
        List<String> violacoes = encontrarViolacoes(DOMINIO);

        assertTrue(violacoes.isEmpty(),
                "O domínio importa dependência proibida (AD-1):\n" + String.join("\n", violacoes));
    }

    @Test
    void detectaArquivoDeExemploProibido(@TempDir Path pasta) throws IOException {
        Path arquivo = pasta.resolve("Exemplo.java");
        Files.writeString(arquivo, """
                package com.rks.domain;

                import java.util.List;
                import org.springframework.stereotype.Service;
                import static com.rks.infra.persistencia.Algo.valor;
                import jakarta.persistence.Entity;
                import com.rks.application.CasoDeUso;

                class Exemplo {}
                """);

        List<String> violacoes = encontrarViolacoes(pasta);

        assertEquals(4, violacoes.size(), violacoes.toString());
        assertTrue(violacoes.stream().allMatch(v -> v.contains("Exemplo.java")));
    }

    @Test
    void aceitaArquivoLimpo(@TempDir Path pasta) throws IOException {
        Files.writeString(pasta.resolve("Limpo.java"), """
                package com.rks.domain;

                import java.util.List;
                import com.rks.domain.outro.Tipo;

                class Limpo {}
                """);

        assertTrue(encontrarViolacoes(pasta).isEmpty());
    }

    private static List<String> encontrarViolacoes(Path raiz) throws IOException {
        if (!Files.isDirectory(raiz)) {
            return List.of();
        }
        try (Stream<Path> arquivos = Files.walk(raiz)) {
            return arquivos
                    .filter(p -> p.toString().endsWith(".java"))
                    .flatMap(DominioPuroTest::violacoesDo)
                    .toList();
        }
    }

    private static Stream<String> violacoesDo(Path arquivo) {
        try {
            return Files.readAllLines(arquivo).stream()
                    .map(String::strip)
                    .filter(linha -> linha.startsWith("import "))
                    .map(linha -> linha.replaceFirst("^import\s+(static\s+)?", ""))
                    .filter(DominioPuroTest::proibido)
                    .map(alvo -> arquivo + " importa " + alvo);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler " + arquivo, e);
        }
    }

    private static boolean proibido(String alvo) {
        return IMPORTS_PROIBIDOS.stream()
                .anyMatch(p -> alvo.equals(p + ";") || alvo.startsWith(p + ".") || alvo.startsWith(p + ";"));
    }
}
