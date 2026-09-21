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

---

## 3. Testes unitários de `CaixaService.fechaCaixa`

- **Data:** 19/09/2026
- **Responsável:** Alysson
- **Ferramenta:** Claude (Claude Code)

**Objetivo**

Criar a suíte de testes unitários do método `fechaCaixa`, reaproveitando o
scaffold da entrada 2 (mocks, contexto de segurança, reset do singleton
`Aplicacao` e helpers), no mesmo fluxo: testes sugeridos, validação, cobertura das
lacunas e nova validação.

### Passo 1 — Sugerir os testes

**Prompt utilizado (melhorado para clareza)**

> Crie os testes do método `fechaCaixa` cobrindo exatamente estes cenários:
> 1. Senha `""`: retorno "Favor, informe a senha".
> 2. Senha `null`: provavelmente lança exceção; o teste deve documentar isso.
> 3. `usuario.getSenha()` retornando `"1234"` e senha enviada `"123"`: retorno
>    "Senha incorreta, favor verifique".
> 4. Caixa com data de fechamento já preenchida: exceção "Caixa já esta fechado".
> 5. `caixas.save(...)` lançando exceção genérica via Mockito: a mensagem
>    validada deve ser "Ocorreu um erro ao fechar o caixa, chame o suporte".
> 6. Caixa com valor total `null`: retorno "Caixa fechado com sucesso" e valor de
>    fechamento 0.0.
> 7. `usuarios.buscaUsuario(...)` lançando exceção: a mensagem deve chegar
>    intacta ao chamador.
> 8. Fechamento correto com senha válida: retorno "Caixa fechado com sucesso".

**Resultado**

8 testes. O cenário 2 chegou com o enunciado cortado; assumi que o comportamento
esperado é `NullPointerException` (`senha.equals("")` sem validação de `null`).

### Passo 2 — Validar as implementações

- No cenário 3, `"1234"` não é um hash BCrypt válido: o `matches` retorna `false`
  (com aviso no log), então o resultado esperado é obtido.
- O cenário 8 confere também o valor de fechamento, a data preenchida e uma única
  chamada a `save`.
- Criado o helper `criaCaixaAberto(valorTotal)` para os cenários com caixa
  recuperado por `findById`.

### Passo 3 — Pedir para cobrir os cenários que faltaram

**Prompt utilizado (melhorado para clareza)**

> Assuma o papel de um testador experiente. Analise os testes já escritos para
> `fechaCaixa`, identifique cenários ainda não cobertos (efeitos colaterais que
> não podem ocorrer, entradas inválidas, valores limite, falhas de dependências)
> e crie os testes necessários.

**Resultado**

14 testes adicionais:

- **Efeitos colaterais:** sem `findById` nem `save` com senha vazia ou incorreta;
  sem `save` com caixa já fechado; `findById` e `save` chamados uma vez, com o
  mesmo objeto.
- **Senha:** senha só com espaço e senha correta com espaço no final são
  incorretas; usuário com senha `null` retorna "Senha incorreta".
- **Falhas de dependência:** exceção em `findById` chega intacta; `buscaUsuario`
  retornando `null` gera `NullPointerException`; caixa inexistente
  (`Optional.empty()`) gera `NoSuchElementException`.
- **Valores:** total 0.0 resulta em fechamento 0.0; total negativo é copiado sem
  validação; data de fechamento fica entre o instante antes e depois da chamada.

### Passo 4 — Validar novamente os resultados

- Os testes de usuário inexistente, caixa inexistente, senha nula e valor total
  negativo **documentam o comportamento atual**, que provavelmente é um defeito do
  `CaixaService`. Se o service for corrigido, esses testes devem ser atualizados.
- Execução: **pendente**. Os testes ainda não foram rodados; confirmar na IDE (ou
  com Maven local) e anexar a evidência de execução e cobertura.

**Limitações da IA / observações**

A IA não conseguiu compilar nem executar os testes neste ambiente, então nenhum
resultado de "passou/falhou" foi verificado por ela.

---

## 4. Elaboração e revisão do plano de teste

- **Data:** 20/09/2026
- **Responsável:** Alysson
- **Ferramenta:** Claude (Claude Code)

**Objetivo**

Usar a IA como apoio na elaboração e na criação do plano de teste (documento
vinculado no `README.md`), com revisões do texto já escrito e sugestões do que
colocar em cada uma das seções.

**O que foi pedido à IA**

1. Sugerir o que cada seção do plano deveria conter, dado o sistema-alvo (`pdv`),
   a divisão da equipe (entrada 1) e as exigências da disciplina.
2. Revisar os rascunhos das seções, apontando lacunas, inconsistências e trechos
   pouco claros.

**Sugestões da IA**

A IA sugeriu o conteúdo esperado de cada seção e comentou os rascunhos, apontando
pontos a completar ou reescrever. O plano foi escrito e mantido pela equipe, e a IA
atuou como apoiadora de estrutura e de revisão.

**Validação do grupo**

As sugestões e revisões foram tratadas como ponto de partida. O grupo avaliou cada
uma, aceitou, adaptou ou descartou conforme o que de fato foi feito no projeto, e
conferiu que o plano final é coerente com o escopo, a divisão de classes e as
funcionalidades reais do `pdv`.

---

## 5. Briefing e geração dos slides da apresentação

- **Data:** 20/09/2026
- **Responsável:** Alysson
- **Ferramentas:** Claude (Claude Code) para o briefing; Gemini para os slides

**Objetivo**

Montar os slides da apresentação do trabalho, com foco no processo (como o grupo
criou, dividiu e executou o trabalho, e como usou IA), e não nos detalhes técnicos
do código. Uso em duas etapas: o Claude escreve o documento de instruções e o
Gemini gera a apresentação a partir dele.

### Passo 1 — Criar o briefing (Claude)

**Prompt utilizado (melhorado para clareza)**

> Crie um documento para enviar ao Gemini gerar os slides do trabalho. O foco é o
> processo de criação e execução, como fizemos e como dividimos o trabalho. Use como
> fonte o `docs/ai/AI-LOG.md` e a divisão de trabalho da equipe.

**Resultado**

Um arquivo Markdown com: instrução ao Gemini (10 a 12 slides em português, tom
acadêmico, sugestão de visual e notas do apresentador por slide, proibição de
inventar dados), estrutura sugerida de 12 slides e o conteúdo de cada um (contexto,
escolha do `pdv`, tabela de divisão por integrante, papéis transversais, ferramentas
e organização do repositório, metodologia de uso de IA, estudos de caso de
`CaixaService.cadastro` e `fechaCaixa`, plano de teste, limitações da IA e lições
aprendidas).

**Validação**

O briefing foi montado apenas com o que já estava no AI-LOG, no README e na divisão
de classes, sem incluir datas nem resultados de execução. Como o AI-LOG registra que
os testes ainda não foram rodados, o briefing informa isso e não cita "passou/falhou"
nem cobertura.

### Passo 2 — Gerar os slides (Gemini)

**Prompt utilizado**

O conteúdo do briefing do passo 1, enviado ao Gemini como instrução completa.

**Resultado**

Apresentação de slides gerada pelo Gemini a partir do briefing.

**Validação do grupo**

O grupo deve conferir cada slide contra o briefing e contra o repositório (divisão de
classes, papéis, quantidade de testes, ferramentas), corrigir qualquer dado inventado
ou impreciso e ajustar o visual antes da apresentação. Pendente: registrar aqui o
resultado dessa conferência.

**Limitações da IA / observações**

O Gemini trabalha só com o texto do briefing e não tem acesso ao repositório, então
pode preencher lacunas por conta própria. O briefing pede explicitamente que não
invente números, datas ou resultados, mas a conferência humana continua necessária.

---

## 6. Testes unitários de `CaixaLancamentoService.lancamento`

- **Data:** 20/09/2026
- **Responsável:** Bruno
- **Ferramenta:** Claude (Claude Code)

**Objetivo**

Criar a suíte de testes unitários do método `lancamento` da classe
`CaixaLancamentoService` (JUnit 4 + Mockito), seguindo o mesmo fluxo em cinco
passos das entradas 2 e 3: scaffold, cenários planejados, validação, cobertura de
lacunas e validação final.

**Prompt utilizado (melhorado para clareza)**

> Quero criar os testes unitários do método `lancamento` da classe
> `CaixaLancamentoService`. Use o código real do repositório como fonte e não
> invente comportamentos que não estejam implementados. O projeto usa Java 8,
> Spring Boot 2.0.2, JUnit 4 e Mockito. Siga cinco passos: (1) scaffold apenas,
> com os mocks e helpers realmente necessários; (2) testes para os cenários
> planejados — sem caixa aberto, saldo insuficiente, saída convertida para
> negativo, sangria e suprimento com observação vazia, falha do repository — e,
> se o código não se comportar como o cenário sugere, explicar a divergência e
> documentar o comportamento atual em vez de forçar o teste a passar; (3) revisar
> cada teste contra o código-fonte; (4) sugerir casos adicionais sem implementar,
> para eu escolher; (5) revisão final separando comportamento correto de possível
> defeito. Não altere código de produção e não afirme que um teste passou sem
> executar.

O trabalho foi feito um passo por vez, com validação minha entre cada um antes de
seguir para o próximo.

### Passo 1 — Scaffold

`src/test/java/net/originmobi/pdv/service/CaixaLancamentoServiceTest.java` com
`MockitoJUnitRunner` (estrito), `@Mock` para `CaixaLancamentoRepository`,
`@InjectMocks` para o service e helpers para montar `Caixa` aberto, `Caixa`
fechado e `CaixaLancamento`.

`UsuarioService` foi deliberadamente deixado de fora: é `@Autowired` na classe,
mas o método `lancamento` nunca o usa. Validado com `mvn test-compile`.

### Passo 2 — Cenários planejados

8 testes. Três cenários divergiram do esperado e foram documentados como
comportamento atual, sem alterar o código de produção:

- O guard de "Nenhum caixa aberto" é **código morto**: as duas condições do `&&`
  são mutuamente exclusivas. Na prática, uma ENTRADA sem caixa é salva
  normalmente e um caixa já fechado também é aceito.
- Uma SAÍDA sem caixa falha por acidente (`Optional.get()` sobre vazio), gerando
  `RuntimeException` **sem mensagem**, e não "Nenhum caixa aberto".
- "Saldo insuficiente" não lança exceção: retorna uma `String`, que o chamador
  pode ignorar.

### Passo 3 — Validação

Revisão de cada teste contra o código. Duas limitações registradas:

- O teste da saída sem caixa não consegue provar qual exceção ocorreu, porque o
  `catch (Exception e)` descarta a causa original.
- O `ArgumentCaptor` guarda a referência do objeto, não um snapshot — como o
  service muta o próprio `CaixaLancamento`, ele prova que `save` foi chamado, mas
  não isola o estado.

### Passo 4 — Cobertura de lacunas

A IA levantou 16 casos possíveis e não implementou nenhum por conta própria.
Escolhi 8 (os de regra de negócio e observação, mais um de efeito colateral) e
descartei os de `NullPointerException`, que eram repetitivos.

Total: **16 testes**.

### Passo 5 — Validação final

**Confirmado correto:** saldo insuficiente não persiste; saída positiva vira
negativa; observações padrão de sangria e suprimento; observação preenchida é
preservada; falha do repository vira a mensagem de suporte; limite `>` estrito
(valor igual ao saldo passa, um centavo acima é recusado).

**Possíveis defeitos documentados por teste (não corrigidos):**

- Valor negativo em SAÍDA fura a validação de saldo (`-200 > 100` é falso) e
  deixa o caixa negativo.
- ENTRADA não tem validação nenhuma — um suprimento negativo reduz o caixa.
- O guard de caixa aberto é código morto.
- `data_cadastro` é preenchida antes das validações, mutando o objeto mesmo em
  operação recusada.
- `isEmpty()` em vez de `trim().isEmpty()`: observação só com espaços não recebe
  o texto padrão.
- Tipos fora de SANGRIA/SUPRIMENTO fazem `setObservacao("")`, que não muda nada.
- `dataHoraAtual` é campo de instância num `@Service` singleton (estado mutável
  compartilhado).

Esses achados são candidatos a issues de bug.

**Não coberto:** `@Transactional`/rollback e a validação `@Size(250)` da
observação exigiriam teste de integração; a concorrência no `dataHoraAtual` é
risco por leitura de código, sem teste determinístico.

**Execução**

Diferente das entradas 2 e 3, os testes **foram executados**:
`mvn -o test -Dtest=CaixaLancamentoServiceTest` → **16 testes, 0 falhas, 0 erros**.
Os testes que documentam defeitos passam justamente por reproduzirem o defeito.

**Limitações da IA / observações**

A suíte completa (`mvn test`) falha em `PdvApplicationTests`, mas por motivo
alheio ao trabalho: o ambiente roda **JDK 21** e o projeto declara
`<java.version>1.8</java.version>`, e o CGLIB do Spring 5.0.6 não sobe sob o
sistema de módulos do JDK moderno. Confirmado que a falha reproduz com o arquivo
de teste removido do projeto. Nenhum código de produção foi alterado.

---

## 7. Casos de teste manual de Sangria / Suprimento

- **Data:** 20/09/2026
- **Responsável:** Bruno
- **Ferramenta:** Claude (Claude Code)

**Objetivo**

Projetar os casos de teste manual da funcionalidade "Sangria / suprimento de
caixa", com os quatro cenários mínimos previstos na divisão da equipe: sangria
com caixa aberto, suprimento com caixa aberto, sangria maior que o saldo e
conferência do extrato do caixa.

**Prompt utilizado (melhorado para clareza)**

> Analise o sistema real e crie os casos de teste manual da funcionalidade
> Sangria / Suprimento. Para cada cenário, gere um caso contendo ID,
> funcionalidade, pré-condição, passos, ação/entrada, resultado esperado,
> resultado obtido e status. Não invente comportamento esperado: confirme os
> fluxos pela interface e pelo código. Use os IDs CT-BRU-MAN-001 a
> CT-BRU-MAN-004 e salve em local adequado dentro de `docs/`.

**Resultado**

`docs/casos-de-teste/bruno-sangria-suprimento.md` com os quatro casos.

Antes de escrever os resultados esperados, a IA levantou no código quatro pontos
que mudariam o texto dos casos se fossem assumidos por suposição:

- O botão da sangria na interface tem o rótulo **"Retirada"** (não "Sangria"), e
  o modal se chama "Retirada de Caixa".
- O retorno do servidor é mostrado em um **alert** do navegador, com o texto que o
  service devolve (`caixa.js`).
- O saldo do caixa **não** é atualizado pelo código Java: quem atualiza é o
  trigger de banco `tr_atualizaValoresCaixa_AFTER_INSERT`, disparado no insert do
  lançamento (`V1__cria_estrutura_inicial.sql`).
- Na saída o valor é gravado negativo, então o extrato exibe o valor com sinal
  negativo e a linha em vermelho.

**Validação**

Os quatro pontos acima foram conferidos por mim nos arquivos citados antes de
fechar os casos. Os campos **Resultado obtido** e **Status** foram deixados
explicitamente como "a preencher" / "não executado", porque nenhum caso manual
havia sido executado no momento em que o documento foi criado.

**Limitações da IA / observações**

A IA não tem navegador nem como capturar tela, então não executa os casos manuais
nem produz as evidências. A execução e os prints em `docs/evidencias/bruno/` são
feitos por mim.

---

## 8. Execução dos testes manuais de Sangria / Suprimento

- **Data:** 20/09/2026
- **Responsável:** Bruno
- **Ferramenta:** Claude (Claude Code)

**Objetivo**

Subir o ambiente, executar os quatro casos manuais de Sangria / Suprimento e
registrar resultado obtido, status e evidências.

**Prompt utilizado (melhorado para clareza)**

> Verifique a configuração do projeto e suba o sistema da forma prevista (Docker).
> Confirme aplicação, banco, login e acesso ao fluxo de sangria/suprimento. Depois
> me oriente na execução dos quatro casos e atualize os documentos com resultado
> obtido, status e referência à evidência. Não declare um teste como "passou" sem
> ele ter sido realmente executado.

**Resultado**

Ambiente subido com `docker compose up -d` e verificado: banco `pdv-db` healthy com
as 2 migrations do Flyway aplicadas, aplicação no ar na porta 8080, login
`gerente` / `123` autenticando e tela de caixa acessível.

A IA preparou a pré-condição criando um caixa do tipo CAIXA com saldo de
R$ 100,00 e capturou o estado do banco antes da execução
(`docs/evidencias/bruno/db-antes.txt`).

A execução na interface foi feita por mim. Resultado dos quatro casos:

| Caso | Cenário | Status |
|---|---|---|
| CT-BRU-MAN-001 | Sangria de R$ 50,00 com saldo suficiente | PASSOU |
| CT-BRU-MAN-002 | Suprimento de R$ 30,00 | PASSOU |
| CT-BRU-MAN-003 | Sangria de R$ 500,00 com saldo de R$ 80,00 | PASSOU |
| CT-BRU-MAN-004 | Conferência do extrato e dos saldos | PASSOU |

Evidências em `docs/evidencias/bruno/`, mais o estado do banco depois da execução
(`db-depois.txt`).

**Ajustes feitos durante a etapa**

O roteiro inicial descrevia o caminho de navegação de forma incompleta: dizia
apenas "menu Caixa", quando o menu se chama **"Caixa / Cofre"** e os botões
Suprimento / Retirada só existem dentro da tela **Gerenciar Caixa**, não na lista.
O documento de casos foi corrigido.

Durante a investigação, a IA também afirmou incorretamente que o usuário `gerente`
estava sem permissões, por ter consultado a tabela `permissao` em vez de
`permissoes`. A consulta correta mostra 51 permissões vinculadas ao grupo
ADMINISTRADOR, incluindo `CAIXA_SANGRIA` e `CAIXA_SUPRIMENTO`. O erro foi
corrigido antes de qualquer conclusão entrar nos documentos.

**Validação**

Os quatro resultados foram conferidos em duas fontes independentes: os prints da
interface e o estado do banco. O banco confirma o comportamento esperado —
`caixa_lancamento` ficou com 3 registros (a tentativa de R$ 500,00 não foi
persistida) e o caixa ficou com `valor_total` 80, `valor_entrada` 130 e
`valor_saida` 50.

**Limitações da IA / observações**

A IA não executou nenhum caso pela interface nem capturou prints — não tem
navegador. A execução e as evidências são minhas; a IA preparou o ambiente,
conferiu o banco e preencheu os documentos a partir do que foi observado.

---

## 9. Auditoria final da parte de Sangria / Suprimento

- **Data:** 20/09/2026
- **Responsável:** Bruno
- **Ferramenta:** Claude (Claude Code)

**Objetivo**

Revisar tudo o que é da minha responsabilidade antes da entrega e produzir um
checklist do que está pronto e do que ficou pendente.

**Prompt utilizado (melhorado para clareza)**

> Faça uma auditoria apenas da minha responsabilidade: classe de teste, testes
> unitários e sua execução, casos manuais, resultados, evidências, issues,
> contribuição no Plano de Teste e no Escopo, entradas no AI-LOG e referências no
> README. Verifique se tudo está na branch de entrega e gere um checklist com
> OK / PENDENTE / NÃO SE APLICA.

**Resultado**

A suíte foi reexecutada durante a auditoria (`mvn -o test -Dtest=CaixaLancamentoServiceTest`):
16 testes, 0 falhas, BUILD SUCCESS. Os 4 casos manuais estão com resultado obtido
e status preenchidos, sem pendências.

Duas lacunas foram encontradas e corrigidas:

- O arquivo `db-antes.txt` havia sido gerado mas não chegou a ser versionado, e
  não estava mais no disco. Foi regravado a partir da saída capturada no momento
  original (20/09/2026 20:01:50), com essa observação registrada no próprio
  arquivo.
- O `README.md` não citava a pasta `docs/casos-de-teste/`. A referência foi
  adicionada.

**Validação**

Conferido que a branch de entrega é a `master` (o repositório não tem `main`,
apesar de o Plano de Teste citar esse nome) e que tudo está sincronizado com o
remoto.

Sobre o Plano de Teste: a parte da minha responsabilidade já estava contemplada no
documento (escopo 1.1.1, papéis 1.3 e entregáveis), então não houve texto novo a
acrescentar.

Sobre o Documento de Escopo (escrito pelo Daniel): recebido depois da auditoria e
conferido contra o código. A minha funcionalidade está descrita corretamente — a
tabela da Seção 3 traz "Caixa / Sangria / suprimento / CaixaLancamentoService /
lancamento"; a Seção 3.1 descreve os botões "Suprimento" e "Retirada" delegando ao
método com estilo ENTRADA ou SAIDA e as três regras (verificação de saldo, valor
gravado como negativo e observação padrão quando vazia); e a Observação 3 registra
que os totais do caixa são atualizados por trigger, efeito só observável nos testes
manuais. Nenhuma correção foi necessária na minha parte.

**Limitações da IA / observações**

Dois pontos do Plano de Teste continuam divergentes do repositório e não foram
alterados por serem de seções compartilhadas: o documento cita JUnit 5, mas o
projeto usa JUnit 4.12; e cita `./mvnw test`, que não funciona no projeto (o
comando que funciona é `mvn test`).

Também fica registrado que `mvn test` executando a suíte completa termina em
BUILD FAILURE por causa de `PdvApplicationTests`, que falha ao subir o contexto
Spring quando o ambiente roda JDK 21 enquanto o projeto declara Java 8. É um
problema pré-existente, alheio a esta parte do trabalho, e reproduz mesmo com o
arquivo `CaixaLancamentoServiceTest.java` removido do projeto.
