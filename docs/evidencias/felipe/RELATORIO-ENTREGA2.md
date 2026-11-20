# Relatório da Entrega 2 — `RecebimentoService`

- **Responsável:** Felipe Ratto
- **Classe:** `RecebimentoService` (complexidade ciclomática 24 pelo JaCoCo)
- **Execução:** 02/11/2026, com JDK 17, Maven 3.9.9 e aplicação em Docker
- **Casos de teste:** [felipe-recebimento.md](../../casos-de-teste/felipe-recebimento.md)

## Resultados

Os três momentos seguem o padrão de nomes de `docs/evidencias/PADRAO-EVIDENCIAS.md`:

- **inicial:** testes da Entrega 1 sobre o código original
- **antes sonar:** testes novos sobre o código original
- **depois sonar:** testes novos sobre o código corrigido

| Critério | Meta | Inicial | Antes sonar | Depois sonar |
|---|---|---|---|---|
| Testes unitários | | 31 | 48 (1 falha: bug do `Long`) | 48 |
| Cobertura de arestas (JaCoCo) | ≥ 80% | 22/34 (64,7%) | 33/34 (97,1%) | 34/34 (100%) |
| Cobertura de linhas (JaCoCo) | | 57/91 (62,6%) | 91/91 (100%) | 94/94 (100%) |
| Escore de mutação (PIT) | ≥ 80% | 24/39 (62%) | 36/39 (92%) | 40/41 (98%) |
| Problemas no SonarQube | resolver | | 15 (6 bugs, 9 code smells) | 0 |

As contagens mudam entre "antes sonar" e "depois sonar" porque a correção alterou o código da
classe. Cada momento tem o print (`.png`, com detalhe em `(pt2)`) e o dado bruto (`.csv` do
JaCoCo, `.xml` do PIT, `.json` do Sonar) nos arquivos `E2-COB`, `E2-MUT` e `E2-SONAR`.

## Mutante sobrevivente

O único sobrevivente fica em `vlsobra = vlsobra < 0 ? 0 : vlsobra`, onde o PIT troca `<` por `<=`.
É um mutante equivalente: quando `vlsobra` é zero, os dois ramos produzem zero, então nenhum
teste consegue distinguir o mutante do original.

O sobrevivente da linha 108 no "antes" era o M01 da mutação manual (issue #9). Ele deixou de
existir porque o nulo agora é verificado antes do zero.

## Correções do SonarQube

| Regra | Qtd. | Problema | Correção |
|---|---|---|---|
| `java:S6813` | 9 | Injeção por atributo | Injeção por construtor |
| `java:S4973` | 1 | `Long` comparado com `!=` | `equals` |
| `java:S3655` | 5 | `Optional.get()` sem verificação | `orElseThrow` com mensagem |

Também foram removidos o `println` e as chamadas soltas a `e.getMessage()`. As exceções agora
preservam a causa original.

**Mudanças de comportamento:**

- Cliente com código acima de 127 consegue abrir recebimento.
- Título nulo mostra a mensagem de título obrigatório, em vez de `NullPointerException`.
- Recebimento, título ou caixa ausentes mostram "Recebimento não encontrado", "Título não
  encontrado" e "Nenhum caixa aberto", em vez de "No value present".
- Com título nulo, zero ou recebimento fechado, a validação acontece antes da busca do título.

## Testes de integração

Coleção `postman/recebimento.postman_collection.json`, executada com newman: 24 requests e 42
asserções. Saídas em `E2-INT - Recebimento antes sonar.txt` e `E2-INT - Recebimento depois sonar.txt`.

| Execução | Falhas | Causa |
|---|---|---|
| Antes da correção | 3 | 2 do cliente com código acima de 127; 1 do request 23 |
| Depois da correção | 1 | Request 23, que falha de propósito por causa da issue #10 |

## Testes de sistema

`RecebimentoSeleniumTest`, executado em modo headless no Brave com ChromeDriver 154. Prints
da execução em `E2-SEL - Recebimento antes sonar.png` e `E2-SEL - Recebimento depois sonar.png`,
com a saída completa do Maven nos `.txt` de mesmo nome. Em cada execução a aplicação rodou em
Docker com o código do momento correspondente; os testes Selenium são os mesmos nas duas.

| Execução | Resultado |
|---|---|
| Antes da correção | 10 testes; 1 erro em `deveAbrirRecebimentoDeClienteComCodigoAcimaDe127`: alerta "A parcela 9011 não pertence ao cliente selecionado" (issue #21) |
| Depois da correção | 10 testes, todos passando |

## Pendências conhecidas

- As issues #10 e #11 continuam abertas, por decisão do responsável. O teste
  `deveGravarAcrescimoEDescontoMasNaoRepassarAsParcelasPossivelBug` documenta o comportamento atual.
- `RecebimentoController.remove` devolve `http://localhost:8080/receber` fixo, então o
  cancelamento redireciona errado em outras portas. Ainda sem issue.
- Sem caixa aberto, a falta de caixa só é detectada depois de `parcelas.receber`. A transação
  deve desfazer a alteração, mas isso não foi verificado por teste de integração.
