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
