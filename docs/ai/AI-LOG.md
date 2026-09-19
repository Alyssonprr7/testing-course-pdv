# AI-LOG

Registro dos usos relevantes de IA no trabalho. Cada integrante adiciona as suas
próprias entradas no final do arquivo, seguindo o mesmo formato da entrada 1.

---

## 1. Escolha do sistema-alvo e divisão da equipe

- **Data:** 15/09/2026
- **Responsável:** grupo (registrado por Alysson)
- **Ferramenta:** Claude (Claude Code)

**Objetivo**

Duas etapas: (a) identificar, entre os projetos disponíveis na organização
`repo-software-testing-courses`, qual tinha classes com a complexidade ciclomática
exigida pela disciplina; (b) a partir do projeto escolhido, montar a divisão de
classes, funcionalidades e papéis entre os cinco integrantes.

**O que foi pedido à IA**

1. Comparar os projetos candidatos e sugerir quais tinham métodos de regra de
   negócio com ramificação suficiente (vários `if`/`else`, validações encadeadas)
   para render casos de teste unitário não triviais.
2. Sobre o projeto escolhido (`pdv`), sugerir uma divisão equilibrada: uma classe
   de serviço + uma funcionalidade de teste manual por integrante, mais os papéis
   transversais (repositório, TestLink, issues, escopo, evidências/AI-LOG).

**Sugestões da IA**

A IA indicou o `pdv` (ERP/PDV web em Java, Spring Boot, Thymeleaf, MySQL) como o
candidato com mais classes de serviço densas em regra de negócio, e propôs a
seguinte divisão:

| Integrante | Classe (teste unitário) | Funcionalidade (teste manual) | Papel |
|---|---|---|---|
| Alysson | `CaixaService` | Abrir e fechar caixa | Líder de Teste / Repositório |
| Jose | `VendaService` | Fechar venda no PDV | TestLink |
| Felipe | `RecebimentoService` | Receber parcela / conta a receber | Bug tracking (Issues) |
| Daniel | `NotaFiscalItemService` | Adicionar item à NF-e | Documento de Escopo |
| Bruno | `CaixaLancamentoService` | Sangria / suprimento de caixa | Padrões & Evidências / AI-LOG |

**Validação do grupo**

As sugestões foram tratadas como ponto de partida, não como decisão pronta. O grupo
abriu cada uma das cinco classes no código do `pdv` e conferiu que os métodos
indicados (`cadastro`/`fechaCaixa`, `fechaVenda`, `receber`,
`verificaRegraDeTributacao`/`insere`, `lancamento`) existiam e tinham de fato a
ramificação alegada — ou seja, que rendiam casos de teste suficientes e faziam
sentido como escopo individual. Também foi conferido o equilíbrio de carga entre os
cinco integrantes (uma classe + uma funcionalidade manual para cada, com os papéis
transversais distribuídos). Só depois dessa conferência a divisão foi fechada. O
`NotaFiscalItemService` foi mantido com uma classe reserva acordada
(`ProdutoController`), por ser o mais difícil de mockar.

---

## 2. Testes unitários de `CaixaService.cadastro`

- **Data:** 19/09/2026
- **Responsável:** Alysson
- **Ferramenta:** Claude (Claude Code)

**Objetivo**

Criar a suíte de testes unitários do método `cadastro` da classe `CaixaService`
(JUnit 4 + Mockito), seguindo um fluxo em cinco passos: scaffold, testes
sugeridos por mim, validação, cobertura de lacunas e nova validação.

### Passo 1 — Criar o scaffold

**Prompt utilizado (melhorado para clareza)**

> Crie o scaffold da classe de teste `CaixaServiceTest` para `CaixaService`
> (`src/main/java/net/originmobi/pdv/service/CaixaService.java`). Inicialize tudo
> o que os testes unitários seguintes vão precisar: mocks das dependências,
> injeção no service, contexto de segurança e dados de apoio. Não escreva os
> casos de teste ainda.

**Resultado**

`src/test/java/net/originmobi/pdv/service/CaixaServiceTest.java` com:
`MockitoJUnitRunner`, `@Mock` para `CaixaRepository`, `UsuarioService` e
`CaixaLancamentoService`, `@InjectMocks` para o service, autenticação simulada no
`SecurityContextHolder`, reset por reflexão do singleton `Aplicacao` (que guarda o
usuário em campo estático e vazaria entre testes) e métodos auxiliares para criar
`Usuario` e `Caixa`.

**Validação**

Conferi que a injeção funciona (campos `@Autowired` sem construtor) e que a versão
do Spring Boot (2.0.2) implica JUnit 4, e não JUnit 5. Não foi possível rodar o
Maven no ambiente (sem `mvn`, `mvnw` quebrado), então a execução ficou para a IDE.

### Passo 2 — Sugerir os testes

**Prompt utilizado (melhorado para clareza)**

> Crie os testes do método `cadastro` cobrindo exatamente estes cenários:
> 1. Caixa do tipo `CAIXA` com `caixas.caixaAberto()` retornando um caixa aberto:
>    deve lançar exceção com a mensagem "Existe caixa de dias anteriores em
>    aberto, favor verifique".
> 2. Valor de abertura negativo: exceção "Valor informado é inválido".
> 3. `caixas.save(caixa)` lançando exceção qualquer via Mockito: o assert deve
>    validar a mensagem "Erro no processo de abertura, chame o suporte técnico".
> 4. Valor de abertura maior que zero e `lancamentos.lancamento(...)` lançando
>    exceção: mensagem "Erro no processo, chame o suporte".
> 5. Cadastro válido com valor positivo: deve retornar o código do caixa.
> 6. Valor de abertura `null`: cadastra com sucesso e zera valor de abertura e
>    valor total.
> 7. Três testes com valor zero e descrição vazia, um por tipo: descrição final
>    "Caixa diário", "Cofre" e "Banco".
> 8. Os mesmos três cenários com descrição informada: deve ser mantida.
> 9. `usuarios.buscaUsuario(...)` lançando exceção com mensagem estranha
>    (simulando erro de banco): a exceção deve chegar intacta ao chamador.
> 10. Tipo `BANCO` com agência e conta contendo "-": na saída não pode haver
>     hífen.

**Resultado**

14 testes (os cenários 7 e 8 geram três testes cada).

### Passo 3 — Validar as implementações

Revisei cada teste contra o código de `cadastro`. Pontos conferidos e ajustes:

- O `save` do repositório é mock e não gera id, então o cenário 5 só retorna
  número se o `codigo` for definido antes da chamada.
- Como `lancamento` retorna `String`, o stub de exceção usa
  `doThrow(...).when(...)`.
- Para `BANCO`, agência e conta precisam ser válidas nos cenários 7 e 8; caso
  contrário o `replaceAll` lança `NullPointerException` e o teste falha por outro
  motivo.
- O cenário 9 passa porque `buscaUsuario` está fora dos `try/catch` do service.
- `assertThrows` não existe no JUnit 4.12, então usei `ExpectedException`.

### Passo 4 — Pedir para cobrir os cenários que faltaram

**Prompt utilizado (melhorado para clareza)**

> Assuma o papel de um testador experiente. Analise os testes já escritos para
> `cadastro`, identifique cenários ainda não cobertos (regra de negócio, valores
> limite, efeitos colaterais que não podem ocorrer, entradas inválidas) e crie os
> testes necessários.

**Resultado**

18 testes adicionais, agrupados em:

- **Regra de negócio:** a verificação de caixa aberto vale só para `CAIXA`
  (`COFRE` e `BANCO` passam); valor 0 não gera lançamento e `0.01` gera; o
  lançamento de abertura é conferido com `ArgumentCaptor` (observação por tipo,
  valor, `SALDOINICIAL`, `ENTRADA`, usuário); `save` chamado uma vez, com usuário
  e data preenchidos.
- **Efeitos colaterais:** sem `save` nem lançamento com caixa aberto; sem `save`
  com valor negativo (`-0.01`); sem lançamento quando o `save` falha.
- **Agência e conta:** remoção de qualquer caractere não numérico; campos
  intactos quando o tipo não é `BANCO`.
- **Entradas nulas:** tipo, descrição, agência e conta nulos.

### Passo 5 — Validar novamente os resultados

Revisão dos 18 testes novos:

- Os quatro testes de `NullPointerException` **documentam o comportamento atual**,
  que provavelmente é um defeito do `CaixaService` (falta de validação de
  entrada). Se o service for corrigido, esses testes devem ser atualizados, e o
  achado pode virar issue de bug.
- Remoção dos imports duplicados que apareceram na edição.
- Execução: **pendente**. Os testes ainda não foram rodados; confirmar na IDE (ou
  com Maven local) e anexar a evidência de execução e cobertura.

**Limitações da IA / observações**

A IA não conseguiu compilar nem executar os testes neste ambiente, então nenhum
resultado de "passou/falhou" foi verificado por ela. A conferência final depende
de rodar a suíte localmente.
