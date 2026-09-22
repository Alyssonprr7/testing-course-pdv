# AI-LOG — Felipe Ratto

Registro individual dos usos de IA feitos por Felipe Ratto no trabalho da disciplina
de teste de software. Este arquivo segue o mesmo formato do `AI-LOG.md` do grupo e
registra, em ordem cronológica, os prompts enviados e as respostas da IA nas
sessões conduzidas por mim.

## Cabeçalho

| Campo | Informação |
|---|---|
| **Responsável** | Felipe Ratto |
| **Ferramenta** | Claude Code (Claude Agent SDK), extensão para VS Code |
| **Modelo** | Claude Sonnet 5 (ID `claude-sonnet-5`) |
| **Início do registro** | 20/09/2026 |
| **Projeto** | `testing-course-pdv` (fork do `pdv`: ERP/PDV web em Java, Spring Boot 2.0.2, Thymeleaf, MySQL) |
| **Stack de teste** | JUnit 4 + Mockito |
| **Arquivo de referência** | [`AI-LOG.md`](AI-LOG.md) (registro do grupo) |

### Contexto

O trabalho consiste em criar testes unitários e manuais para um sistema real,
usando IA como apoio e documentando o uso de forma transparente. A divisão da
equipe (entrada 1 do `AI-LOG.md`) atribui a mim:

- **Classe para teste unitário:** `RecebimentoService`
  (`src/main/java/net/originmobi/pdv/service/RecebimentoService.java`), método
  `receber`, com o arquivo de testes em
  `src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java`.
- **Funcionalidade para teste manual:** receber parcela / conta a receber.
- **Papel transversal:** bug tracking (Issues).

### Convenções deste registro

- Cada interação relevante vira uma entrada numerada, no mesmo formato do
  `AI-LOG.md`: data, responsável, ferramenta, objetivo, prompt, resultado e
  validação.
- O prompt é transcrito como enviado; quando houver melhoria de clareza, isso fica
  indicado.
- Resultados de execução só são registrados como "passou/falhou" quando de fato
  foram rodados. Enquanto isso não acontecer, ficam como **pendente**.
- As sugestões da IA são ponto de partida e passam por validação minha antes de
  entrarem no repositório.

---

## 1. Revisão dos testes unitários de `RecebimentoService.receber`

- **Data:** 20/09/2026
- **Responsável:** Felipe Ratto
- **Ferramenta:** Claude (Claude Code), modelo Claude Sonnet 5

**Objetivo**

Ter uma revisão dos testes unitários que escrevi (JUnit 4 + Mockito) para
`RecebimentoService`, com sugestões de melhoria explicadas de forma didática, para
fixar o conteúdo e entender o porquê de cada ajuste.

**Prompt utilizado**

> Escrevi testes unitários com JUnit 4 + Mockito para a classe RecebimentoService:
>
> - Classe de teste: src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java
> - Classe testada: src/main/java/net/originmobi/pdv/service/RecebimentoService.java
>
> Pode revisar meus testes e sugerir melhorias? explique-as com clareza para melhor
> fixação do conteúdo e contextualização.

**Resultado**

A IA leu a classe de teste (11 testes, todos sobre `receber`; a resposta original
informou 10 por engano, corrigido a pedido de Felipe na entrada 2), a classe testada e
os apoios (`Aplicacao`, enum `TituloTipo`, assinaturas de `CaixaLancamento`,
`Recebimento` e `CartaoLancamentoService`). Não alterou nenhum arquivo. A revisão
apontou:

- **Pontos fortes:** uso de objetos de domínio reais em vez de mocks, helpers
  `criaRecebimento`/`criaParcela`/`criaTitulo`, stubs que fazem cada exceção vir da
  validação pretendida (e não de outra anterior), `tearDown` limpando o
  `SecurityContextHolder`, uso de `never()` nos testes de cartão x caixa.
- **Melhorias, por prioridade:**
  1. Nenhum teste verifica o estado final gravado em `recebimentos.save` (valor
     recebido, acréscimo, desconto, data de processamento, título vinculado); usar
     `ArgumentCaptor<Recebimento>`.
  2. Os três `catch` do service (falha em `parcelas.receber`, em
     `lancamentos.lancamento` e em `recebimentos.save`) não têm teste; verificar a
     mensagem e os efeitos colaterais que não podem ocorrer (por exemplo, não salvar
     o recebimento se o lançamento no caixa falhou).
  3. `verify(lancamentos).lancamento(any(CaixaLancamento.class))` é uma verificação
     fraca; capturar o `CaixaLancamento` e conferir observação, valor, tipo
     `RECEBIMENTO`, estilo `ENTRADA`.
  4. Cenários de ramificação e limite que faltam: pagamento que atravessa parcelas
     (80,00 sobre parcelas de 60,00 e 40,00), valor no limite (100,01 sobre 100,00 e
     0,01), arredondamento do total (99,996 tratado como 100,00), acréscimo e
     desconto diferentes de zero e cartão sem consultar o caixa.
  5. Comportamentos atuais que parecem defeitos e podem ser documentados por teste
     de caracterização: `codtitulo` nulo gera `NullPointerException` (a comparação
     `codtitulo == 0` vem antes de `codtitulo == null`), recebimento ou título
     inexistente geram `NoSuchElementException`, caixa fechado só é detectado depois
     de as parcelas já terem sido recebidas, e acréscimo/desconto não chegam a
     `parcelas.receber` (sempre `0.00`).
  6. Qualidade do teste: `MockitoJUnitRunner.Silent` esconde stubs não usados no
     `setUp` (preferir o runner estrito e mover os stubs para um helper de cenário
     feliz); `ExpectedException` com `RuntimeException.class` é genérico e não
     permite verificar nada depois da exceção (usar `try/catch` com `fail()` quando
     precisar de `verify`); usar o enum em vez de siglas em texto.
  7. Fora do escopo dos testes atuais: `abrirRecebimento` e `remover` não têm testes.

**Validação**

- As afirmações sobre o comportamento do service foram conferidas linha a linha no
  código de `receber` (ordem das validações, cálculo de `vlsobra`/`vlquitado` no laço,
  ramificação por sigla, `catch` que engolem a causa original).
- Os exemplos numéricos (80,00 sobre 60,00 + 40,00 resultando em 60,00 e 20,00) foram
  calculados à mão a partir do laço.
- Os pontos 4 e 5 são hipóteses derivadas da leitura do código e ainda precisam ser
  confirmadas escrevendo e rodando os testes.

**Limitações da IA / observações**

Não foi possível executar a suíte neste ambiente (não há `mvn` instalado), então
nenhum resultado de "passou/falhou" foi verificado. O comportamento de arredondamento
do `DecimalFormat` da ICU e o comportamento padrão dos mocks do Mockito (`Optional`
vazio para métodos sem stub) foram assumidos com base no conhecimento da IA e devem
ser confirmados na execução.

---

## 2. Aplicação das melhorias em `RecebimentoServiceTest`

- **Data:** 20/09/2026
- **Responsável:** Felipe Ratto
- **Ferramenta:** Claude (Claude Code), modelo Claude Sonnet 5

**Objetivo**

Aplicar no arquivo de teste as melhorias 1 a 5 da revisão da entrada 1, mantendo o
estilo do `CaixaServiceTest`, e corrigir a contagem de testes informada na revisão.

**Prompt utilizado**

> Obrigado pela revisão, ficou bem clara! Só um detalhe: você citou 10 testes, mas eu
> contei 11 no arquivo.
>
> Pode aplicar as melhorias no RecebimentoServiceTest.java, com estas regras:
>
> APLICAR:
> - Melhoria 1: ArgumentCaptor no recebimentos.save, conferindo valor recebido,
>   acréscimo, desconto, data de processamento e título.
> - Melhoria 2: testes para as três falhas (parcelas.receber, lancamentos.lancamento e
>   recebimentos.save), usando try/catch e conferindo que o save não acontece depois da
>   falha no lançamento.
> - Melhoria 3: ArgumentCaptor no CaixaLancamento, conferindo observação, valor, tipo e
>   estilo.
> - Melhoria 4: pagamento de 80 sobre parcelas de 60 e 40, limites 100,01 e 0,01, e um
>   teste com acréscimo/desconto diferentes de zero. Deixe o arredondamento de 99,996 de
>   fora por enquanto, porque depende do Locale da máquina.
> - Melhoria 5: testes para codtitulo nulo, recebimento inexistente, título inexistente
>   e caixa sem lançamento possível. Dê nomes que deixem claro que documentam um
>   possível bug (ex.: deveLancarNullPointerQuandoTituloNulo), pois vão mudar se o
>   service for corrigido.
>
> NÃO APLICAR:
> - Melhoria 6 (trocar runner, extrair helpers, mudar siglas): mantenha o estilo do
>   CaixaServiceTest, com MockitoJUnitRunner.Silent.
> - Melhoria 7 (abrirRecebimento e remover): fora do escopo.
>
> ESTILO (importante):
> - Adicione comentários no código.
> - Não mexa em outros arquivos além do teste.
>
> Depois me diga quais testes novos foram adicionados.

**Resultado**

- A IA confirmou a contagem correta (11 testes originais, um erro da revisão anterior)
  e corrigiu o número na entrada 1.
- Foram adicionados 13 testes em `RecebimentoServiceTest.java`, que passou de 11 para
  24 testes, todos com comentários e agrupados por seções no estilo do
  `CaixaServiceTest`:
  - **Melhoria 1:** `deveGravarRecebimentoProcessadoAoFinalizar`
  - **Melhoria 3:** `deveMontarLancamentoDoCaixaComDadosDoRecebimento`
  - **Melhoria 2:** `deveLancarExcecaoQuandoFalhaAoReceberParcela`,
    `deveLancarExcecaoENaoSalvarQuandoFalhaAoLancarNoCaixa`,
    `deveLancarExcecaoQuandoFalhaAoSalvarRecebimento`
  - **Melhoria 4:** `devePagarPrimeiraParcelaEParteDaSegundaQuandoValorAtravessaParcelas`,
    `deveLancarExcecaoQuandoValorRecebidoExcedeTotalEmUmCentavo`,
    `deveAceitarValorRecebidoDeUmCentavo`,
    `deveGravarAcrescimoEDescontoMasNaoRepassarAsParcelasPossivelBug`
  - **Melhoria 5:** `deveLancarNullPointerQuandoTituloNulo`,
    `deveLancarNoSuchElementQuandoRecebimentoInexistente`,
    `deveLancarNoSuchElementQuandoTituloInexistente`,
    `deveLancarNoSuchElementQuandoNaoHaCaixaAbertoPossivelBug`
- Não foram feitos: melhorias 6 e 7, teste de arredondamento de 99,996, nenhuma
  mudança de runner (continua `MockitoJUnitRunner.Silent`), de siglas ou de helpers
  existentes. Nenhum outro arquivo do projeto foi alterado (apenas este log, por
  causa da regra de registro).

**Validação**

- Cada teste novo foi conferido contra o fluxo de `receber` para garantir que a
  exceção ou verificação vem do ponto pretendido (por exemplo, `codtitulo` nulo falha
  na linha 108 antes de qualquer outro passo).
- Os valores do cenário 80 sobre 60 + 40 (60 e 20) e do limite de 0,01 foram
  calculados à mão a partir do laço do service.
- Os quatro testes de "possível bug" e o de acréscimo/desconto documentam o
  comportamento atual e devem ser atualizados se o `RecebimentoService` for corrigido;
  são candidatos a issues.
- Execução: **pendente**.

**Limitações da IA / observações**

A IA não conseguiu compilar nem executar os testes neste ambiente (sem `mvn` e sem
dependências baixadas), então nenhum resultado de "passou/falhou" foi verificado. É
preciso rodar a suíte na IDE e anexar a evidência. Também foi assumido que os mocks
sem stub retornam `Optional` vazio (comportamento do Mockito 2).

---

## 3. Revalidação dos testes e mutação manual em `RecebimentoService.receber`

- **Data:** 20/09/2026
- **Responsável:** Felipe Ratto
- **Ferramenta:** Claude (Claude Code), modelo Claude Sonnet 5

**Objetivo**

Depois de a suíte inteira passar, verificar se os testes estão bem construídos e se o
resultado verde é confiável, e usar teste de mutação manual como veredito final.

**Prompt utilizado**

> Executei os testes e estranhamente todos retornaram sucesso, verifiquei os testes e
> o código em si e não encontrei inconsistencias, sem falha.
>
> Verifique novamente todos os testes e valide se estão devidamente construidos,
> cogite alterações e verifique o código fonte, caso nada seja encontrado, vamos
> realizar um Teste da mutação manual para veredito final.

**Resultado**

- **Execução independente:** a IA encontrou as dependências no `~/.m2` e o código
  compilado em `target/`, e rodou o `RecebimentoServiceTest` direto com o JUnit
  (JDK 26, `-Dnet.bytebuddy.experimental=true`, classpath montado à mão, sem
  Maven/Surefire): `OK (24 tests)`. Isso confirma o resultado do Felipe.
- **Revisão da construção dos testes:** nenhum teste malformado. Os testes de
  exceção chegam ao `if` pretendido e o `try/catch` com `fail()` está correto.
  Foram encontrados vazios de verificação (usuário e caixa do lançamento, título
  entregue ao cartão, códigos informados, caixa não consultado no cartão).
- **Mutação manual:** 46 mutantes (M01 a M46) aplicados em uma **cópia** de
  `RecebimentoService.java` fora do projeto, cada um compilado e executado contra a
  suíte. O código-fonte do projeto não foi alterado.
  - Suíte atual (24 testes): 35 mortos e 11 sobreviventes (76%).
  - Sobreviventes: M01 (equivalente: `codtitulo == null` é código morto, pois o
    desembrulho do `Long` nulo lança `NullPointerException` antes), M20 (título
    entregue ao cartão), M25 e M40 (usuário do lançamento), M27 (recebimento
    vinculado ao lançamento), M41 (arredondamento do total, excluído por decisão do
    Felipe), M42 (caixa consultado no ramo do cartão), M43, M45 e M46 (códigos
    fixos no lugar dos argumentos) e M44 (ordem entre parcelas vazias e valor
    zero).
  - Com 6 testes propostos (em cópia, fora do projeto): 44 mortos; sobram só M01 e
    M41. Os 6 propostos são: usuário e caixa do lançamento, recebimento vinculado
    ao lançamento (por reflexão), título entregue ao cartão, cartão sem consultar
    caixa, códigos diferentes de 1 (recebimento 5, título 7) e prioridade da
    validação de parcelas.
  - Com o teste de arredondamento (99,996) somado: 45 mortos; sobrevive só o M01.
- **Locale:** o teste de arredondamento passou em pt-BR, en-US, de-DE e fr-FR. Só
  falhou em ar-EG (dígitos arábico-índicos), onde o próprio service já quebra
  (`NumberFormatException`) e vários testes atuais também falham. Ou seja, o
  arredondamento não é mais sensível ao Locale do que o restante da suíte.
- Nenhuma alteração foi aplicada ao `RecebimentoServiceTest.java` nesta entrada;
  as propostas aguardam decisão do Felipe.

**Validação**

- A execução do controle (M00, sem mutação) passou com os 24 testes, garantindo que o
  método de execução é confiável.
- Cada mutante que sobreviveu foi classificado como lacuna real, mutante equivalente
  ou exclusão consciente, e cada proposta foi comprovada matando o mutante que
  visava.
- `git status` confirmou que só o `AI-LOG-FELIPERATTO.md` e arquivos da IDE
  (`.classpath`, `.project`, `.settings/`) estão fora do versionamento.

**Limitações da IA / observações**

A execução foi feita com o JUnit direto, não com o Maven, e em JDK 26; o resultado
pode diferir do ambiente da IDE. O conjunto de mutantes foi escolhido à mão e não é
exaustivo. Os sobreviventes M01 (código morto) e o ponto de acréscimo/desconto não
repassado às parcelas são candidatos a issue de bug.

---

## 4. Implementação dos 7 testes propostos pela mutação

- **Data:** 20/09/2026
- **Responsável:** Felipe Ratto
- **Ferramenta:** Claude (Claude Code), modelo Claude Sonnet 5

**Objetivo**

Implementar em `RecebimentoServiceTest.java` os 7 testes propostos na entrada 3 para
matar os mutantes sobreviventes.

**Prompt utilizado**

> Impemente os 7 testes por favor

**Resultado**

Foram adicionados 7 testes (a suíte passou de 24 para 31), todos com comentários e
em duas seções novas ("argumentos repassados às dependências" e "ordem das
validações e arredondamento"). Foi acrescentado o `import java.lang.reflect.Field`.

- `deveVincularUsuarioECaixaAbertosAoLancamentoDoCaixa` (mata M25, M40 e o caixa do
  lançamento)
- `deveVincularRecebimentoAoLancamentoDoCaixa` (M27, por reflexão no campo privado,
  pois `CaixaLancamento` não tem `getRecebimento`)
- `deveEntregarOTituloInformadoAoLancamentoDoCartao` (M20)
- `deveReceberNoCartaoSemConsultarCaixaAberto` (M42)
- `deveUsarOsCodigosInformadosEmVezDeValoresFixos` (M43, M45, M46)
- `deveValidarParcelasAntesDoValorRecebido` (M44)
- `deveArredondarTotalParaDuasCasasAntesDeComparar` (M41)

Nenhum outro arquivo do projeto foi alterado (apenas este log).

**Validação**

- Compilação do arquivo real e execução direta com o JUnit (JDK 26, sem Maven):
  `OK (31 tests)`.
- Mutação repetida contra o arquivo real: 45 de 46 mutantes mortos. O único
  sobrevivente é o M01, mutante equivalente (`codtitulo == null` é código morto do
  service).
- Pendente: rodar a suíte na IDE/Maven do Felipe e anexar a evidência.

**Limitações da IA / observações**

O teste de reflexão depende do nome do campo privado `recebimento` em
`CaixaLancamento`; se o campo for renomeado, o teste quebra. O M01 só some com uma
correção no service, que também exigiria atualizar `deveLancarNullPointerQuandoTituloNulo`.

---

## 5. Confirmação da execução e encaminhamento do M01

- **Data:** 20/09/2026
- **Responsável:** Felipe Ratto
- **Ferramenta:** Claude (Claude Code), modelo Claude Sonnet 5

**Objetivo**

Registrar o resultado da execução dos 31 testes na máquina do Felipe e o destino do
único mutante sobrevivente.

**Prompt utilizado**

> Ok, rodei os testes e todos foram aprovados, acredito que a essa altura podemos
> confiar nos resultados. Quanto ao caso do M01 vou criar uma Issue para correção
> posteriormente, obrigado!

**Resultado**

- Felipe executou `RecebimentoServiceTest` (31 testes) no ambiente dele e todos
  passaram. Isso fecha o item "Execução: pendente" da entrada 4. A execução foi feita
  por Felipe; a IA só a havia reproduzido antes com o JUnit direto.
- O M01 (`codtitulo == null` é código morto em `RecebimentoService.receber`, linha 108,
  porque `codtitulo == 0` desembrulha o `Long` e lança `NullPointerException` antes)
  será tratado por uma Issue aberta pelo Felipe. Ao corrigir o service (por exemplo,
  `codtitulo == null || codtitulo == 0`), o teste `deveLancarNullPointerQuandoTituloNulo`
  precisa ser atualizado para esperar a mensagem "Selecione um título para realizar o
  recebimento".
- Ainda pendente: anexar a evidência da execução (print/relatório) e de cobertura, se
  exigida pela disciplina. Nenhum arquivo do projeto foi alterado nesta entrada.

**Limitações da IA / observações**

O resultado "aprovado" vem do relato do Felipe; a IA não viu a saída da execução dele.

---

## 6. Redação da Issue do M01 no padrão do template de bug

- **Data:** 20/09/2026
- **Responsável:** Felipe Ratto
- **Ferramenta:** Claude (Claude Code), modelo Claude Sonnet 5

**Objetivo**

Escrever, no padrão do template `.github/ISSUE_TEMPLATE/bug_report.md`, o texto da
Issue sobre o mutante M01 (validação de título nulo em `RecebimentoService.receber`).

**Prompt utilizado**

> Para a Issue preciso deixar claro aqui neste markdown, seguindo padrão
>
> ## Descrição
> Descreva o problema encontrado.
>
> ## Passos para reproduzir
> 1.
> 2.
> 3.
>
> ## Resultado esperado
> O que deveria acontecer.
>
> ## Resultado obtido
> O que de fato aconteceu.
>
> ## Ambiente
> - Branch/commit:
> - Tipo de teste: manual / unitário / integração
> - Classe ou funcionalidade testada:
>
> ## Evidência
> (anexar print ou log)
>
> ## Responsável
> Nome do integrante que encontrou o bug

**Resultado**

- A IA confirmou que o template colado é o mesmo do repositório
  (`.github/ISSUE_TEMPLATE/bug_report.md`) e entregou o texto da Issue na resposta,
  sem criar arquivo novo no projeto.
- Antes de escrever, leu `RecebimentoController` e `TituloService` e encontrou dois
  fatos que mudam o enquadramento do bug: (1) o controller converte título vazio em
  `0L`, então pela tela o `codtitulo` nulo não chega ao service (o defeito é latente,
  de severidade baixa); (2) com o `TituloService` real, `busca(null)` chama
  `findById(null)`, que pelo contrato do `CrudRepository` lança
  `IllegalArgumentException` antes de chegar à linha 108. O `NullPointerException`
  do teste só acontece porque o `TituloService` é mock.
- A Issue foi redigida sem afirmar reprodução pela interface, apenas por teste
  unitário.

**Validação**

Os fatos sobre o controller e o `TituloService` foram lidos no código. O comportamento
de `findById(null)` com repositório real **não foi executado**, apenas inferido do
contrato do Spring Data; a Issue o marca como não verificado.

**Limitações da IA / observações**

O commit informado na Issue é o `HEAD` atual; os 31 testes ainda não estavam
commitados, então Felipe deve atualizar o campo após o commit. A evidência (print da
execução) precisa ser anexada por ele.

---
## 7. Execução manual dos testes de "Receber parcela" e organização dos dados coletados

- **Data:** 20/09/2026
- **Responsável:** Felipe Ratto
- **Ferramenta:** Nenhuma IA nesta etapa (execução manual)

**Objetivo**

Executar manualmente, no navegador Brave, os 5 cenários de teste da funcionalidade
"Receber parcela / conta a receber" (RC-01 a RC-05), coletando evidências para
posterior documentação.

**O que foi feito**

- Execução manual de cada cenário no sistema PDV rodando via Docker, usando o
  navegador Brave.
- Para cada cenário: consulta ao banco de dados MySQL antes e depois da execução
  (snapshot dos valores relevantes: parcela, recebimento, caixa).
- Captura de prints das telas relevantes de cada passo.
- Registro do passo a passo executado, valores usados e alertas exibidos em arquivo
  JSON (`execucao.json`, um por caso).
- Anotação, por caso, se o resultado obtido bateu ou não com o resultado esperado
  (PASSOU / FALHOU), incluindo os dois casos com divergência (RC-03 e RC-04) e o
  achado adicional A1 (falha ao gerar parcela via venda "A Prazo").

**Resultado**

Conjunto de evidências brutas por caso (prints + `db-antes`/`db-depois` + `execucao.json`
+ anotação de status), salvo em `docs/evidencias/felipe/manuais/RC-0X/`, servindo de
base para a entrada 8 (montagem do documento final).

**Validação**

Conferência feita no momento da execução, comparando resultado esperado x observado
na própria interface e no banco de dados, antes de anotar o status de cada caso.

---
## 8. Preenchimento do modelo Word com os resultados dos testes manuais

- **Data:** 20/09/2026
- **Responsável:** Felipe Ratto
- **Ferramenta:** Claude (Claude Code), modelo Claude Sonnet 5

**Objetivo**

Transformar as evidências brutas coletadas manualmente na entrada 7 (prints, banco
antes/depois, passo a passo em JSON e anotações de status) em um documento de caso de
teste formatado, seguindo o template do grupo.

**Prompt utilizado**

> Precisamos alimentar o template
> @"docs/evidencias/felipe/manuais/exemploCasoDeTeste.doc" com as informações presentes em docs/evidencias/felipe/manuais

**Resultado**


- Mapeamento: fluxo principal = RC-01; Fluxos Alternativos 1 a 4 = RC-02 a RC-05. Foram
  preenchidos título, objetivo, testador, data, ambiente (navegador Brave 1.94.121,
  MySQL 8.0.46, Arch Linux/CachyOS — conforme informado por Felipe), pré-requisitos,
  notas e 21 passos com esperado/real e PASSOU/FALHOU, todos derivados dos dados
  coletados manualmente na entrada 7 (não reexecutados pela IA).
- Foi acrescentada ao final uma seção "EVIDÊNCIAS" com 10 prints (2 por caso),
  copiados das pastas `RC-01` a `RC-05`.
- Escolhas da IA: o título usa "RC-01 a RC-05" (nenhum código "CT-..." foi inventado); a
  coluna "Requisitos validados" recebeu o id do caso, pois não há requisitos numerados
  formalmente ainda; a numeração dos passos é contínua entre os fluxos (numeração
  automática do modelo).


**Limitações da IA / observações**

- Falta abrir o arquivo no Word ou Google Docs para conferir paginação e tamanho dos
  prints. (Realizado)
- Só entraram no documento os dados já coletados na entrada 7; nada foi reexecutado
  pela IA.


---

## 9. Redação do corpo das Issues de bug (RC-03, RC-04 e A1)

- **Data:** 20/09/2026
- **Responsável:** Felipe Ratto
- **Ferramenta:** Claude, modelo Claude Sonnet 5

**Objetivo**

Redigir o corpo das 3 issues de bug identificadas nos testes manuais da funcionalidade
"Receber parcela / conta a receber" (RC-03, RC-04) e na preparação de dados (A1), seguindo
o template de bug report já configurado no repositório (`.github/ISSUE_TEMPLATE/bug_report.md`).

**Prompt utilizado**

> Preciso criar as Issues para os bugs vistos nos testes manuais, o commit referenciado é: 4a4b12e

> Evidencias: docs/evidencias/felipe/manuais

> Anexo: Teste manual - Receber parcela.docx 


**Resultado**

A IA gerou o texto de 3 issues (título, labels sugeridas e corpo no formato do template:
Descrição, Passos para reproduzir, Resultado esperado, Resultado obtido, Ambiente, Causa
provável, Evidência, Responsável), a partir dos dados já registrados nas Evidencias e Anexo:

- Issue 1 — RC-03: desconto informado no recebimento não é aplicado à baixa da parcela
  (parcela não quitada apesar do alerta de sucesso).
- Issue 2 — RC-04: acréscimo informado no recebimento não é refletido no lançamento de
  caixa (valor cobrado a mais "desaparece" do caixa).
- Issue 3 — A1: falha ao fechar venda "A Prazo" (`parcela.data_alteracao` sem `DEFAULT`
  no schema), achado durante a preparação de dados para os testes de RC-01 a RC-05.


**Decisão**

- Textos das 3 issues aceitos como estavam, com o commit `4a4b12e` referenciado no campo
  Ambiente de cada uma.
- Labels da Issue 2 ajustadas: a IA sugeriu inicialmente apenas `modulo:financeiro`
  (por a causa do bug estar na `RecebimentoService`), mas Felipe optou por manter também
  `modulo:caixa` e `modulo:vendas`, por ser onde o sintoma é observado.
  Responsável mantido como Felipe Ratto em todas, por ter sido quem executou os testes que
  encontraram os defeitos.

**Validação**

(valores de banco antes/depois, trechos de código citados como causa provável) antes da criação manual no GitHub.

**Limitações da IA / observações**

- A IA não tem acesso ao repositório para criar as issues diretamente (nem foi autorizada
  a fazer isso nesta interação); o texto gerado precisa ser colado manualmente.