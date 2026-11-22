# Casos de teste funcional — Itens da nota fiscal

- **Responsável:** Daniel
- **Classe:** `NotaFiscalItemService` (`insere`, `remove`, `buscaItensNota`)
- **Técnicas:** particionamento em classes de equivalência e análise de valor-limite
- **Testes unitários:** `NotaFiscalItemServiceTest` (JUnit 4 e Mockito). As dependências são mocks e as entidades são objetos reais.

## Classes de equivalência de `insere(prod, codnota, qtd, tipo)`

| Entrada | Classes válidas | Classes inválidas |
|---|---|---|
| Produto | existente | inexistente |
| Tributação do produto | cadastrada | ausente |
| NCM | preenchido | vazio; nulo |
| Substituição tributária | NAO; SIM com CEST preenchido | SIM com CEST vazio; SIM com CEST nulo |
| Unidade | preenchida | vazia; nula |
| Regras da tributação | ao menos uma do tipo da nota | nenhuma do tipo da nota |
| Regra para a UF do destinatário | existe regra da UF com o tipo da nota | não existe |
| Item na nota | produto novo; produto já lançado (soma a quantidade) | — |

## Valores-limite

| Variável | Valores | Casos |
|---|---|---|
| Nº de regras do tipo da nota | 0 (erro) e 1 (segue) | UT-NFI-06, 07, 08 e 19 |
| Texto (NCM, unidade, CEST) | vazio (erro) e preenchido (segue) | UT-NFI-03, 04, 05, 10 e 11 |
| Texto (NCM, unidade, CEST) | nulo (esperado: mensagem de negócio; hoje falha) | UT-NFI-12, 13 e 14 |

## Casos de teste

| ID | Cenário | Origem |
|---|---|---|
| UT-NFI-01 | Produto inexistente | Funcional: partição |
| UT-NFI-02 | Produto sem tributação | Funcional: partição |
| UT-NFI-03 | NCM vazio | Funcional: valor-limite |
| UT-NFI-04 | Substituição tributária com CEST vazio | Funcional: valor-limite |
| UT-NFI-05 | Unidade vazia | Funcional: valor-limite |
| UT-NFI-06 | Só regra de entrada em nota de saída | Funcional: partição e valor-limite |
| UT-NFI-07 | Só regra de saída em nota de entrada | Funcional: partição e valor-limite |
| UT-NFI-08 | Tributação sem nenhuma regra | Funcional: valor-limite |
| UT-NFI-09 | Sem tributação e sem NCM (ordem das validações) | Funcional: partição |
| UT-NFI-10 | Produto sem substituição tributária ignora CEST | Funcional: partição |
| UT-NFI-11 | Substituição tributária com CEST preenchido | Funcional: partição |
| UT-NFI-12 | NCM nulo | Funcional: valor-limite (falha: defeito) |
| UT-NFI-13 | Unidade nula | Funcional: valor-limite (falha: defeito) |
| UT-NFI-14 | CEST nulo em substituição tributária | Funcional: valor-limite (falha: defeito) |
| UT-NFI-15 | Nenhuma regra para a UF do destinatário | Funcional: partição |
| UT-NFI-16 | Regra da UF com tipo diferente do da nota | Funcional: partição |
| UT-NFI-17 | Várias regras: usa a que combina UF e tipo | Funcional: partição |
| UT-NFI-18 | Nota de entrada com regra de entrada | Funcional: partição |
| UT-NFI-19 | Item novo com valores calculados | Funcional: partição |
| UT-NFI-20 | Produto já na nota: soma a quantidade | Funcional: partição |
| UT-NFI-21 | Outro produto na nota: não soma | Funcional: partição |
| UT-NFI-22 | Totais da nota atualizados | Funcional: partição |
| UT-NFI-23 | Falha ao salvar o item | Funcional: partição (falha da dependência) |
| UT-NFI-24 | Remove item e atualiza totais | Estrutural: lacuna apontada pelo JaCoCo |
| UT-NFI-25 | Falha ao remover o item | Estrutural: lacuna apontada pelo JaCoCo |
| UT-NFI-26 | Busca itens de uma nota | Estrutural: lacuna apontada pelo JaCoCo |
| UT-NFI-27 | Busca itens de nota sem itens | Estrutural: lacuna apontada pelo JaCoCo |

Os casos UT-NFI-01 a 23 vieram da técnica funcional e cobrem todas as arestas de `insere` e
`verificaRegraDeTributacao`. Os casos 24 a 27 foram acrescentados depois do relatório do JaCoCo,
que mostrou `remove` e `buscaItensNota` sem cobertura.