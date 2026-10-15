package com.rks.api.admin;

import com.rks.application.erro.ErroDeRegra;
import com.rks.application.imagens.ImagemService;
import com.rks.application.times.JogadorService;
import com.rks.application.times.TimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/** Logo de Time e foto de Jogador, enviadas ao armazenamento externo (história 2.30). */
@RestController
@RequestMapping("/api/admin")
public class ImagensAdminController {

    private final ImagemService imagens;
    private final TimeService times;
    private final JogadorService jogadores;

    public ImagensAdminController(ImagemService imagens, TimeService times, JogadorService jogadores) {
        this.imagens = imagens;
        this.times = times;
        this.jogadores = jogadores;
    }

    @PutMapping(value = "/teams/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TimeService.Time enviarLogo(Authentication quem, @PathVariable UUID id,
                                       @RequestParam("arquivo") MultipartFile arquivo) {
        imagens.enviarLogo(quem.getName(), id, bytes(arquivo));
        return times.buscar(id);
    }

    @DeleteMapping("/teams/{id}/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerLogo(Authentication quem, @PathVariable UUID id) {
        imagens.removerLogo(quem.getName(), id);
    }

    @PutMapping(value = "/players/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JogadorService.Jogador enviarFoto(Authentication quem, @PathVariable UUID id,
                                             @RequestParam("arquivo") MultipartFile arquivo) {
        imagens.enviarFoto(quem.getName(), id, bytes(arquivo));
        return jogadores.obter(id);
    }

    @DeleteMapping("/players/{id}/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerFoto(Authentication quem, @PathVariable UUID id) {
        imagens.removerFoto(quem.getName(), id);
    }

    private static byte[] bytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw ErroDeRegra.regra("IMAGEM_ILEGIVEL", "Não foi possível ler o arquivo enviado.");
        }
    }
}
