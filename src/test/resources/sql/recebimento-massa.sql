-- Massa dos testes de sistema e integração de Recebimento. Pode ser reaplicada a qualquer momento.
-- As parcelas entram direto no banco porque a venda "A Prazo" não gera parcela (issue #12).
-- Uso: docker compose exec -T pdv-db mysql -uroot -p123456 pdv < src/test/resources/sql/recebimento-massa.sql

DROP TEMPORARY TABLE IF EXISTS massa_recebimento;
CREATE TEMPORARY TABLE massa_recebimento AS SELECT DISTINCT recebimento_cod AS codigo FROM recebimento_parcelas WHERE parcela_cod BETWEEN 9001 AND 9099;
DELETE FROM caixa_lancamento WHERE recebimento_codigo IN (SELECT codigo FROM massa_recebimento);
DELETE FROM recebimento_parcelas WHERE parcela_cod BETWEEN 9001 AND 9099;
DELETE FROM recebimento WHERE codigo IN (SELECT codigo FROM massa_recebimento);
DROP TEMPORARY TABLE massa_recebimento;
DELETE FROM parcela WHERE codigo BETWEEN 9001 AND 9099;
DELETE FROM receber WHERE codigo BETWEEN 9001 AND 9002;
DELETE FROM venda WHERE codigo BETWEEN 9001 AND 9002;
DELETE FROM pessoa WHERE codigo = 9001;
DELETE FROM titulo WHERE codigo = 9001;

-- O banco novo não tem título cadastrado
INSERT INTO titulo (codigo, descricao, tipo_codigo) VALUES (9001, 'Dinheiro Massa', 1);

-- Cliente com código acima de 127, para exercitar a comparação de Long em abrirRecebimento
INSERT INTO pessoa (codigo, nome, cpfcnpj, apelido, data_nascimento, observacao, endereco_codigo, data_cadastro)
VALUES (9001, 'Cliente Massa Recebimento', '000.000.009-01', 'Massa', '1990-01-01', 'Massa de teste', 1, '2026-01-01');

INSERT INTO venda (codigo, situacao, pessoa_codigo, usuario_codigo, valor_total) VALUES
(9001, 'FECHADA', 1, 1, 700), (9002, 'FECHADA', 9001, 1, 100);

INSERT INTO receber (codigo, observacao, valor_total, pessoa_codigo, data_cadastro, venda_codigo) VALUES
(9001, 'Massa de teste', 700, 1, NOW(), 9001), (9002, 'Massa de teste', 100, 9001, NOW(), 9002);

INSERT INTO parcela (codigo, valor_total, valor_desconto, valor_acrescimo, valor_recebido, valor_restante, quitado, sequencia, data_cadastro, data_alteracao, data_vencimento, receber_codigo) VALUES
(9001, 100, 0, 0, 0, 100, 0, 1, NOW(), NOW(), '2030-01-10', 9001),
(9002, 100, 0, 0, 0, 100, 0, 2, NOW(), NOW(), '2030-02-10', 9001),
(9003,  60, 0, 0, 0,  60, 0, 3, NOW(), NOW(), '2030-03-10', 9001),
(9004,  40, 0, 0, 0,  40, 0, 4, NOW(), NOW(), '2030-04-10', 9001),
(9005, 100, 0, 0, 100, 0, 1, 5, NOW(), NOW(), '2030-05-10', 9001),
(9006, 100, 0, 0, 0, 100, 0, 6, NOW(), NOW(), '2030-06-10', 9001),
(9007, 100, 0, 0, 0, 100, 0, 7, NOW(), NOW(), '2030-07-10', 9001),
(9008, 100, 0, 0, 0, 100, 0, 8, NOW(), NOW(), '2030-08-10', 9001),
(9011, 100, 0, 0, 0, 100, 0, 1, NOW(), NOW(), '2030-01-10', 9002);
