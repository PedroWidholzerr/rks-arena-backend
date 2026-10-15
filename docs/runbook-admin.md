# Runbook do administrador

Só existem administradores (de 1 a 3 ativos). Não há cadastro aberto, nem contas de time ou jogador.

## Primeiro administrador

Na primeira subida, sem nenhum administrador ativo, a aplicação cria um a partir das variáveis de ambiente do Render:

- `ADMIN_EMAIL`: e-mail do administrador.
- `ADMIN_INITIAL_PASSWORD`: senha inicial, com pelo menos 10 caracteres.

A senha é guardada com BCrypt e a **troca é obrigatória no primeiro login**: até trocar, o token só abre `POST /api/auth/password`.

Com algum administrador ativo, essas duas variáveis são **ignoradas**; a aplicação nunca sobrescreve um administrador existente. Depois do primeiro login, remova `ADMIN_INITIAL_PASSWORD` do Render.

## Esqueci a senha do único administrador

1. No Render, defina `ADMIN_RESET_ON_BOOT=true` e `ADMIN_INITIAL_PASSWORD` com uma senha nova (10 caracteres ou mais).
2. Reinicie o serviço (Manual Deploy). Na subida, a senha do **único** administrador ativo é redefinida e a troca volta a ser obrigatória.
3. Entre com a senha nova e troque-a.
4. **Remova `ADMIN_RESET_ON_BOOT` e `ADMIN_INITIAL_PASSWORD`** do Render, para a redefinição não se repetir a cada subida.

A redefinição só acontece com exatamente 1 administrador ativo. Com 2 ou mais, outro administrador redefine por `POST /api/admin/users`, criando uma conta nova, e desativa a antiga.

## Outros administradores

- `POST /api/admin/users` cria um administrador com senha temporária de 16 caracteres, mostrada **uma única vez** na resposta, e troca obrigatória no primeiro login.
- O teto é de 3 administradores **ativos**. Os desativados ficam para auditoria e nunca são excluídos.
- `PATCH /api/admin/users/{id}/active` ativa ou desativa. O último administrador ativo nunca é desativado.
- Criar e (des)ativar rodam sob trava de transação do PostgreSQL, então dois pedidos ao mesmo tempo não furam o teto.

## Sessão

O JWT dura 12 horas. Administrador desativado perde o acesso na hora, mesmo com token válido.
