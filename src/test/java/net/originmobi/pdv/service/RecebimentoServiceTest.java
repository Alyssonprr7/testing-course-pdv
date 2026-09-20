package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Parcela;
import net.originmobi.pdv.model.Recebimento;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.TituloTipo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.RecebimentoRepository;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RecebimentoServiceTest {

	@Mock
	private RecebimentoRepository recebimentos;

	@Mock
	private RecebimentoParcelaService receParcelas;

	@Mock
	private ParcelaService parcelas;

	@Mock
	private CaixaService caixas;

	@Mock
	private UsuarioService usuarios;

	@Mock
	private CaixaLancamentoService lancamentos;

	@Mock
	private TituloService titulos;

	@Mock
	private CartaoLancamentoService cartaoLancamentos;

	@InjectMocks
	private RecebimentoService recebimentoService;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("usuario_teste", null));
		when(usuarios.buscaUsuario("usuario_teste")).thenReturn(new Usuario());
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));
	}

	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void deveLancarExcecaoQuandoTituloNaoSelecionado() {
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Selecione um título para realizar o recebimento");

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 0L);
	}

	@Test
	public void deveLancarExcecaoQuandoRecebimentoJaFechado() {
		Recebimento recebimento = criaRecebimento(100.0);
		recebimento.setData_processamento(new Timestamp(System.currentTimeMillis()));
		when(recebimentos.findById(1L)).thenReturn(Optional.of(recebimento));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Recebimento já esta fechado");

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);
	}

	@Test
	public void deveLancarExcecaoQuandoValorRecebidoMaiorQueTotal() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Valor de recebimento é superior aos títulos");

		recebimentoService.receber(1L, 150.0, 0.0, 0.0, 1L);
	}

	@Test
	public void deveLancarExcecaoQuandoRecebimentoSemParcelas() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Collections.<Parcela>emptyList());

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Recebimento não possue parcelas");

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);
	}

	@Test
	public void deveLancarExcecaoQuandoValorRecebidoZero() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Valor de recebimento inválido");

		recebimentoService.receber(1L, 0.0, 0.0, 0.0, 1L);
	}

	@Test
	public void deveLancarExcecaoQuandoValorRecebidoNegativo() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Valor de recebimento inválido");

		recebimentoService.receber(1L, -10.0, 0.0, 0.0, 1L);
	}

	@Test
	public void deveQuitarParcialmenteQuandoValorMenorQueTotal() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L))
				.thenReturn(Arrays.asList(criaParcela(1L, 60.0), criaParcela(2L, 40.0)));

		String retorno = recebimentoService.receber(1L, 50.0, 0.0, 0.0, 1L);

		assertEquals("Recebimento realizado com sucesso", retorno);
		verify(parcelas).receber(1L, 50.0, 0.0, 0.0);
		verify(parcelas, never()).receber(eq(2L), anyDouble(), anyDouble(), anyDouble());
	}

	@Test
	public void deveQuitarTotalmenteQuandoValorIgualAoTotal() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L))
				.thenReturn(Arrays.asList(criaParcela(1L, 60.0), criaParcela(2L, 40.0)));

		String retorno = recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		assertEquals("Recebimento realizado com sucesso", retorno);
		verify(parcelas).receber(1L, 60.0, 0.0, 0.0);
		verify(parcelas).receber(2L, 40.0, 0.0, 0.0);
	}

	@Test
	public void deveLancarNoCaixaQuandoTituloEmDinheiro() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		verify(lancamentos).lancamento(any(CaixaLancamento.class));
		verify(cartaoLancamentos, never()).lancamento(anyDouble(), any());
	}

	@Test
	public void deveLancarNoCartaoQuandoTituloCartaoDebito() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("CARTDEB")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		verify(cartaoLancamentos).lancamento(eq(100.0), any());
		verify(lancamentos, never()).lancamento(any(CaixaLancamento.class));
	}

	@Test
	public void deveLancarNoCartaoQuandoTituloCartaoCredito() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("CARTCRED")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		verify(cartaoLancamentos).lancamento(eq(100.0), any());
		verify(lancamentos, never()).lancamento(any(CaixaLancamento.class));
	}

	private Recebimento criaRecebimento(Double valorTotal) {
		Recebimento recebimento = new Recebimento();
		recebimento.setCodigo(1L);
		recebimento.setValor_total(valorTotal);
		return recebimento;
	}

	private Parcela criaParcela(Long codigo, Double valorRestante) {
		Parcela parcela = new Parcela();
		parcela.setCodigo(codigo);
		parcela.setValor_restante(valorRestante);
		return parcela;
	}

	private Titulo criaTitulo(String sigla) {
		TituloTipo tipo = new TituloTipo();
		tipo.setSigla(sigla);

		Titulo titulo = new Titulo();
		titulo.setCodigo(1L);
		titulo.setTipo(tipo);
		return titulo;
	}
}
