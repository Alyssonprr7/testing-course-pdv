package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
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

	// ------------------------------------------- estado gravado em recebimentos.save

	@Test
	public void deveGravarRecebimentoProcessadoAoFinalizar() {
		Titulo titulo = criaTitulo("DIN");
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(titulo));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		// captura o objeto que o service entregou ao repository
		ArgumentCaptor<Recebimento> captor = ArgumentCaptor.forClass(Recebimento.class);
		verify(recebimentos).save(captor.capture());
		Recebimento salvo = captor.getValue();

		assertEquals(Double.valueOf(100.0), salvo.getValor_recebido());
		assertEquals(Double.valueOf(0.0), salvo.getValor_acrescimo());
		assertEquals(Double.valueOf(0.0), salvo.getValor_desconto());
		// o recebimento chegou sem data de processamento, então ela precisa ter sido preenchida
		assertNotNull(salvo.getData_processamento());
		// o título informado precisa estar vinculado ao recebimento gravado
		assertSame(titulo, salvo.getTitulo());
	}

	// ------------------------------------------- lançamento no caixa

	@Test
	public void deveMontarLancamentoDoCaixaComDadosDoRecebimento() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		// captura o lançamento para conferir o conteúdo, e não só que ele foi chamado
		ArgumentCaptor<CaixaLancamento> captor = ArgumentCaptor.forClass(CaixaLancamento.class);
		verify(lancamentos).lancamento(captor.capture());
		CaixaLancamento lancamento = captor.getValue();

		assertEquals("Referente ao recebimento 1", lancamento.getObservacao());
		assertEquals(Double.valueOf(100.0), lancamento.getValor());
		assertEquals(TipoLancamento.RECEBIMENTO, lancamento.getTipo());
		assertEquals(EstiloLancamento.ENTRADA, lancamento.getEstilo());
	}

	// ------------------------------------------- falhas nas dependências (blocos catch)

	@Test
	public void deveLancarExcecaoQuandoFalhaAoReceberParcela() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));
		// doThrow funciona tanto para métodos void quanto para os que retornam valor
		doThrow(new RuntimeException("falha no banco")).when(parcelas).receber(anyLong(), anyDouble(), anyDouble(),
				anyDouble());

		try {
			recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);
			fail("Deveria ter lançado exceção");
		} catch (RuntimeException e) {
			assertEquals("Ocorreu um erro ao realizar o recebimento, chame o suporte", e.getMessage());
		}

		// a falha interrompe o processo: nada é lançado no caixa nem gravado
		verify(lancamentos, never()).lancamento(any(CaixaLancamento.class));
		verify(recebimentos, never()).save(any(Recebimento.class));
	}

	@Test
	public void deveLancarExcecaoENaoSalvarQuandoFalhaAoLancarNoCaixa() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));
		doThrow(new RuntimeException("falha no banco")).when(lancamentos).lancamento(any(CaixaLancamento.class));

		try {
			recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);
			fail("Deveria ter lançado exceção");
		} catch (RuntimeException e) {
			assertEquals("Ocorreu um erro ao realizar o recebimento, chame o suporte", e.getMessage());
		}

		// o recebimento não pode ser marcado como processado se o caixa não foi lançado
		verify(recebimentos, never()).save(any(Recebimento.class));
	}

	@Test
	public void deveLancarExcecaoQuandoFalhaAoSalvarRecebimento() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));
		doThrow(new RuntimeException("falha no banco")).when(recebimentos).save(any(Recebimento.class));

		try {
			recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);
			fail("Deveria ter lançado exceção");
		} catch (RuntimeException e) {
			assertEquals("Ocorreu um erro ao realizar o recebimento, chame o suporte", e.getMessage());
		}

		// o lançamento no caixa já tinha sido feito antes do save; em produção o
		// @Transactional desfaz isso, mas com mocks o service em si não desfaz nada
		verify(lancamentos).lancamento(any(CaixaLancamento.class));
	}

	// ------------------------------------------- distribuição do valor e limites

	@Test
	public void devePagarPrimeiraParcelaEParteDaSegundaQuandoValorAtravessaParcelas() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L))
				.thenReturn(Arrays.asList(criaParcela(1L, 60.0), criaParcela(2L, 40.0)));

		String retorno = recebimentoService.receber(1L, 80.0, 0.0, 0.0, 1L);

		assertEquals("Recebimento realizado com sucesso", retorno);
		// 80 = 60 quitando a primeira + 20 abatendo a segunda
		verify(parcelas).receber(1L, 60.0, 0.0, 0.0);
		verify(parcelas).receber(2L, 20.0, 0.0, 0.0);
	}

	@Test
	public void deveLancarExcecaoQuandoValorRecebidoExcedeTotalEmUmCentavo() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Valor de recebimento é superior aos títulos");

		// limite superior: 100,01 sobre um total de 100,00
		recebimentoService.receber(1L, 100.01, 0.0, 0.0, 1L);
	}

	@Test
	public void deveAceitarValorRecebidoDeUmCentavo() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		// limite inferior: 0,01 é o menor valor positivo, portanto válido
		String retorno = recebimentoService.receber(1L, 0.01, 0.0, 0.0, 1L);

		assertEquals("Recebimento realizado com sucesso", retorno);
		verify(parcelas).receber(1L, 0.01, 0.0, 0.0);
	}

	@Test
	public void deveGravarAcrescimoEDescontoMasNaoRepassarAsParcelasPossivelBug() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		recebimentoService.receber(1L, 100.0, 5.0, 2.0, 1L);

		ArgumentCaptor<Recebimento> captor = ArgumentCaptor.forClass(Recebimento.class);
		verify(recebimentos).save(captor.capture());
		assertEquals(Double.valueOf(5.0), captor.getValue().getValor_acrescimo());
		assertEquals(Double.valueOf(2.0), captor.getValue().getValor_desconto());

		// comportamento atual: o service sempre passa 0.00 de acréscimo e desconto para
		// a parcela, mesmo quando eles foram informados. Provável defeito; se o service
		// for corrigido, este verify deve ser atualizado
		verify(parcelas).receber(1L, 100.0, 0.0, 0.0);
	}

	// ------------------------------------------- comportamento atual que parece defeito
	// Os testes abaixo documentam o que o service faz HOJE. Se ele for corrigido
	// (validação de entrada), os testes precisam ser atualizados.

	@Test
	public void deveLancarNullPointerQuandoTituloNulo() {
		// codtitulo == 0 é avaliado antes de codtitulo == null, e o desembrulho do
		// Long nulo lança NullPointerException em vez da mensagem de título obrigatório
		thrown.expect(NullPointerException.class);

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, null);
	}

	@Test
	public void deveLancarNoSuchElementQuandoRecebimentoInexistente() {
		// findById retorna Optional vazio e o service chama .get() sem validar
		when(recebimentos.findById(1L)).thenReturn(Optional.empty());
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));

		thrown.expect(NoSuchElementException.class);

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);
	}

	@Test
	public void deveLancarNoSuchElementQuandoTituloInexistente() {
		// o título não é encontrado, mas o service faz titulo.get() para vinculá-lo
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.empty());

		thrown.expect(NoSuchElementException.class);

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);
	}

	@Test
	public void deveLancarNoSuchElementQuandoNaoHaCaixaAbertoPossivelBug() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));
		// sobrescreve o stub do setUp: agora não existe caixa aberto
		when(caixas.caixaAberto()).thenReturn(Optional.empty());

		try {
			recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);
			fail("Deveria ter lançado NoSuchElementException");
		} catch (NoSuchElementException e) {
			// esperado: caixa.get() sem verificar se há caixa aberto
		}

		// o problema: a ausência de caixa só é detectada depois de a parcela já ter
		// sido recebida. A checagem deveria acontecer antes de qualquer alteração
		verify(parcelas).receber(1L, 100.0, 0.0, 0.0);
		verify(lancamentos, never()).lancamento(any(CaixaLancamento.class));
		verify(recebimentos, never()).save(any(Recebimento.class));
	}

	// ------------------------------------------- argumentos repassados às dependências
	// Cenários incluídos após a mutação manual: sem eles, trocar esses argumentos
	// no service não fazia nenhum teste falhar.

	@Test
	public void deveVincularUsuarioECaixaAbertosAoLancamentoDoCaixa() {
		// objetos guardados em variáveis para comparar por identidade depois
		Usuario usuario = new Usuario();
		Caixa caixa = new Caixa();
		when(usuarios.buscaUsuario("usuario_teste")).thenReturn(usuario);
		when(caixas.caixaAberto()).thenReturn(Optional.of(caixa));
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		ArgumentCaptor<CaixaLancamento> captor = ArgumentCaptor.forClass(CaixaLancamento.class);
		verify(lancamentos).lancamento(captor.capture());
		// o lançamento precisa ser do usuário logado e do caixa que está aberto
		assertSame(usuario, captor.getValue().getUsuario());
		assertSame(caixa, captor.getValue().getCaixa().get());
	}

	@Test
	public void deveVincularRecebimentoAoLancamentoDoCaixa() throws Exception {
		Recebimento recebimento = criaRecebimento(100.0);
		when(recebimentos.findById(1L)).thenReturn(Optional.of(recebimento));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		ArgumentCaptor<CaixaLancamento> captor = ArgumentCaptor.forClass(CaixaLancamento.class);
		verify(lancamentos).lancamento(captor.capture());
		// CaixaLancamento não possui getRecebimento, então o campo privado é lido por reflexão
		Field campo = CaixaLancamento.class.getDeclaredField("recebimento");
		campo.setAccessible(true);
		assertSame(recebimento, campo.get(captor.getValue()));
	}

	@Test
	public void deveEntregarOTituloInformadoAoLancamentoDoCartao() {
		Titulo titulo = criaTitulo("CARTDEB");
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(titulo));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		// eq(Optional.of(titulo)) exige o título informado, ao contrário de any()
		verify(cartaoLancamentos).lancamento(eq(100.0), eq(Optional.of(titulo)));
	}

	@Test
	public void deveReceberNoCartaoSemConsultarCaixaAberto() {
		// não há caixa aberto, mas recebimento no cartão não depende de caixa
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("CARTCRED")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		String retorno = recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		assertEquals("Recebimento realizado com sucesso", retorno);
		verify(caixas, never()).caixaAberto();
	}

	@Test
	public void deveUsarOsCodigosInformadosEmVezDeValoresFixos() {
		// códigos diferentes de 1 provam que os argumentos são repassados às dependências
		Recebimento recebimento = criaRecebimento(100.0);
		recebimento.setCodigo(5L);
		when(recebimentos.findById(5L)).thenReturn(Optional.of(recebimento));
		when(titulos.busca(7L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(5L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		String retorno = recebimentoService.receber(5L, 100.0, 0.0, 0.0, 7L);

		assertEquals("Recebimento realizado com sucesso", retorno);
		// o código do recebimento também aparece na observação do lançamento
		ArgumentCaptor<CaixaLancamento> captor = ArgumentCaptor.forClass(CaixaLancamento.class);
		verify(lancamentos).lancamento(captor.capture());
		assertEquals("Referente ao recebimento 5", captor.getValue().getObservacao());
	}

	// ------------------------------------------- ordem das validações e arredondamento

	@Test
	public void deveValidarParcelasAntesDoValorRecebido() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Collections.<Parcela>emptyList());

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Recebimento não possue parcelas");

		// com as duas condições inválidas (sem parcelas e valor zero), vale a validação de parcelas
		recebimentoService.receber(1L, 0.0, 0.0, 0.0, 1L);
	}

	@Test
	public void deveArredondarTotalParaDuasCasasAntesDeComparar() {
		// 99,996 é formatado como 100,00; receber 100,00 não pode ser "superior aos títulos"
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(99.996)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));

		String retorno = recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		assertEquals("Recebimento realizado com sucesso", retorno);
	}

	// ------------------------------------------- helpers

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
