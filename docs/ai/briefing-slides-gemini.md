# Briefing para o Gemini: slides sobre o processo do trabalho

## Instrução ao Gemini

Crie uma apresentação de slides (10 a 12 slides, em português do Brasil) para a
disciplina **Qualidade e Teste de Software** (Sistemas de Informação, UFF). O foco
**não é o código nem os resultados técnicos em detalhe**: é contar **como o grupo
criou e executou o trabalho, como dividiu as tarefas e como usou IA no processo**.

Tom: acadêmico, claro e objetivo. Slides enxutos (título + 3 a 5 tópicos curtos),
com tabelas e diagramas simples onde ajudar. Inclua sugestão de visual para cada
slide e, se possível, notas do apresentador. Use apenas as informações abaixo; não
invente números, datas ou resultados.

Estrutura sugerida:

1. Capa (título, disciplina, integrantes)
2. Contexto e objetivo do trabalho
3. Escolha do sistema-alvo (`pdv`) e critério usado
4. Divisão da equipe: classes e funcionalidades
5. Divisão da equipe: papéis transversais
6. Como organizamos o trabalho (ferramentas e repositório)
7. Como usamos IA: metodologia geral
8. Estudo de caso: testes de `CaixaService.cadastro`
9. Estudo de caso: testes de `CaixaService.fechaCaixa`
10. Plano de teste com apoio de IA
11. Validação humana e limitações da IA
12. Lições aprendidas e próximos passos

---

## 1. Contexto

- Trabalho em grupo de **5 integrantes**: Alysson Rocha, Bruno Taconi, Daniel
  Fernandes, Felipe Rato e José Augusto.
- Objetivo: aplicar técnicas de teste de software (unitário e manual) em um sistema
  real, com plano de teste, evidências e registro do uso de IA.
- Trabalho em duas entregas. Este material cobre o processo da primeira.

## 2. Escolha do sistema-alvo

- Projeto escolhido: **`pdv`**, um ERP/PDV web da organização
  `repo-software-testing-courses`. Foi feito um fork para o repositório do grupo.
- Stack: Java 8, Spring Boot 2.0.2, Thymeleaf, MySQL, Flyway, Maven, Docker.
- Funcionalidades do sistema: cadastros, controle de estoque, comandas, vendas,
  fluxo de caixa, contas a pagar e receber, venda com cartões, permissões por grupo
  e relatórios.
- **Critério da escolha:** a disciplina exige classes com complexidade ciclomática
  alta (muitas ramificações `if`/`else` e validações encadeadas). O `pdv` foi o
  candidato com mais classes de serviço densas em regra de negócio, o que rende casos
  de teste unitário não triviais.
- **Uso de IA nesta etapa:** o Claude ajudou a comparar os projetos candidatos e a
  propor a divisão. O grupo abriu cada classe no código e conferiu se os métodos
  indicados existiam e tinham a ramificação alegada. Só depois disso a escolha foi
  fechada.

## 3. Divisão do trabalho

Cada integrante recebeu **uma classe para teste unitário**, **uma funcionalidade
para teste manual** e **um papel transversal**. A divisão foi conferida para manter
a carga equilibrada.

| Integrante | Classe (teste unitário) | Funcionalidade (teste manual) | Papel transversal |
|---|---|---|---|
| Alysson | `CaixaService` | Abrir e fechar caixa | Líder de Teste / Repositório |
| José | `VendaService` | Fechar venda no PDV | TestLink |
| Felipe | `RecebimentoService` | Receber parcela / conta a receber | Bug tracking (GitHub Issues) |
| Daniel | `NotaFiscalItemService` | Adicionar item à NF-e | Documento de Escopo |
| Bruno | `CaixaLancamentoService` | Sangria / suprimento de caixa | Padrões & Evidências / AI-LOG |

Detalhes dos papéis:

- **Líder de Teste / Repositório:** cuida do repositório, da estrutura do projeto e
  da coordenação geral.
- **TestLink:** cadastra os cenários de teste na ferramenta.
- **Bug tracking:** abre e acompanha as issues de defeitos encontrados.
- **Documento de Escopo:** redige o que está dentro e fora do escopo dos testes.
- **Padrões & Evidências / AI-LOG:** garante o padrão dos artefatos, as evidências de
  execução e o registro do uso de IA.

Observação: o `NotaFiscalItemService` foi mantido com uma classe reserva acordada
(`ProdutoController`), por ser a mais difícil de mockar.

## 4. Organização e ferramentas

- **Repositório Git** com o fork do `pdv`, dividido em:
  - `src/test/java/`: testes unitários
  - `docs/ai/AI-LOG.md`: registro dos usos de IA
  - `docs/evidencias/`: evidências de execução, **uma pasta por integrante**
- **Testes unitários:** JUnit 4 (imposto pela versão do Spring Boot) + Mockito.
- **Cobertura e qualidade:** JaCoCo (já no `pom.xml`).
- **Gestão de testes e defeitos:** TestLink e GitHub Issues.
- **Documentação:** Plano de Teste em Google Docs, vinculado no `README.md`, e pasta
  compartilhada no Google Drive com os demais documentos.
- **IA:** Claude (Claude Code), com todo uso relevante registrado no AI-LOG.

## 5. Como usamos IA: metodologia

Regra do grupo: **cada integrante registra suas próprias entradas no AI-LOG**, no
mesmo formato (data, responsável, ferramenta, objetivo, prompts, resultado,
validação, limitações).

Princípio central: **a IA sugere, o grupo valida.** As sugestões eram ponto de
partida, nunca decisão pronta. O fluxo usado para os testes unitários foi:

1. **Scaffold:** a IA cria a classe de teste com mocks e dados de apoio, sem casos
   ainda.
2. **Testes sugeridos por nós:** o integrante define os cenários e a IA os
   implementa.
3. **Validação:** o integrante revisa cada teste contra o código real.
4. **Cobertura de lacunas:** a IA assume o papel de "testador experiente" e propõe
   os cenários que faltaram.
5. **Nova validação:** revisão dos testes adicionados.

Os prompts foram melhorados para maior clareza antes do uso e registrados no log.

## 6. Estudo de caso: `CaixaService.cadastro` (Alysson)

- Scaffold criado com `MockitoJUnitRunner`, mocks (`CaixaRepository`,
  `UsuarioService`, `CaixaLancamentoService`), contexto de segurança simulado e
  reset por reflexão do singleton `Aplicacao` (que vazaria estado entre testes).
- **14 testes** definidos pelo integrante: caixa aberto, valor inválido, falhas de
  `save` e de lançamento, cadastro válido, valor nulo, descrições padrão por tipo
  (Caixa diário, Cofre, Banco) e tratamento de agência/conta.
- **18 testes adicionais** propostos pela IA como testador experiente: regra de
  negócio, valores limite, efeitos colaterais que não podem ocorrer e entradas
  nulas.
- Total: **32 testes** para o método.
- **Validação humana encontrou pontos que a IA não previu**, por exemplo:
  - o `save` do mock não gera id, então o teste de retorno precisava definir o
    código antes;
  - `assertThrows` não existe no JUnit 4.12, então foi usado `ExpectedException`;
  - agência e conta precisavam ser válidas para não causar `NullPointerException`
    por outro motivo.
- Quatro testes **documentam comportamento atual que provavelmente é defeito**
  (falta de validação de entrada, causando `NullPointerException`). Podem virar
  issue de bug.

## 7. Estudo de caso: `CaixaService.fechaCaixa` (Alysson)

- Reaproveitou o scaffold da etapa anterior, no mesmo fluxo de cinco passos.
- **8 testes** definidos pelo integrante: senha vazia/nula/incorreta, caixa já
  fechado, falha no `save`, valor total nulo, erro de dependência e fechamento
  correto.
- **14 testes adicionais** da IA: efeitos colaterais (o que não pode ser chamado),
  variações de senha, falhas de dependência e valores limite.
- Total: **22 testes** para o método.
- Também foram encontrados comportamentos que indicam defeitos (usuário ou caixa
  inexistente, senha nula, valor total negativo aceito sem validação).

## 8. Plano de teste com apoio de IA

- A IA sugeriu o conteúdo esperado de cada seção do plano, considerando o sistema,
  a divisão da equipe e as exigências da disciplina.
- A IA também revisou os rascunhos, apontando lacunas, inconsistências e trechos
  pouco claros.
- **O plano foi escrito e mantido pelo grupo.** A IA atuou como apoio de estrutura e
  revisão. O grupo aceitou, adaptou ou descartou cada sugestão conforme o que foi
  realmente feito no projeto.

## 9. Validação humana e limitações da IA

- Todas as saídas da IA passaram por conferência do grupo contra o código real.
- A IA **não conseguiu compilar nem executar os testes** no ambiente usado, então
  nenhum resultado de "passou/falhou" foi verificado por ela. A execução e a
  evidência de cobertura dependem de rodar a suíte na IDE ou com Maven local.
- Testes que documentam comportamento atual precisam ser atualizados caso o código
  seja corrigido.
- Quando um enunciado de cenário chegou cortado, a IA assumiu o comportamento mais
  provável e isso foi registrado para conferência.

## 10. Lições aprendidas (sugestão de conteúdo para o último slide)

- Dividir por classe, funcionalidade e papel deixou a carga equilibrada e as
  responsabilidades claras.
- IA acelera o scaffold e amplia a cobertura de cenários, mas a validação humana é
  indispensável (ela pegou problemas de versão do JUnit, de mocks e de dados de
  entrada).
- Pedir à IA que aja como "testador experiente" revelou cenários que o grupo não
  tinha listado.
- Testar também revelou possíveis defeitos reais no sistema (falta de validação de
  entrada).
- Registrar prompts e validações no AI-LOG dá rastreabilidade ao uso de IA.

Próximos passos: executar as suítes e anexar evidências e cobertura, abrir issues
para os defeitos encontrados, completar os testes das demais classes e das
funcionalidades manuais.
