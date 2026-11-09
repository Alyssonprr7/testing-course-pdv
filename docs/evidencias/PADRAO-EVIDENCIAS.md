# Padrão de nomenclatura das evidências

Responsável pelo padrão: Bruno Taconi (Padrões & Evidências).

## Regra

Uma pasta por integrante em `docs/evidencias/<nome>/`, e cada arquivo nomeado como:

```
<ID do caso de teste> - <descrição curta do cenário>.<extensão>
```

Exemplos:

```
CT-CAIXA-001 - sucesso.png
CT-BRU-MAN-003 - saldo insuficiente.png
```

Quando um cenário precisa de mais de um print, acrescentar `(pt2)`, `(pt3)`:

```
CT-CAIXA-002 - sucesso (pt2).png
```

O ID do arquivo tem que ser o mesmo ID usado no documento de casos de teste do
integrante, para dar rastreabilidade entre o caso e a evidência.

## Relatórios de ferramentas (Entrega 2)

Prints de relatório não têm ID de caso de teste. No lugar do ID vai um prefixo
com a entrega e o tipo de evidência, seguido da classe analisada e do momento:

```
E2-<TIPO> - <Classe> <momento>.<extensão>
```

| Tipo | Ferramenta |
|---|---|
| `COB` | Cobertura (JaCoCo) |
| `MUT` | Mutação (PIT) |
| `SONAR` | Inspeção de código (SonarQube) |
| `SEL` | Execução dos testes de sistema (Selenium) |

O momento é `antes sonar` ou `depois sonar`, porque as correções do Sonar alteram
o código e mudam os números de cobertura e mutação. Exemplos:

```
E2-COB - CaixaLancamentoService antes sonar.png
E2-MUT - CaixaLancamentoService depois sonar (pt2).png
```

## Evidências de banco (opcional, recomendado)

Quando o cenário altera dados, vale registrar o estado do banco antes e depois:

```
db-antes.txt
db-depois.txt
```

Isso permite conferir o resultado por uma fonte independente do print da tela.

## Situação atual do repositório

O padrão acima foi aplicado às evidências do Bruno. As demais pastas ainda seguem
convenções próprias, e não foram renomeadas de propósito:

- **`alysson/`** — já usa o formato `CT-CAIXA-001 - sucesso.png`, que é a base deste
  padrão. Falta apenas normalizar o espaço antes do hífen em dois arquivos.
- **`felipe/`** — usa uma pasta por caso (`manuais/RC-01/`) com prints numerados por
  passo, mais `db-antes.txt`, `db-depois.txt` e `execucao.json`. É mais detalhado que
  este padrão. Renomear esses arquivos quebraria o `execucao.json`, que se refere aos
  prints pelo número do passo.

A decisão de unificar ou manter as duas formas é do grupo. Renomear arquivos de
outro integrante sem combinar antes quebra as referências dos documentos dele.
