# pdv

Projeto desenvolvido para a disciplina de Qualidade e Testes de Software, do curso de Sistemas de Informação da Universidade Federal Fluminense (UFF).

Sistema de ERP web desenvolvido em Java com Spring Framework

# Integrantes
- Alysson Rocha
- Bruno Taconi
- Daniel Fernandes
- Felipe Rato
- José Augusto

# Plano de Teste
[Acesse o plano de teste aqui](https://docs.google.com/document/d/1uxhmSSpGm3gTKWZWIFvaeuEIjmxqehvo/edit?usp=sharing)

# Documentos do Trabalho
[Pasta no Google Drive com os demais arquivos](https://drive.google.com/drive/folders/1kaB5_6IXWjn08Hi6ciTrBH_lG2oXEZnD)

# Artefatos do trabalho
- `docs/ai/AI-LOG.md`: registro dos usos de IA
- `docs/evidencias/`: evidências de execução, uma pasta por integrante
- `src/test/java/`: testes unitários

# Recursos
- Cadastro produtos/clientes/fornecedor
- Controle de estoque
- Gerenciar comandas
- Realizar venda
- Controle de fluxo de caixa
- Controle de pagar e receber
- Venda com cartões
- Gerenciar permissões de usuários por grupos
- Cadastrar novas formas de pagamentos
- Relatórios

# Instalação
> As seções "Instalação", "Logando no sistema" e "Execução com Docker" vêm do projeto original.

Para instalar o sistema, você deve criar o banco de dados "pdv" no mysql e configurar o arquivo application.properties
com os dados do seu usuário root do mysql e rodar o projeto pelo Eclipse ou gerar o jar do mesmo e executar.

# Logando no sistema
Para logar no sistema, use o usuário "gerente" e a senha "123".

# Tecnologias utilizadas
- Spring Framework 5
- Thymeleaf 3
- MySQL
- Hibernate
- Flyway

# Execução com Docker
Para executar a aplicação utilizando o docker, utilize o seguinte comando na raiz do projeto:
```sh
docker compose up -d
```

# Executando os testes
Com o Maven instalado, na raiz do projeto:
```sh
mvn test
```
