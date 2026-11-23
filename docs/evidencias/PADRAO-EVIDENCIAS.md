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
| `INT` | Execução dos testes de integração (Postman / newman) |
| `UNI` | Execução dos testes unitários, quando o log é a evidência (ex.: falha que revela bug) |
| `NF` | Teste não funcional (ex.: desempenho com JMeter) |

O momento é `antes sonar` ou `depois sonar`, porque as correções do Sonar alteram
o código e mudam os números de cobertura e mutação. Quando houver medição antes de os testes
serem melhorados, ela usa o momento `inicial`. Exemplos:

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

Conferida em 23/11/2026, antes da Entrega 2.

| Pasta | Entrega 1 | Entrega 2 | Segue o padrão? |
|---|---|---|---|
| `bruno/` | `CT-BRU-MAN-00N - <cenário>.png`, `db-antes.txt`, `db-depois.txt` | `E2-COB`, `E2-MUT`, `E2-SEL`, `E2-SONAR`, `E2-NF`, `E2-UNI`, com momento | Sim |
| `felipe/` | `manuais/RC-0N/` com prints numerados por passo, `db-antes.txt`, `db-depois.txt` e `execucao.json`; `unitario/` | `E2-COB`, `E2-MUT`, `E2-SEL`, `E2-SONAR`, `E2-INT`, `E2-UNI`, com momento (inclui `.csv`, `.xml`, `.json` e `.txt`) | Sim. Na Entrega 1, a pasta por caso é mais detalhada que o padrão e foi mantida |
| `daniel/` | `evidencia1.png` a `evidencia4.png` | `E2-COB`, `E2-MUT`, `E2-SEL`, `E2-SONAR`, com momento | Sim na Entrega 2. Na Entrega 1, nomes genéricos |
| `alysson/` | `CT-CAIXA-00N - <cenário>.png` (dois arquivos sem o espaço antes do hífen) | `E2-COB - CaixaService.png` e `E2-MUT - CaixaService.png` (sem momento); `cobertura-mutacao/` (prints do JaCoCo e do PIT); `postman-caixa/NN.png`; `SON-ANTES.png` e `SON-DEPOIS.png` | Parcialmente |
| `jose/` | `Testes Manuais/Fechar Vendas/EV-NN.png` e `Testes Manuais/Incluir Pessoa/ev NN.png` | Uma pasta por ferramenta: `Unitário/`, `Mutação/`, `Selenium/`, `SonarQube/` e `Postamn-VendaService/` (prints `NN.png` e a collection) | Não, usa convenção própria |

Onde encontrar cada tipo de evidência nas pastas que não seguem o padrão:

| Tipo | `alysson/` | `jose/` |
|---|---|---|
| `COB` | `E2-COB - CaixaService.png`, `cobertura-mutacao/JaCoCo - *.png` | `Unitário/Cobertura *.png`, `Unitário/Complexidade *.png` |
| `MUT` | `E2-MUT - CaixaService.png`, `cobertura-mutacao/PIT - *.png` | `Mutação/` |
| `SEL` | sem print na pasta | `Selenium/Resultados.png` |
| `SONAR` | `SON-ANTES.png`, `SON-DEPOIS.png` | `SonarQube/` |
| `INT` | `postman-caixa/NN.png` | `Postamn-VendaService/` |

Os arquivos não foram renomeados de propósito. A conferência foi feita no dia antes da
entrega, e o relatório da Entrega 2 aponta para caminhos dessas pastas (por exemplo, a
collection em `jose/Postamn-VendaService/`). Renomear arquivos de outro integrante sem
combinar antes quebra as referências dos documentos dele.
