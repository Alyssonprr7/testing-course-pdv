package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.CaixaLancamentoRepository;

@RunWith(MockitoJUnitRunner.class)
public class CaixaLancamentoServiceTest {

	private static final String SUCESSO = "Lançamento realizado com sucesso";
	private static final String SALDO_INSUFICIENTE = "Saldo insuficiente para realizar esta operação";

	@Mock
	private CaixaLancamentoRepository caixaLancamento;

	@InjectMocks
	private CaixaLancamentoService service;

	/** Caixa aberto (data_fechamento nula) com o saldo informado. */
	private Caixa caixaAberto(Double valorTotal) {
		return new Caixa("caixa teste", CaixaTipo.CAIXA, valorTotal, valorTotal, null, new Date(), null, new Usuario());
	}

	/** Caixa fechado, com data de fechamento preenchida. */
	private Caixa caixaFechado(Double valorTotal) {
		return new Caixa("caixa teste", CaixaTipo.CAIXA, valorTotal, valorTotal, valorTotal, new Date(),
				new Timestamp(System.currentTimeMillis()), new Usuario());
	}

	/** Lançamento montado com o construtor real da entidade. */
	private CaixaLancamento lancamento(String observacao, Double valor, TipoLancamento tipo, EstiloLancamento estilo,
			Caixa caixa) {
		return new CaixaLancamento(observacao, valor, tipo, estilo, caixa, new Usuario());
	}

	/** Captura o objeto efetivamente entregue ao repository. */
	private CaixaLancamento capturarSalvo() {
		ArgumentCaptor<CaixaLancamento> captor = ArgumentCaptor.forClass(CaixaLancamento.class);
		verify(caixaLancamento).save(captor.capture());
		return captor.getValue();
	}

	// ------------------------------------------------------------------
	// Cenário 1 - lançamento sem caixa aberto
	// ------------------------------------------------------------------

	/**
	 * DIVERGÊNCIA: o guard de "Nenhum caixa aberto" é inalcançável, então uma
	 * ENTRADA sem caixa nenhum é salva normalmente. Teste documenta o
	 * comportamento atual.
	 */
	@Test
	public void entradaSemCaixa_salvaEmVezDeRecusar() {
		CaixaLancamento lanc = lancamento("suprimento", 50.0, TipoLancamento.SUPRIMENTO, EstiloLancamento.ENTRADA,
				null);

		assertEquals(SUCESSO, service.lancamento(lanc));
		verify(caixaLancamento).save(lanc);
	}

	/**
	 * DIVERGÊNCIA: uma SAIDA sem caixa quebra no {@code vlTotalCaixa.get()}
	 * (NoSuchElementException). O catch genérico relança um RuntimeException sem
	 * mensagem, e não "Nenhum caixa aberto".
	 */
	@Test
	public void saidaSemCaixa_lancaRuntimeExceptionSemMensagem() {
		CaixaLancamento lanc = lancamento("sangria", 50.0, TipoLancamento.SANGRIA, EstiloLancamento.SAIDA, null);

		try {
			service.lancamento(lanc);
			fail("esperava RuntimeException");
		} catch (RuntimeException e) {
			assertNull(e.getMessage());
		}
		verify(caixaLancamento, never()).save(lanc);
	}

	/**
	 * DIVERGÊNCIA: caixa já fechado (data_fechamento preenchida) também não é
	 * bloqueado pelo serviço.
	 */
	@Test
	public void lancamentoEmCaixaFechado_naoEhBloqueado() {
		CaixaLancamento lanc = lancamento("suprimento", 50.0, TipoLancamento.SUPRIMENTO, EstiloLancamento.ENTRADA,
				caixaFechado(100.0));

		assertEquals(SUCESSO, service.lancamento(lanc));
		verify(caixaLancamento).save(lanc);
	}

	// ------------------------------------------------------------------
	// Cenário 2 - saída com saldo insuficiente
	// ------------------------------------------------------------------

	@Test
	public void saidaComSaldoInsuficiente_retornaMensagemENaoSalva() {
		CaixaLancamento lanc = lancamento("sangria", 150.0, TipoLancamento.SANGRIA, EstiloLancamento.SAIDA,
				caixaAberto(100.0));

		assertEquals(SALDO_INSUFICIENTE, service.lancamento(lanc));
		verify(caixaLancamento, never()).save(lanc);
		// o valor não chega a ser convertido para negativo
		assertEquals(Double.valueOf(150.0), lanc.getValor());
	}

	// ------------------------------------------------------------------
	// Cenário 3 - saída positiva convertida para valor negativo
	// ------------------------------------------------------------------

	@Test
	public void saidaComValorPositivo_ehConvertidaParaNegativo() {
		CaixaLancamento lanc = lancamento("retirada", 50.0, TipoLancamento.SANGRIA, EstiloLancamento.SAIDA,
				caixaAberto(100.0));

		assertEquals(SUCESSO, service.lancamento(lanc));
		assertEquals(Double.valueOf(-50.0), capturarSalvo().getValor());
	}

	// ------------------------------------------------------------------
	// Cenário 4 - sangria com observação vazia
	// ------------------------------------------------------------------

	@Test
	public void sangriaComObservacaoVazia_recebeObservacaoPadrao() {
		CaixaLancamento lanc = lancamento("", 50.0, TipoLancamento.SANGRIA, EstiloLancamento.SAIDA,
				caixaAberto(100.0));

		assertEquals(SUCESSO, service.lancamento(lanc));
		assertEquals("Sangria de caixa", capturarSalvo().getObservacao());
	}

	// ------------------------------------------------------------------
	// Cenário 5 - suprimento com observação vazia
	// ------------------------------------------------------------------

	@Test
	public void suprimentoComObservacaoVazia_recebeObservacaoPadrao() {
		CaixaLancamento lanc = lancamento("", 50.0, TipoLancamento.SUPRIMENTO, EstiloLancamento.ENTRADA,
				caixaAberto(100.0));

		assertEquals(SUCESSO, service.lancamento(lanc));
		assertEquals("Suprimento de caixa", capturarSalvo().getObservacao());
	}

	// ------------------------------------------------------------------
	// Cenário 6 - falha do repository ao salvar
	// ------------------------------------------------------------------

	@Test
	public void falhaAoSalvar_lancaRuntimeExceptionComMensagemDeSuporte() {
		CaixaLancamento lanc = lancamento("sangria", 50.0, TipoLancamento.SANGRIA, EstiloLancamento.SAIDA,
				caixaAberto(100.0));
		doThrow(new RuntimeException("falha no banco")).when(caixaLancamento).save(lanc);

		try {
			service.lancamento(lanc);
			fail("esperava RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Erro ao realizar lançamento, chame o suporte", e.getMessage());
		}
	}

	// ==================================================================
	// Passo 4 - casos adicionais
	// ==================================================================

	// ------------------------------------------------------------------
	// Regra de saldo
	// ------------------------------------------------------------------

	/**
	 * DEFEITO: valor negativo passa pela validação de saldo, porque -200 > 100 é
	 * falso. Como já é negativo, também não é convertido. O caixa fica negativo.
	 */
	@Test
	public void saidaComValorNegativo_furaValidacaoDeSaldo() {
		CaixaLancamento lanc = lancamento("sangria", -200.0, TipoLancamento.SANGRIA, EstiloLancamento.SAIDA,
				caixaAberto(100.0));

		assertEquals(SUCESSO, service.lancamento(lanc));
		assertEquals(Double.valueOf(-200.0), capturarSalvo().getValor());
	}

	/**
	 * DEFEITO: ENTRADA não passa por nenhuma validação, então um valor negativo
	 * reduz o caixa sem checagem de saldo.
	 */
	@Test
	public void entradaComValorNegativo_naoEhValidada() {
		CaixaLancamento lanc = lancamento("suprimento", -500.0, TipoLancamento.SUPRIMENTO, EstiloLancamento.ENTRADA,
				caixaAberto(100.0));

		assertEquals(SUCESSO, service.lancamento(lanc));
		assertEquals(Double.valueOf(-500.0), capturarSalvo().getValor());
	}

	/** Limite: a comparação é > estrito, então saída igual ao saldo é aceita. */
	@Test
	public void saidaComValorIgualAoSaldo_ehPermitida() {
		CaixaLancamento lanc = lancamento("sangria", 100.0, TipoLancamento.SANGRIA, EstiloLancamento.SAIDA,
				caixaAberto(100.0));

		assertEquals(SUCESSO, service.lancamento(lanc));
		assertEquals(Double.valueOf(-100.0), capturarSalvo().getValor());
	}

	/** Limite: um centavo acima do saldo já é recusado. */
	@Test
	public void saidaUmCentavoAcimaDoSaldo_ehRecusada() {
		CaixaLancamento lanc = lancamento("sangria", 100.01, TipoLancamento.SANGRIA, EstiloLancamento.SAIDA,
				caixaAberto(100.0));

		assertEquals(SALDO_INSUFICIENTE, service.lancamento(lanc));
		verify(caixaLancamento, never()).save(lanc);
	}

	// ------------------------------------------------------------------
	// Observação
	// ------------------------------------------------------------------

	/** Observação preenchida não pode ser sobrescrita pelo texto padrão. */
	@Test
	public void sangriaComObservacaoPreenchida_mantemOTextoInformado() {
		CaixaLancamento lanc = lancamento("teste", 50.0, TipoLancamento.SANGRIA, EstiloLancamento.SAIDA,
				caixaAberto(100.0));

		assertEquals(SUCESSO, service.lancamento(lanc));
		assertEquals("teste", capturarSalvo().getObservacao());
	}

	/**
	 * DIVERGÊNCIA: só SANGRIA e SUPRIMENTO têm observação padrão. Os demais tipos
	 * entram no if e recebem setObservacao(""), que não muda nada.
	 */
	@Test
	public void recebimentoComObservacaoVazia_ficaSemObservacaoPadrao() {
		CaixaLancamento lanc = lancamento("", 50.0, TipoLancamento.RECEBIMENTO, EstiloLancamento.ENTRADA,
				caixaAberto(100.0));

		assertEquals(SUCESSO, service.lancamento(lanc));
		assertEquals("", capturarSalvo().getObservacao());
	}

	/**
	 * DIVERGÊNCIA: a checagem usa isEmpty() e não trim().isEmpty(), então uma
	 * observação só com espaços não recebe o texto padrão.
	 */
	@Test
	public void sangriaComObservacaoEmBranco_naoRecebeObservacaoPadrao() {
		CaixaLancamento lanc = lancamento("   ", 50.0, TipoLancamento.SANGRIA, EstiloLancamento.SAIDA,
				caixaAberto(100.0));

		assertEquals(SUCESSO, service.lancamento(lanc));
		assertEquals("   ", capturarSalvo().getObservacao());
	}

	// ------------------------------------------------------------------
	// Efeito colateral
	// ------------------------------------------------------------------

	/**
	 * DIVERGÊNCIA: data_cadastro é preenchida antes de qualquer validação, então o
	 * objeto do chamador sai mutado mesmo numa operação recusada.
	 */
	@Test
	public void saldoInsuficiente_aindaAssimPreencheDataCadastro() {
		CaixaLancamento lanc = lancamento("sangria", 150.0, TipoLancamento.SANGRIA, EstiloLancamento.SAIDA,
				caixaAberto(100.0));

		assertEquals(SALDO_INSUFICIENTE, service.lancamento(lanc));
		assertNotNull(lanc.getData_cadastro());
	}

}
