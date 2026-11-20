# Casos de teste funcional — Recebimento de parcelas

- **Responsável:** Felipe Ratto
- **Funcionalidade:** Receber parcela / conta a receber
- **Classe relacionada:** `RecebimentoService` (`abrirRecebimento`, `receber`, `remover`)
- **Técnicas:** particionamento em classes de equivalência e análise de valor limite
- **Casos manuais de origem:** RC-01 a RC-05, em `docs/evidencias/felipe/manuais/`

Os valores esperados vêm do código e do comportamento observado nos testes manuais. Onde o
comportamento atual é um defeito conhecido, o caso aponta a issue correspondente.

## `abrirRecebimento(codpes, arrayParcelas)`

| Entrada | Classes válidas | Classes inválidas |
|---|---|---|
| Código da parcela | numérico de parcela existente | não numérico |
| Situação da parcela | em aberto | quitada |
| Dono da parcela | o cliente informado, com código de qualquer tamanho | outro cliente |
| Cliente | existente | inexistente |
| Quantidade de parcelas | uma; várias | nenhuma (sem validação no service) |

| ID | Cenário | Resultado esperado | Teste unitário |
|---|---|---|---|
| CT-ABR-01 | Uma parcela em aberto | Salva com o total da parcela e devolve o código | `deveAbrirRecebimentoComUmaParcela` |
| CT-ABR-02 | Duas parcelas (60,50 + 39,50) | Total 100,00 com as duas parcelas | `deveSomarRestanteDeTodasAsParcelasAoAbrirRecebimento` |
| CT-ABR-03 | Lista vazia | Salva com total 0,00, sem validação | `deveAbrirRecebimentoComTotalZeroQuandoListaDeParcelasVazia` |
| CT-ABR-04 | Parcela quitada | "Parcela 5 já esta quitada, verifique." | `deveLancarExcecaoAoAbrirRecebimentoComParcelaQuitada` |
| CT-ABR-05 | Segunda parcela quitada | Interrompe e não salva | `deveInterromperAberturaQuandoSegundaParcelaEstaQuitada` |
| CT-ABR-06 | Parcela de outro cliente | "A parcela 5 não pertence ao cliente selecionado" | `deveLancarExcecaoAoAbrirRecebimentoComParcelaDeOutroCliente` |
| CT-ABR-07 | Cliente com código 200 (acima de 127) | Abre normalmente | `deveAceitarParcelaDoProprioClienteComCodigoAcimaDe127` |
| CT-ABR-08 | Cliente inexistente | "Cliente não encontrado" | `deveLancarExcecaoAoAbrirRecebimentoComClienteInexistente` |
| CT-ABR-09 | Parcela com restante nulo | "Erro ao receber, chame o suporte" | `deveLancarExcecaoAoAbrirRecebimentoQuandoRestanteDaParcelaNulo` |
| CT-ABR-10 | Falha ao salvar | "Erro ao receber, chame o suporte" | `deveLancarExcecaoQuandoFalhaAoSalvarRecebimentoAberto` |
| CT-ABR-11 | Código "abc" | `NumberFormatException` | `deveLancarNumberFormatQuandoCodigoDaParcelaInvalido` |

## `receber(codreceber, vlrecebido, vlacrescimo, vldesconto, codtitulo)`

| Entrada | Classes válidas | Classes inválidas |
|---|---|---|
| Título | existente, em dinheiro ou cartão | nulo; zero; inexistente |
| Recebimento | aberto, com parcelas | fechado; inexistente; sem parcelas |
| Valor recebido | 0 < valor ≤ total | valor ≤ 0; valor > total |

Valores limite do valor recebido, para um total de 100,00:

| Valor | Classe | Resultado esperado |
|---|---|---|
| -0,01 ou menos | inválida | "Valor de recebimento inválido" |
| 0,00 | inválida | "Valor de recebimento inválido" |
| 0,01 | válida | Abate 0,01 da parcela |
| 100,00 | válida | Quita a parcela |
| 100,01 | inválida | "Valor de recebimento é superior aos títulos" |

| ID | Cenário | Resultado esperado | Teste unitário |
|---|---|---|---|
| CT-REC-01 | Título zero | "Selecione um título para realizar o recebimento" | `deveLancarExcecaoQuandoTituloNaoSelecionado` |
| CT-REC-02 | Título nulo | Mesma mensagem do título zero (#9) | `deveExigirTituloQuandoCodigoNulo` |
| CT-REC-03 | Recebimento fechado | "Recebimento já esta fechado" | `deveLancarExcecaoQuandoRecebimentoJaFechado` |
| CT-REC-04 | Valor 100,01 | "Valor de recebimento é superior aos títulos" | `deveLancarExcecaoQuandoValorRecebidoExcedeTotalEmUmCentavo` |
| CT-REC-05 | Sem parcelas | "Recebimento não possue parcelas" | `deveLancarExcecaoQuandoRecebimentoSemParcelas` |
| CT-REC-06 | Valor 0,00 | "Valor de recebimento inválido" | `deveLancarExcecaoQuandoValorRecebidoZero` |
| CT-REC-07 | Valor negativo | "Valor de recebimento inválido" | `deveLancarExcecaoQuandoValorRecebidoNegativo` |
| CT-REC-08 | Valor 0,01 | Abate 0,01 | `deveAceitarValorRecebidoDeUmCentavo` |
| CT-REC-09 | Valor igual ao total | Quita a parcela | `deveQuitarTotalmenteQuandoValorIgualAoTotal` |
| CT-REC-10 | Valor atravessa duas parcelas | 60,00 na primeira e 20,00 na segunda | `devePagarPrimeiraParcelaEParteDaSegundaQuandoValorAtravessaParcelas` |
| CT-REC-11 | Sobra depois da última parcela | Encerra sem erro | `deveEncerrarLacoQuandoSobraValorDepoisDaUltimaParcela` |
| CT-REC-12 | Parcela sem saldo | Repassa 0,00 e segue para a próxima | `deveRepassarZeroPositivoQuandoParcelaNaoTemSaldoRestante` |
| CT-REC-13 | Título em dinheiro | Lança no caixa | `deveLancarNoCaixaQuandoTituloEmDinheiro` |
| CT-REC-14 | Título de débito ou crédito | Lança no cartão | `deveLancarNoCartaoQuandoTituloCartaoDebito`, `deveLancarNoCartaoQuandoTituloCartaoCredito` |

## `remover(codigo)`

| ID | Cenário | Resultado esperado | Teste unitário |
|---|---|---|---|
| CT-REM-01 | Recebimento aberto | "removido com sucesso" | `deveRemoverRecebimentoNaoProcessado` |
| CT-REM-02 | Recebimento processado | Recusa e não exclui | `deveLancarExcecaoAoRemoverRecebimentoProcessado` |
| CT-REM-03 | Falha ao excluir | "Erro ao remover orçamento, chame o suporte" | `deveLancarExcecaoQuandoFalhaAoRemoverRecebimento` |
| CT-REM-04 | Recebimento inexistente | Segue para a exclusão, sem validação | `deveTentarRemoverMesmoQuandoRecebimentoNaoExiste` |

## Testes de sistema e de integração

Os dois usam a massa em `src/test/resources/sql/recebimento-massa.sql`, necessária porque a
venda "A Prazo" não gera parcela (issue #12).

- **Sistema:** `src/test/java/net/originmobi/pdv/selenium/RecebimentoSeleniumTest.java`
- **Integração:** `postman/recebimento.postman_collection.json`

Para rodar a coleção:

```sh
docker compose exec -T pdv-db mysql -uroot -p123456 pdv < src/test/resources/sql/recebimento-massa.sql
npx newman run postman/recebimento.postman_collection.json
```

Precisa haver nenhum caixa do tipo CAIXA aberto. O request 23 falha de propósito, porque
registra a issue #10.

Para rodar o Selenium com a aplicação no ar (`-Dchrome.binary` só é necessário em navegadores
como o Brave):

```sh
mvn test -Dtest=RecebimentoSeleniumTest -Dchrome.binary=/usr/bin/brave -Dwebdriver.chrome.driver=<caminho do chromedriver>
```

As propriedades `-DbaseUrl` e `-Ddb.url` trocam as portas padrão `8080` e `3306`.
