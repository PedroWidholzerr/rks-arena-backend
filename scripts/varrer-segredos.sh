#!/usr/bin/env bash
# Falha se algum arquivo versionado tiver padrão de chave (AD-21).
# Uso: scripts/varrer-segredos.sh [raiz]   (por padrão, o repositório atual)
set -u
raiz="${1:-.}"
cd "$raiz" || exit 2

# Os padrões são montados em pedaços para este arquivo não casar com ele mesmo.
jwt='eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}'
supabase='sb_'"secret"'_[A-Za-z0-9_-]{20,}'
turnstile='0x4AAAAAAA[A-Za-z0-9_-]{20,}'
riot='RGAPI-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}'
atribuicao='(JWT_SECRET|SUPABASE_SERVICE_KEY|TURNSTILE_SECRET|RIOT_API_KEY|DB_PASSWORD|ADMIN_INITIAL_PASSWORD)[[:space:]]*[=:][[:space:]]*[^[:space:]$"'"'"'{<][^[:space:]]{7,}'

achou=0
while IFS= read -r arquivo; do
  [ -f "$arquivo" ] || continue
  case "$arquivo" in scripts/varrer-segredos.sh|package-lock.json|*.png|*.jpg|*.ico|*.webp|*.woff*) continue ;; esac
  if grep -I -n -E -e "$jwt" -e "$supabase" -e "$turnstile" -e "$riot" -e "$atribuicao" -- "$arquivo" >/tmp/achado.$$ 2>/dev/null; then
    # Mostra só arquivo e linha, nunca o valor.
    while IFS=: read -r linha _; do
      echo "SEGREDO em potencial: $arquivo:$linha"
    done </tmp/achado.$$
    achou=1
  fi
done < <(git ls-files)
rm -f /tmp/achado.$$

if [ "$achou" -ne 0 ]; then
  echo "Varredura de segredos falhou: remova o valor, rotacione a chave e use variável de ambiente."
  exit 1
fi
echo "Varredura de segredos: nada encontrado."
