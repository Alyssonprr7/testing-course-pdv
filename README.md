# pdv

Projeto desenvolvido para a disciplina de Qualidade e Testes de Software, do curso de Sistemas de Informação da Universidade Federal Fluminense (UFF).

Sistema de ERP web desenvolvido em Java com Spring Framework

# Integrantes
- Alysson Rocha
- Bruno
- Daniel
- Felipe Rato
- José Augusto

# Plano de Teste
[Acesse o plano de teste aqui](https://docs.google.com/document/d/1uxhmSSpGm3gTKWZWIFvaeuEIjmxqehvo/edit?usp=sharing&ouid=109721338967909889524&rtpof=true&sd=true)

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
Para instalar o sistema, você deve criar o banco de dado "pdv" no mysql e configurar o arquivo application.properties
com os dados do seu usuário root do mysql e rodar o projeto pelo Eclipse ou gerar o jar do mesmo e execultar.

# Logando no sistema
Para logar no sistema, use o usuário "gerente" e a senha "123".

# Tecnologias utilizadas
- Spring Framework 5
- Thymeleaf 3
- MySQL
- Hibernate
- FlyWay

# Execução com Docker
Para executar a aplicação utilizando o docker, utilize o seguinte comando na raiz do projeto:
```sh
docker compose up -d
```

