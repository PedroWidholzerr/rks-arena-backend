package com.rks.application.times;

import com.rks.application.erro.ErroDeRegra;
import com.rks.infra.persistencia.JogadorRepositorio;
import com.rks.infra.persistencia.entidade.JogadorEntidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Jogadores por Riot ID (FR-5). {@code riot_id_key} = {@code lower(nick)#lower(tag)} é único. Mudar o
 * nick edita o mesmo Jogador, então o histórico fica. Nada exposto aqui além de Riot ID.
 */
@Service
public class JogadorService {

    public record Jogador(UUID id, String nick, String tag, String riotId, String fotoUrl, Instant criadoEm) {
    }

    private final JogadorRepositorio jogadores;

    public JogadorService(JogadorRepositorio jogadores) {
        this.jogadores = jogadores;
    }

    /** Busca por Riot ID; texto vazio lista todos. */
    @Transactional(readOnly = true)
    public List<Jogador> buscar(String busca) {
        String termo = busca == null ? "" : busca.strip().toLowerCase(Locale.ROOT);
        return jogadores.buscar(termo).stream().map(JogadorService::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public Jogador obter(UUID id) {
        return paraDto(entidade(id));
    }

    @Transactional
    public Jogador criar(String nick, String tag) {
        JogadorEntidade j = new JogadorEntidade();
        preencher(j, nick, tag);
        return paraDto(jogadores.save(j));
    }

    @Transactional
    public Jogador atualizar(UUID id, String nick, String tag) {
        JogadorEntidade j = entidade(id);
        preencher(j, nick, tag);
        return paraDto(jogadores.save(j));
    }

    private JogadorEntidade entidade(UUID id) {
        return jogadores.findById(id)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("JOGADOR_NAO_ENCONTRADO", "Jogador não encontrado."));
    }

    private void preencher(JogadorEntidade j, String nick, String tag) {
        String nickLimpo = nick == null ? "" : nick.strip();
        String tagLimpa = tag == null ? "" : tag.strip().replaceFirst("^#", "");
        if (nickLimpo.isEmpty() || nickLimpo.length() > 60 || nickLimpo.contains("#")) {
            throw ErroDeRegra.regra("NICK_INVALIDO", "O nick é obrigatório, vai até 60 caracteres e não leva #.");
        }
        if (tagLimpa.isEmpty() || tagLimpa.length() > 20 || tagLimpa.contains("#")) {
            throw ErroDeRegra.regra("TAG_INVALIDA", "A tag do Riot ID é obrigatória e vai até 20 caracteres.");
        }
        String chave = chave(nickLimpo, tagLimpa);
        jogadores.findByRiotIdKey(chave).filter(outro -> !outro.getId().equals(j.getId())).ifPresent(outro -> {
            throw ErroDeRegra.conflito("JOGADOR_JA_EXISTE", "Já existe um Jogador com o Riot ID " + nickLimpo + "#" + tagLimpa + ".",
                    Map.of("jogadorId", outro.getId()));
        });
        j.setNick(nickLimpo);
        j.setTag(tagLimpa);
        j.setRiotIdKey(chave);
    }

    public static String chave(String nick, String tag) {
        return nick.toLowerCase(Locale.ROOT) + "#" + tag.toLowerCase(Locale.ROOT);
    }

    static Jogador paraDto(JogadorEntidade j) {
        return new Jogador(j.getId(), j.getNick(), j.getTag(), j.getNick() + "#" + j.getTag(), j.getFotoUrl(), j.getCriadoEm());
    }
}
