# Casos de teste manual — Sangria / Suprimento de caixa

- **Responsável:** Bruno Taconi
- **Funcionalidade:** Caixa — Sangria / Suprimento
- **Classe relacionada:** `CaixaLancamentoService` (método `lancamento`)
- **Ambiente:** aplicação via `docker compose up -d`, navegador em `http://localhost:8080`,
  login `gerente` / senha `123`
- **Evidências:** `docs/evidencias/bruno/` (inclui `db-antes.txt` e `db-depois.txt` com o
  estado do banco antes e depois da execução)
- **Execução:** 20/09/2026, por Bruno Taconi — os 4 casos executados, todos **PASSOU**

## Observações sobre o comportamento real do sistema

Levantado no código antes de escrever os casos, para não inventar resultado esperado:

- O menu lateral se chama **"Caixa / Cofre"**.
- Os botões **Suprimento**, **Retirada**, **Tranferência** e **Fechar** só existem
  dentro da tela **Gerenciar Caixa**, não na lista.
- O botão da sangria aparece com o rótulo **"Retirada"**, e o modal tem o
  título "Retirada de Caixa" (`gerenciar.html`, `modalSangria.html`).
- Esses botões dependem das permissões `CAIXA_SUPRIMENTO` / `CAIXA_SANGRIA`, que o
  usuário `gerente` tem por pertencer ao grupo ADMINISTRADOR.
- O retorno do servidor é exibido em um **alert do navegador**, com o texto que o
  service devolve (`caixa.js`).
- O saldo do caixa **não** é atualizado pelo Java: quem atualiza é o trigger de
  banco `tr_atualizaValoresCaixa_AFTER_INSERT`, disparado no insert do lançamento.
- Na saída, o valor é gravado **negativo**, então o extrato mostra o valor com
  sinal negativo e a linha fica destacada em vermelho.

---

## CT-BRU-MAN-001 — Sangria com caixa aberto e saldo suficiente

| Campo | Conteúdo |
|---|---|
| **Funcionalidade** | Caixa — Sangria |
| **Pré-condição** | Aplicação no ar; usuário `gerente` logado; existir um caixa do tipo CAIXA **aberto**, com saldo total conhecido (ex.: R$ 100,00) |

**Passos / ação / entrada**

1. Acessar o menu lateral **Caixa / Cofre**, localizar o caixa aberto na lista e
   clicar nele para abrir a tela **Gerenciar Caixa** (`/caixa/gerenciar/{codigo}`).
2. Anotar o valor do campo **Saldo Total**.
3. Clicar no botão **Retirada**.
4. Preencher **Valor da Sangria** com `50,00`.
5. Preencher **Observação** com `sangria teste`.
6. Clicar em **Confirmar**.

**Resultado esperado**

- Alert com a mensagem `Lançamento realizado com sucesso`.
- O modal fecha.
- Na tabela **Lançamentos** aparece uma linha nova com: Descrição `sangria teste`,
  Valor `R$ -50,00`, E/S `SAIDA`, data/hora do momento; linha destacada em vermelho.
- **Saldo Total** passa a ser o valor anterior menos R$ 50,00.

| Campo | Conteúdo |
|---|---|
| **Resultado obtido** | Alert exibido: `Lançamento realizado com sucesso`. Linha 2 criada na tabela Lançamentos com Descrição `sangria teste`, Valor `R$ -50,00`, E/S `SAIDA`, em vermelho. Saldo Total passou de R$ 100,00 para R$ 50,00. |
| **Status** | **PASSOU** |
| **Evidência** | `CT-BRU-MAN-001 - sangria.png` |

---

## CT-BRU-MAN-002 — Suprimento com caixa aberto

| Campo | Conteúdo |
|---|---|
| **Funcionalidade** | Caixa — Suprimento |
| **Pré-condição** | Mesma do CT-BRU-MAN-001, com o caixa ainda aberto |

**Passos / ação / entrada**

1. Na tela **Gerenciar Caixa**, anotar o valor do campo **Saldo Total**.
2. Clicar no botão **Suprimento**.
3. Preencher **Valor do Suprimento** com `30,00`.
4. Preencher **Observação** com `suprimento teste`.
5. Clicar em **Confirmar**.

**Resultado esperado**

- Alert com a mensagem `Lançamento realizado com sucesso`.
- O modal fecha.
- Na tabela **Lançamentos** aparece uma linha nova com: Descrição `suprimento teste`,
  Valor `R$ 30,00`, E/S `ENTRADA`, data/hora do momento; linha destacada em verde.
- **Saldo Total** passa a ser o valor anterior mais R$ 30,00.

| Campo | Conteúdo |
|---|---|
| **Resultado obtido** | Alert exibido: `Lançamento realizado com sucesso`. Linha 3 criada com Descrição `suprimento teste`, Valor `R$ 30,00`, E/S `ENTRADA`, em verde. Saldo Total passou de R$ 50,00 para R$ 80,00. |
| **Status** | **PASSOU** |
| **Evidência** | `CT-BRU-MAN-002 - suprimento.png` |

---

## CT-BRU-MAN-003 — Sangria de valor maior que o saldo disponível

| Campo | Conteúdo |
|---|---|
| **Funcionalidade** | Caixa — Sangria (validação de saldo) |
| **Pré-condição** | Caixa aberto com saldo total conhecido (ex.: R$ 80,00 após os casos anteriores) |

**Passos / ação / entrada**

1. Na tela **Gerenciar Caixa**, anotar o valor do campo **Saldo Total**.
2. Clicar no botão **Retirada**.
3. Preencher **Valor da Sangria** com um valor **maior que o saldo** (ex.: `500,00`).
4. Preencher **Observação** com `sangria acima do saldo`.
5. Clicar em **Confirmar**.

**Resultado esperado**

- Alert com a mensagem `Saldo insuficiente para realizar esta operação`.
- **Nenhuma** linha nova na tabela **Lançamentos**.
- **Saldo Total** permanece igual ao anotado no passo 1.

| Campo | Conteúdo |
|---|---|
| **Resultado obtido** | Alert exibido: `Saldo insuficiente para realizar esta operação`. Nenhuma linha nova na tabela. Saldo Total permaneceu R$ 80,00. Confirmado no banco: a tabela `caixa_lancamento` continua com 3 registros. |
| **Status** | **PASSOU** |
| **Evidência** | `CT-BRU-MAN-003 - saldo insuficiente.png` |

---

## CT-BRU-MAN-004 — Conferir os lançamentos no extrato do caixa

| Campo | Conteúdo |
|---|---|
| **Funcionalidade** | Caixa — Extrato de lançamentos |
| **Pré-condição** | CT-BRU-MAN-001, 002 e 003 executados, na ordem |

**Passos / ação / entrada**

1. Na tela **Gerenciar Caixa**, recarregar a página (F5).
2. Conferir a tabela **Lançamentos**.
3. Conferir os campos de saldo do caixa.

**Resultado esperado**

- A tabela lista a sangria do CT-001 (`R$ -50,00`, SAIDA) e o suprimento do
  CT-002 (`R$ 30,00`, ENTRADA), com as observações digitadas.
- A tentativa do CT-003 **não** aparece na tabela.
- **Saldo Total** = saldo inicial − 50,00 + 30,00.

| Campo | Conteúdo |
|---|---|
| **Resultado obtido** | Tabela lista os 3 lançamentos (abertura R$ 100,00 / sangria R$ -50,00 / suprimento R$ 30,00). A tentativa de R$ 500,00 não aparece. Totais exibidos: Entrada R$ 130,00, Saída R$ 50,00, Saldo Total R$ 80,00. |
| **Status** | **PASSOU** |
| **Evidência** | `CT-BRU-MAN-004 - extrato.png` |
