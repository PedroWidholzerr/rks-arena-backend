package com.rks.infra.armazenamento;

import com.rks.application.erro.ErroDeRegra;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessadorDeImagemTest {

    private static byte[] imagem(String formato, int largura, int altura) throws IOException {
        boolean jpeg = formato.equals("jpg");
        BufferedImage img = new BufferedImage(largura, altura, jpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, largura, altura);
        g.setColor(Color.BLUE);
        g.fillOval(largura / 4, altura / 4, largura / 2, altura / 2);
        g.dispose();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(img, formato, saida);
        return saida.toByteArray();
    }

    private static BufferedImage ler(byte[] dados) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(dados));
    }

    private static String codigo(byte[] dados) {
        return assertThrows(ErroDeRegra.class, () -> ProcessadorDeImagem.processar(dados)).codigo();
    }

    @Test
    void pngGrandeEReduzidoPara512pxMantendoAProporcao() throws IOException {
        ProcessadorDeImagem.Processada p = ProcessadorDeImagem.processar(imagem("png", 1024, 512));

        BufferedImage saida = ler(p.conteudo());
        assertEquals("image/png", p.contentType());
        assertEquals("png", p.extensao());
        assertEquals(512, saida.getWidth());
        assertEquals(256, saida.getHeight());
    }

    @Test
    void jpegGrandeEReduzidoERegravadoComoJpeg() throws IOException {
        ProcessadorDeImagem.Processada p = ProcessadorDeImagem.processar(imagem("jpg", 400, 1200));

        BufferedImage saida = ler(p.conteudo());
        assertEquals("image/jpeg", p.contentType());
        assertEquals("jpg", p.extensao());
        assertEquals(171, saida.getWidth());
        assertEquals(512, saida.getHeight());
    }

    @Test
    void imagemPequenaNaoEAmpliada() throws IOException {
        BufferedImage saida = ler(ProcessadorDeImagem.processar(imagem("png", 100, 80)).conteudo());

        assertEquals(100, saida.getWidth());
        assertEquals(80, saida.getHeight());
    }

    @Test
    void jpegSaiSemMetadados() throws IOException {
        // JPEG de entrada com um comentário (metadado) embutido.
        BufferedImage img = new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB);
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream entrada = new ByteArrayOutputStream();
        try (ImageOutputStream saidaImg = ImageIO.createImageOutputStream(entrada)) {
            escritor.setOutput(saidaImg);
            IIOMetadata metadados = escritor.getDefaultImageMetadata(
                    javax.imageio.ImageTypeSpecifier.createFromRenderedImage(img), null);
            javax.imageio.metadata.IIOMetadataNode raiz =
                    (javax.imageio.metadata.IIOMetadataNode) metadados.getAsTree("javax_imageio_jpeg_image_1.0");
            javax.imageio.metadata.IIOMetadataNode sequencia =
                    (javax.imageio.metadata.IIOMetadataNode) raiz.getElementsByTagName("markerSequence").item(0);
            javax.imageio.metadata.IIOMetadataNode comentario = new javax.imageio.metadata.IIOMetadataNode("com");
            comentario.setAttribute("comment", "SEGREDO-NO-METADADO");
            sequencia.appendChild(comentario);
            metadados.mergeTree("javax_imageio_jpeg_image_1.0", raiz);
            escritor.write(null, new IIOImage(img, null, metadados), null);
        } finally {
            escritor.dispose();
        }
        assertTrue(new String(entrada.toByteArray(), StandardCharsets.ISO_8859_1).contains("SEGREDO-NO-METADADO"),
                "a imagem de entrada precisa ter o metadado para o teste valer");

        byte[] saida = ProcessadorDeImagem.processar(entrada.toByteArray()).conteudo();

        assertFalse(new String(saida, StandardCharsets.ISO_8859_1).contains("SEGREDO-NO-METADADO"));
    }

    @Test
    void webpEhAceitoPorAssinaturaEGuardadoComoVeio() {
        byte[] webp = new byte[64];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, webp, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, webp, 8, 4);

        ProcessadorDeImagem.Processada p = ProcessadorDeImagem.processar(webp);

        assertEquals("image/webp", p.contentType());
        assertEquals("webp", p.extensao());
        assertArrayEquals(webp, p.conteudo());
    }

    @Test
    void conteudoQueNaoEImagemEhRejeitadoMesmoComNomeDeImagem() {
        byte[] texto = "<?php echo 'oi'; ?> isto.png".getBytes(StandardCharsets.UTF_8);

        assertEquals("IMAGEM_FORMATO_INVALIDO", codigo(texto));
    }

    @Test
    void gifEOutrosFormatosNaoSaoAceitos() {
        byte[] gif = "GIF89a....................".getBytes(StandardCharsets.US_ASCII);

        assertEquals("IMAGEM_FORMATO_INVALIDO", codigo(gif));
    }

    @Test
    void assinaturaDePngComCorpoCorrompidoEhRejeitada() {
        byte[] falso = new byte[200];
        new Random(1).nextBytes(falso);
        System.arraycopy(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, 0, falso, 0, 8);

        assertEquals("IMAGEM_FORMATO_INVALIDO", codigo(falso));
    }

    @Test
    void maisDe2MbEhRejeitado() {
        assertEquals("IMAGEM_GRANDE", codigo(new byte[ProcessadorDeImagem.LIMITE_BYTES + 1]));
    }

    @Test
    void vazioEhRejeitado() {
        assertEquals("IMAGEM_VAZIA", codigo(new byte[0]));
        assertEquals("IMAGEM_VAZIA", codigo(null));
    }

    @Test
    void resultadoSempreDecodificaESaiComOTipoCerto() throws IOException {
        for (String formato : new String[]{"png", "jpg"}) {
            byte[] saida = ProcessadorDeImagem.processar(imagem(formato, 700, 300)).conteudo();

            assertNotNull(ler(saida), "a saída em " + formato + " precisa ser uma imagem válida");
        }
    }
}
