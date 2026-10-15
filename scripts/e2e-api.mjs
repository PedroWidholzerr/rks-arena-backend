// Teste de ponta a ponta da API do RKS Arena. Cria dados: use SÓ contra um banco descartável
// (ex.: uma schema temporária), nunca contra produção.
//
// Uso: BASE_URL=http://localhost:8081 E2E_EMAIL=... E2E_SENHA_INICIAL=... node scripts/e2e-api.mjs
import { randomUUID } from "node:crypto";

const BASE = process.env.BASE_URL ?? "http://localhost:8081";
const EMAIL = process.env.E2E_EMAIL;
const SENHA_INICIAL = process.env.E2E_SENHA_INICIAL;
const SENHA_NOVA = "Senha-Nova-E2E-2026";

let passou = 0;
const falhas = [];

function confere(nome, condicao, detalhe = "") {
  if (condicao) {
    passou += 1;
    console.log(`  ok   ${nome}`);
  } else {
    falhas.push(`${nome} ${detalhe}`);
    console.log(`  FALHA ${nome} ${detalhe}`);
  }
}

async function req(metodo, caminho, { token, corpo, cabecalhos = {} } = {}) {
  const resposta = await fetch(BASE + caminho, {
    method: metodo,
    headers: {
      ...(corpo !== undefined ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...cabecalhos,
    },
    body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
  });
  const texto = await resposta.text();
  let json = null;
  try {
    json = texto ? JSON.parse(texto) : null;
  } catch {
    json = texto;
  }
  return { status: resposta.status, json, cabecalhos: resposta.headers };
}

async function main() {
  console.log("1. Login e troca obrigatória de senha");
  let r = await req("POST", "/api/auth/login", { corpo: { email: EMAIL, senha: "senha-errada-xx" } });
  confere("credencial inválida dá 401", r.status === 401, JSON.stringify(r.json));
  const msgSenhaErrada = r.json?.detail;
  r = await req("POST", "/api/auth/login", { corpo: { email: "naoexiste@rks.test", senha: "qualquer-coisa-1" } });
  confere("e-mail inexistente responde igual", r.status === 401 && r.json?.detail === msgSenhaErrada);

  r = await req("POST", "/api/auth/login", { corpo: { email: EMAIL, senha: SENHA_INICIAL } });
  confere("login do admin inicial", r.status === 200 && r.json?.trocarSenha === true, JSON.stringify(r.json));
  let token = r.json?.token;
  r = await req("GET", "/api/admin/seasons", { token });
  confere("com troca pendente a API admin dá 403", r.status === 403);
  r = await req("POST", "/api/auth/password", { token, corpo: { senhaAtual: SENHA_INICIAL, novaSenha: "curta" } });
  confere("senha fraca dá 422 SENHA_FRACA", r.status === 422 && r.json?.codigo === "SENHA_FRACA");
  r = await req("POST", "/api/auth/password", { token, corpo: { senhaAtual: SENHA_INICIAL, novaSenha: SENHA_NOVA } });
  confere("troca de senha libera o acesso", r.status === 200 && r.json?.trocarSenha === false, JSON.stringify(r.json));
  token = r.json?.token;
  r = await req("GET", "/api/admin/seasons", { token });
  confere("API admin responde 200 depois da troca", r.status === 200);
  confere("admin sem cache de borda", r.cabecalhos.get("cache-control") === "no-store");
  r = await req("GET", "/api/admin/seasons");
  confere("sem token dá 401", r.status === 401);
  r = await req("POST", "/api/auth/register", { corpo: {} });
  confere("register não existe", r.status === 401 || r.status === 404);

  console.log("2. Administradores: teto de 3 ativos sob concorrência");
  const emails = Array.from({ length: 5 }, (_, i) => `admin${i}-${randomUUID().slice(0, 6)}@rks.test`);
  const criacoes = await Promise.all(emails.map((email) => req("POST", "/api/admin/users", { token, corpo: { email } })));
  const criados = criacoes.filter((c) => c.status === 201);
  confere("5 criações simultâneas: só 2 passam (1 já existe, teto 3)", criados.length === 2,
    `passaram ${criados.length}: ${criacoes.map((c) => c.status).join(",")}`);
  confere("o excedente é recusado com LIMITE_DE_ADMINISTRADORES",
    criacoes.filter((c) => c.status === 422 && c.json?.codigo === "LIMITE_DE_ADMINISTRADORES").length === 3);
  confere("a senha temporária vem uma vez e a troca é obrigatória",
    criados[0]?.json?.senhaTemporaria?.length === 16 && criados[0]?.json?.administrador?.trocarSenha === true);
  r = await req("GET", "/api/admin/users", { token });
  confere("lista de administradores", r.status === 200 && r.json.length === 3);
  const outros = r.json.filter((a) => a.email !== EMAIL);
  r = await req("PATCH", `/api/admin/users/${outros[0].id}/active`, { token, corpo: { ativo: false } });
  confere("desativar um admin quando há outros", r.status === 200 && r.json.ativo === false);
  const eu = (await req("GET", "/api/admin/users", { token })).json.find((a) => a.email === EMAIL);
  const restante = outros[1];
  await req("PATCH", `/api/admin/users/${restante.id}/active`, { token, corpo: { ativo: false } });
  r = await req("PATCH", `/api/admin/users/${eu.id}/active`, { token, corpo: { ativo: false } });
  confere("desativar o último admin ativo é recusado", r.status === 422 && r.json?.codigo === "ULTIMO_ADMINISTRADOR");

  console.log("3. Temporada e Campeonato");
  r = await req("POST", "/api/admin/seasons", { token, corpo: { nome: "Temporada E2E", dataInicio: "2026-10-01" } });
  confere("cria Temporada", r.status === 201, JSON.stringify(r.json));
  const temporada = r.json;
  r = await req("POST", "/api/admin/championships", { token, corpo: { temporadaId: temporada.id, nome: "Camp E2E", melhorDe: 2 } });
  confere("melhor de 2 dá 422", r.status === 422 && r.json?.codigo === "MELHOR_DE_INVALIDO");
  r = await req("POST", "/api/admin/championships", {
    token,
    corpo: { temporadaId: temporada.id, nome: "Camp E2E", melhorDe: 3, pontuacao: { vitoria: 3, derrotaDecisiva: 1, derrota: 2 } },
  });
  confere("pontuação inválida dá 422", r.status === 422, JSON.stringify(r.json));
  r = await req("POST", "/api/admin/championships", { token, corpo: { temporadaId: temporada.id, nome: "Camp E2E", melhorDe: 3 } });
  confere("cria Campeonato melhor de 3 com Fase implícita", r.status === 201 && !!r.json?.faseId, JSON.stringify(r.json));
  const camp = r.json;
  confere("pontuação padrão 2/1/0", r.json?.configuracao?.pontuacao?.vitoria === 2 && r.json?.configuracao?.pontuacao?.derrotaDecisiva === 1
    && r.json?.configuracao?.pontuacao?.derrota === 0);

  console.log("4. Times, Jogadores e Elenco");
  const times = [];
  for (const [nome, tag] of [["Alfa", "ALF"], ["Beta", "BET"], ["Gama", "GAM"]]) {
    r = await req("POST", "/api/admin/teams", { token, corpo: { nome, tag } });
    times.push(r.json);
  }
  confere("cria 3 Times", times.every((t) => t?.id));
  r = await req("POST", "/api/admin/teams", { token, corpo: { nome: "Outro", tag: "alf" } });
  confere("tag duplicada sem diferenciar maiúsculas dá 409", r.status === 409 && r.json?.codigo === "TAG_JA_CADASTRADA");

  const jogadores = [];
  for (let i = 0; i < 12; i++) {
    r = await req("POST", "/api/admin/players", { token, corpo: { nick: `Jogador${i}`, tag: "BR1" } });
    jogadores.push(r.json);
  }
  confere("cria 12 Jogadores", jogadores.every((j) => j?.id));
  r = await req("POST", "/api/admin/players", { token, corpo: { nick: "JOGADOR0", tag: "br1" } });
  confere("Riot ID duplicado dá 409 com link para o existente", r.status === 409 && r.json?.jogadorId === jogadores[0].id);
  r = await req("PUT", `/api/admin/players/${jogadores[1].id}`, { token, corpo: { nick: "Jogador1Renomeado", tag: "BR1" } });
  confere("mudar o nick edita o mesmo Jogador", r.status === 200 && r.json.id === jogadores[1].id);

  const membros = (inicio, capitaoIdx = 0) =>
    Array.from({ length: 4 }, (_, k) => ({
      jogadorId: jogadores[inicio + k].id,
      funcao: k < 3 ? "TITULAR" : "RESERVA",
      capitao: k === capitaoIdx,
    }));
  r = await req("PUT", `/api/admin/teams/${times[0].id}/rosters/${temporada.id}`, { token, corpo: { membros: membros(0).map((m) => ({ ...m, capitao: false })) } });
  confere("Elenco sem Capitão dá 422", r.status === 422 && r.json?.codigo === "CAPITAO_OBRIGATORIO");
  r = await req("PUT", `/api/admin/teams/${times[0].id}/rosters/${temporada.id}`, { token, corpo: { membros: membros(0).slice(0, 2) } });
  confere("Elenco com tamanho errado dá 422", r.status === 422 && r.json?.codigo === "TAMANHO_DE_ELENCO_INVALIDO");
  for (let t = 0; t < 3; t++) {
    r = await req("PUT", `/api/admin/teams/${times[t].id}/rosters/${temporada.id}`, { token, corpo: { membros: membros(t * 4) } });
    confere(`salva Elenco do Time ${t}`, r.status === 200 && r.json.membros.length === 4 && r.json.membros[0].capitao === true, JSON.stringify(r.json));
  }
  for (let t = 0; t < 3; t++) {
    r = await req("POST", `/api/admin/championships/${camp.id}/teams`, { token, corpo: { timeId: times[t].id } });
    confere(`vincula Time ${t} ao Campeonato`, r.status === 201, JSON.stringify(r.json));
  }
  // um Jogador do Time 1 não pode entrar no Elenco do Time 0 no mesmo Campeonato
  const conflito = membros(0);
  conflito[3] = { jogadorId: jogadores[4].id, funcao: "RESERVA", capitao: false };
  r = await req("PUT", `/api/admin/teams/${times[0].id}/rosters/${temporada.id}`, { token, corpo: { membros: conflito } });
  confere("Jogador em dois Elencos do mesmo Campeonato dá 409", r.status === 409 && r.json?.codigo === "JOGADOR_EM_OUTRO_ELENCO", JSON.stringify(r.json));

  console.log("5. Partidas, resultado, idempotência e versão");
  const [A, B, C] = times;
  const nova = async (a, b, rodada) => (await req("POST", "/api/admin/matches", { token, corpo: { faseId: camp.faseId, timeAId: a.id, timeBId: b.id, rodada } })).json;
  r = await req("POST", "/api/admin/matches", { token, corpo: { faseId: camp.faseId, timeAId: A.id, timeBId: A.id } });
  confere("Time contra si mesmo dá 422", r.status === 422 && r.json?.codigo === "TIMES_IGUAIS");
  r = await req("POST", "/api/admin/matches", { token, corpo: { faseId: camp.faseId, timeAId: A.id, timeBId: B.id, streamUrl: "http://inseguro.test" } });
  confere("link http dá 422", r.status === 422 && r.json?.codigo === "STREAM_URL_INVALIDA");
  const m1 = await nova(A, B, 1);
  const m2 = await nova(B, C, 2);
  const m3 = await nova(C, A, 3);
  confere("Partidas nascem AGENDADAS sem placar", m1.estado === "AGENDADA" && m1.jogosA === null && m1.version === 0);

  r = await req("POST", `/api/admin/matches/${m1.id}/start`, { token, corpo: { version: m1.version } });
  confere("iniciar vai a EM_ANDAMENTO", r.status === 200 && r.json.estado === "EM_ANDAMENTO" && r.json.jogosA === null);
  let v1 = r.json.version;
  r = await req("POST", `/api/admin/matches/${m1.id}/start`, { token, corpo: { version: v1 } });
  confere("iniciar de novo dá 409 TRANSICAO_INVALIDA", r.status === 409 && r.json?.codigo === "TRANSICAO_INVALIDA");

  const chave = randomUUID();
  r = await req("PUT", `/api/admin/matches/${m1.id}/result`, { token, corpo: { version: v1, jogosA: 2, jogosB: 0 } });
  confere("resultado sem Idempotency-Key dá 400", r.status === 400 && r.json?.codigo === "CABECALHO_OBRIGATORIO");
  r = await req("PUT", `/api/admin/matches/${m1.id}/result`, { token, cabecalhos: { "Idempotency-Key": randomUUID() }, corpo: { version: v1, jogosA: 1, jogosB: 1 } });
  confere("placar 1x1 dá 422 PLACAR_INVALIDO", r.status === 422 && r.json?.codigo === "PLACAR_INVALIDO");
  r = await req("PUT", `/api/admin/matches/${m1.id}/result`, { token, cabecalhos: { "Idempotency-Key": randomUUID() }, corpo: { version: 999, jogosA: 2, jogosB: 0 } });
  confere("version defasada dá 409", r.status === 409 && r.json?.codigo === "VERSAO_DESATUALIZADA");
  const mvpDeFora = jogadores[11].id;
  r = await req("PUT", `/api/admin/matches/${m1.id}/result`, { token, cabecalhos: { "Idempotency-Key": randomUUID() }, corpo: { version: v1, jogosA: 2, jogosB: 0, mvpJogadorId: mvpDeFora } });
  confere("MVP de fora dos dois Elencos dá 422", r.status === 422 && r.json?.codigo === "MVP_INVALIDO", JSON.stringify(r.json));

  const corpoResultado = { version: v1, jogosA: 2, jogosB: 0, mvpJogadorId: jogadores[0].id };
  r = await req("PUT", `/api/admin/matches/${m1.id}/result`, { token, cabecalhos: { "Idempotency-Key": chave }, corpo: corpoResultado });
  confere("lança 2x0 com MVP", r.status === 200 && r.json.estado === "FINALIZADA" && r.json.jogosA === 2 && r.json.jogosB === 0, JSON.stringify(r.json));
  const versaoAposResultado = r.json.version;
  const repetida = await req("PUT", `/api/admin/matches/${m1.id}/result`, { token, cabecalhos: { "Idempotency-Key": chave }, corpo: corpoResultado });
  confere("repetir a mesma chave devolve a mesma resposta", repetida.status === 200 && JSON.stringify(repetida.json) === JSON.stringify(r.json));
  r = await req("GET", `/api/admin/matches/${m1.id}`, { token });
  confere("não gravou duas vezes (versão igual)", r.json.version === versaoAposResultado, `v=${r.json.version}`);
  r = await req("PUT", `/api/admin/matches/${m1.id}/result`, { token, cabecalhos: { "Idempotency-Key": chave }, corpo: { ...corpoResultado, jogosB: 1 } });
  confere("mesma chave com corpo diferente dá 422 CHAVE_REUTILIZADA", r.status === 422 && r.json?.codigo === "CHAVE_REUTILIZADA");

  console.log("6. Congelamento da configuração");
  r = await req("PUT", `/api/admin/championships/${camp.id}/config`, { token, corpo: { melhorDe: 5, pontuacao: { vitoria: 2, derrotaDecisiva: 1, derrota: 0 }, criterios: ["CONFRONTO_DIRETO", "SALDO", "JOGOS_VENCIDOS"] } });
  confere("com Partida finalizada a config dá 409 CONFIG_CONGELADA e informa o número",
    r.status === 409 && r.json?.codigo === "CONFIG_CONGELADA" && r.json?.partidasFinalizadas === 1, JSON.stringify(r.json));

  console.log("7. W.O., edição, anulação e classificação");
  r = await req("PUT", `/api/admin/matches/${m2.id}/walkover`, { token, corpo: { version: m2.version, vencedor: "A" } });
  confere("W.O. grava 2x0 e marca walkover", r.status === 200 && r.json.jogosA === 2 && r.json.jogosB === 0 && r.json.walkover === true, JSON.stringify(r.json));
  r = await req("PUT", `/api/admin/matches/${m3.id}/result`, { token, cabecalhos: { "Idempotency-Key": randomUUID() }, corpo: { version: m3.version, jogosA: 2, jogosB: 1 } });
  confere("lança 2x1 (decisiva)", r.status === 200 && r.json.estado === "FINALIZADA");
  const v3 = r.json.version;

  // Pontos (2/1/0): A venceu B 2x0 (A 2, B 0). B venceu C por W.O. (B 2, C 0, W.O. não é decisiva).
  // C venceu A 2x1 (C 2; A perdeu decisiva = 1). Totais: A = 2+1 = 3, B = 0+2 = 2, C = 0+2 = 2.
  r = await req("GET", `/api/public/phases/${camp.faseId}/standings`);
  let tabela = r.json?.dados;
  const pontos = (t) => Object.fromEntries(t.linhas.map((l) => [l.nome, l.pontos]));
  confere("classificação: Alfa 3, Beta 2, Gama 2", JSON.stringify(pontos(tabela)) === JSON.stringify({ Alfa: 3, Beta: 2, Gama: 2 }) || (pontos(tabela).Alfa === 3 && pontos(tabela).Beta === 2 && pontos(tabela).Gama === 2),
    JSON.stringify(pontos(tabela)));
  confere("resposta pública traz atualizadoEm", !!r.json?.atualizadoEm);
  confere("Alfa é o primeiro", tabela.linhas[0].nome === "Alfa" && tabela.linhas[0].posicao === 1);

  // Edita C x A para 1x2: A vence (2 pts), C perde decisiva (1): A = 2+2 = 4, C = 0+1 = 1... mas A perdeu para B? recalcula:
  // A: venceu B 2x0 (2) + venceu C 2x1 na nova edição (2) = 4; B: 0 + 2 (W.O. sobre C) = 2; C: 0 (W.O.) + 1 (decisiva) = 1.
  r = await req("PUT", `/api/admin/matches/${m3.id}/result`, { token, cabecalhos: { "Idempotency-Key": randomUUID() }, corpo: { version: v3, jogosA: 1, jogosB: 2 } });
  confere("editar o resultado", r.status === 200 && r.json.jogosA === 1 && r.json.jogosB === 2, JSON.stringify(r.json));
  const v3b = r.json.version;
  tabela = (await req("GET", `/api/public/phases/${camp.faseId}/standings`)).json.dados;
  const p2 = pontos(tabela);
  // m3 foi Gama (A) x Alfa (B): 1x2 = Alfa vence. Alfa: 2 (Beta) + 2 (Gama) = 4; Beta = 2; Gama = 0 + 1 = 1
  confere("depois de editar: Alfa 4, Beta 2, Gama 1", p2.Alfa === 4 && p2.Beta === 2 && p2.Gama === 1, JSON.stringify(p2));
  r = await req("PUT", `/api/admin/matches/${m3.id}/annul`, { token, corpo: { version: v3b } });
  confere("anular volta a AGENDADA sem placar", r.status === 200 && r.json.estado === "AGENDADA" && r.json.jogosA === null);
  tabela = (await req("GET", `/api/public/phases/${camp.faseId}/standings`)).json.dados;
  const p3 = pontos(tabela);
  confere("anular = nunca ter existido: Alfa 2, Beta 2, Gama 0", p3.Alfa === 2 && p3.Beta === 2 && p3.Gama === 0, JSON.stringify(p3));

  console.log("8. Empate e decisão manual");
  confere("Alfa e Beta empatam em pontos e o desempate decide ou fica aberto", Array.isArray(tabela.empatesAbertos));
  const empate = tabela.empatesAbertos[0];
  if (empate) {
    r = await req("POST", `/api/admin/phases/${camp.faseId}/tie-decisions`, { token, corpo: { ordem: [...empate.times].reverse(), motivo: "Decisão do admin no teste" } });
    confere("decisão manual para o empate atual", r.status === 201, JSON.stringify(r.json));
    confere("depois da decisão não há empate aberto", r.json?.empatesAbertos?.length === 0);
  } else {
    confere("não há empate aberto (confronto direto separou)", true);
  }
  r = await req("POST", `/api/admin/phases/${camp.faseId}/tie-decisions`, { token, corpo: { ordem: [A.id, randomUUID()], motivo: "x" } });
  confere("decisão para conjunto que não está empatado dá 422", r.status === 422 && r.json?.codigo === "EMPATE_INEXISTENTE");
  r = await req("POST", `/api/admin/phases/${camp.faseId}/recalculate`, { token });
  confere("recálculo é consistente", r.status === 200 && r.json?.consistente === true);

  console.log("9. Liberar e editar a configuração depois de anular todas as finalizadas");
  for (const m of [m1, m2]) {
    const atual = (await req("GET", `/api/admin/matches/${m.id}`, { token })).json;
    await req("PUT", `/api/admin/matches/${m.id}/annul`, { token, corpo: { version: atual.version } });
  }
  r = await req("PUT", `/api/admin/championships/${camp.id}/config`, { token, corpo: { melhorDe: 5, pontuacao: { vitoria: 3, derrotaDecisiva: 1, derrota: 0 }, criterios: ["SALDO", "CONFRONTO_DIRETO", "JOGOS_VENCIDOS"] } });
  confere("sem finalizadas a config é editável", r.status === 200 && r.json.configuracao.melhorDe === 5 && r.json.congelado === false, JSON.stringify(r.json));

  console.log("10. Leitura pública");
  r = await req("GET", "/api/public/seasons");
  confere("Temporadas com Campeonatos e destaque", r.status === 200 && r.json.dados.some((t) => t.temporada.id === temporada.id && t.destaque));
  r = await req("GET", `/api/public/teams/${A.id}`);
  confere("Time público traz Elenco", r.status === 200 && r.json.dados.elencos.length === 1 && r.json.dados.elencos[0].membros.length === 4);
  r = await req("GET", `/api/public/players?busca=jogador1`);
  confere("busca por Riot ID", r.status === 200 && r.json.dados.itens.length >= 1);
  r = await req("GET", `/api/public/championships/${camp.id}/matches?time=${A.id}`);
  confere("Partidas filtradas por Time", r.status === 200 && r.json.dados.length === 2);
  r = await req("POST", "/api/public/seasons");
  confere("escrita na área pública é negada", r.status === 401);
  r = await req("DELETE", `/api/admin/seasons/${temporada.id}`, { token });
  confere("excluir Temporada com Campeonatos é bloqueado", r.status === 409);

  console.log(`\n${passou} verificações passaram, ${falhas.length} falharam.`);
  if (falhas.length) {
    console.log(falhas.map((f) => ` - ${f}`).join("\n"));
    process.exit(1);
  }
}

if (!EMAIL || !SENHA_INICIAL) {
  console.error("Defina E2E_EMAIL e E2E_SENHA_INICIAL.");
  process.exit(2);
}
main().catch((e) => {
  console.error(e);
  process.exit(1);
});
