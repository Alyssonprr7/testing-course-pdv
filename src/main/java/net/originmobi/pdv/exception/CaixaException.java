package net.originmobi.pdv.exception;

/**
 * Exceção de negócio lançada pelas operações de caixa (abertura, fechamento e
 * lançamentos iniciais).
 */
public class CaixaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CaixaException(String message) {
		super(message);
	}

	public CaixaException(String message, Throwable cause) {
		super(message, cause);
	}
}
