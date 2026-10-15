package com.rks.infra.armazenamento;

import com.rks.application.erro.ErroDeRegra;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Valida imagem pelo CONTEÚDO, nunca pelo nome nem pelo Content-Type enviado (FR-24, AD-16).
 * PNG e JPEG são decodificados e regravados sem metadados, com no máximo 512 px; WebP é aceito
 * por assinatura e guardado como veio (sem biblioteca de decodificação, não dá para regravá-lo).
 * Limite de 2 MB.
 */
public final class ProcessadorDeImagem {

    public static final int LIMITE_BYTES = 2 * 1024 * 1024;
    public static final int LADO_MAXIMO = 512;
    private static final long LIMITE_DE_PIXELS = 25_000_000L;

    static {
        System.setProperty("java.awt.headless", "true");
    }

    public record Processada(byte[] conteudo, String contentType, String extensao) {
    }

    private ProcessadorDeImagem() {
    }

    public static Processada processar(byte[] dados) {
        if (dados == null || dados.length == 0) {
            throw ErroDeRegra.regra("IMAGEM_VAZIA", "Nenhuma imagem foi enviada.");
        }
        if (dados.length > LIMITE_BYTES) {
            throw ErroDeRegra.regra("IMAGEM_GRANDE", "A imagem passa de 2 MB. Envie um arquivo menor.");
        }
        if (ehPng(dados)) {
            return regravar(dados, "png", "image/png");
        }
        if (ehJpeg(dados)) {
            return regravar(dados, "jpg", "image/jpeg");
        }
        if (ehWebp(dados)) {
            return new Processada(dados, "image/webp", "webp");
        }
        throw ErroDeRegra.regra("IMAGEM_FORMATO_INVALIDO", "Formato não aceito. Envie uma imagem PNG, JPEG ou WebP.");
    }

    private static Processada regravar(byte[] dados, String formato, String contentType) {
        BufferedImage original = decodificar(dados);
        BufferedImage ajustada = ajustar(original, formato.equals("jpg"));
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(ajustada, formato, saida)) {
                throw ErroDeRegra.regra("IMAGEM_FORMATO_INVALIDO", "Não foi possível processar a imagem enviada.");
            }
        } catch (IOException e) {
            throw ErroDeRegra.regra("IMAGEM_FORMATO_INVALIDO", "Não foi possível processar a imagem enviada.");
        }
        return new Processada(saida.toByteArray(), contentType, formato);
    }

    private static BufferedImage decodificar(byte[] dados) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(dados))) {
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) {
                throw invalida();
            }
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada);
                // Confere as dimensões antes de decodificar, para uma imagem enorme não esgotar a memória.
                long pixels = (long) leitor.getWidth(0) * leitor.getHeight(0);
                if (pixels > LIMITE_DE_PIXELS) {
                    throw ErroDeRegra.regra("IMAGEM_GRANDE", "As dimensões da imagem são grandes demais.");
                }
                BufferedImage imagem = leitor.read(0);
                if (imagem == null) {
                    throw invalida();
                }
                return imagem;
            } finally {
                leitor.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ErroDeRegra erro) {
                throw erro;
            }
            throw invalida();
        }
    }

    /** Reduz para no máximo 512 px no maior lado e remove canal alfa para JPEG. */
    private static BufferedImage ajustar(BufferedImage origem, boolean jpeg) {
        int largura = origem.getWidth();
        int altura = origem.getHeight();
        double escala = Math.min(1.0, (double) LADO_MAXIMO / Math.max(largura, altura));
        int novaLargura = Math.max(1, (int) Math.round(largura * escala));
        int novaAltura = Math.max(1, (int) Math.round(altura * escala));
        BufferedImage destino = new BufferedImage(novaLargura, novaAltura,
                jpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (jpeg) {
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, novaLargura, novaAltura);
            }
            g.drawImage(origem, 0, 0, novaLargura, novaAltura, null);
        } finally {
            g.dispose();
        }
        return destino;
    }

    private static ErroDeRegra invalida() {
        return ErroDeRegra.regra("IMAGEM_FORMATO_INVALIDO", "O arquivo não é uma imagem válida. Envie PNG, JPEG ou WebP.");
    }

    static boolean ehPng(byte[] d) {
        return d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A;
    }

    static boolean ehJpeg(byte[] d) {
        return d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
    }

    static boolean ehWebp(byte[] d) {
        return d.length > 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P';
    }
}
