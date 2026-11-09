CREATE TABLE IF NOT EXISTS pessoa (
    codigo BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255),
    cpf_cnpj VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS usuario (
    codigo BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_cadastro DATE,
    pessoa_codigo BIGINT,
    senha VARCHAR(255),
    user VARCHAR(255),
    FOREIGN KEY (pessoa_codigo) REFERENCES pessoa(codigo)
);

CREATE TABLE IF NOT EXISTS venda (
    codigo BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_cadastro TIMESTAMP,
    data_cancelado TIMESTAMP,
    data_finalizado TIMESTAMP,
    observacao VARCHAR(255),
    pessoa_codigo BIGINT,
    situacao VARCHAR(50),
    usuario_codigo BIGINT,
    valor_acrescimo DECIMAL(10,2),
    valor_desconto DECIMAL(10,2),
    valor_produtos DECIMAL(10,2),
    valor_total DECIMAL(10,2),
    FOREIGN KEY (usuario_codigo) REFERENCES usuario(codigo),
    FOREIGN KEY (pessoa_codigo) REFERENCES pessoa(codigo)
);

CREATE TABLE IF NOT EXISTS pagamento_tipo (
    codigo BIGINT AUTO_INCREMENT PRIMARY KEY,
    descricao VARCHAR(255),
    forma_pagamento VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS pagamento_tipo_venda (
    ven_codigo BIGINT,
    pag_tipo_codigo BIGINT,
    FOREIGN KEY (ven_codigo) REFERENCES venda(codigo),
    FOREIGN KEY (pag_tipo_codigo) REFERENCES pagamento_tipo(codigo)
);