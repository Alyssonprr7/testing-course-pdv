package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
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
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Receber;
import net.originmobi.pdv.model.Recebimento;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.TituloTipo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.RecebimentoRepository;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class RecebimentoServiceTest {

	@Mock
	private RecebimentoRepository recebimentos;

	@Mock
	private PessoaService pessoas;

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
	public void deveExigirTituloQuandoCodigoNulo() {
		// o nulo é verificado antes do zero, então a validação deixou de ser código morto (issue #9)
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Selecione um título para realizar o recebimento");

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, null);
	}

	@Test
	public void deveLancarExcecaoQuandoRecebimentoInexistente() {
		when(recebimentos.findById(1L)).thenReturn(Optional.empty());

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Recebimento não encontrado");

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);
	}

	@Test
	public void deveLancarExcecaoQuandoTituloInexistente() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.empty());

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Título não encontrado");

		recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);
	}

	@Test
	public void deveLancarExcecaoQuandoNaoHaCaixaAbertoPossivelBug() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 100.0)));
		// sobrescreve o stub do setUp: agora não existe caixa aberto
		when(caixas.caixaAberto()).thenReturn(Optional.empty());

		try {
			recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);
			fail("Deveria recusar o recebimento sem caixa aberto");
		} catch (RuntimeException e) {
			assertEquals("Nenhum caixa aberto", e.getMessage());
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
		// recebimento no cartão não depende de caixa aberto
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

	// ------------------------------------------- receber: laço de parcelas

	@Test
	public void deveEncerrarLacoQuandoSobraValorDepoisDaUltimaParcela() {
		// total do recebimento maior que a soma das parcelas: a sobra não pode estourar a lista
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L)).thenReturn(Arrays.asList(criaParcela(1L, 50.0)));

		String retorno = recebimentoService.receber(1L, 100.0, 0.0, 0.0, 1L);

		assertEquals("Recebimento realizado com sucesso", retorno);
		verify(parcelas).receber(1L, 50.0, 0.0, 0.0);
	}

	@Test
	public void deveRepassarZeroPositivoQuandoParcelaNaoTemSaldoRestante() {
		// restante zero: nada é quitado na primeira parcela e o valor segue para a próxima
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(50.0)));
		when(titulos.busca(1L)).thenReturn(Optional.of(criaTitulo("DIN")));
		when(receParcelas.parcelasDoReceber(1L))
				.thenReturn(Arrays.asList(criaParcela(1L, 0.0), criaParcela(2L, 50.0)));

		recebimentoService.receber(1L, 50.0, 0.0, 0.0, 1L);

		verify(parcelas).receber(1L, 0.0, 0.0, 0.0);
		verify(parcelas).receber(2L, 50.0, 0.0, 0.0);
	}

	// ------------------------------------------- abrirRecebimento

	@Test
	public void deveAbrirRecebimentoComUmaParcela() {
		Parcela parcela = criaParcelaDoCliente(5L, 100.0, 1L);
		Pessoa cliente = parcela.getReceber().getPessoa();
		when(parcelas.busca(5L)).thenReturn(parcela);
		when(pessoas.buscaPessoa(1L)).thenReturn(Optional.of(cliente));
		atribuiCodigoAoSalvar(7L);

		String retorno = recebimentoService.abrirRecebimento(1L, new String[] { "5" });

		assertEquals("7", retorno);
		ArgumentCaptor<Recebimento> salvo = ArgumentCaptor.forClass(Recebimento.class);
		verify(recebimentos).save(salvo.capture());
		assertEquals(100.0, salvo.getValue().getValor_total(), 0.0);
		assertSame(cliente, salvo.getValue().getPessoa());
		assertEquals(Arrays.asList(parcela), salvo.getValue().getParcela());
		assertNotNull(salvo.getValue().getData_cadastro());
	}

	@Test
	public void deveSomarRestanteDeTodasAsParcelasAoAbrirRecebimento() {
		Parcela primeira = criaParcelaDoCliente(5L, 60.5, 1L);
		Parcela segunda = criaParcelaDoCliente(6L, 39.5, 1L);
		when(parcelas.busca(5L)).thenReturn(primeira);
		doReturn(segunda).when(parcelas).busca(6L);
		when(pessoas.buscaPessoa(1L)).thenReturn(Optional.of(primeira.getReceber().getPessoa()));
		atribuiCodigoAoSalvar(7L);

		recebimentoService.abrirRecebimento(1L, new String[] { "5", "6" });

		ArgumentCaptor<Recebimento> salvo = ArgumentCaptor.forClass(Recebimento.class);
		verify(recebimentos).save(salvo.capture());
		assertEquals(100.0, salvo.getValue().getValor_total(), 0.0);
		assertEquals(Arrays.asList(primeira, segunda), salvo.getValue().getParcela());
	}

	@Test
	public void deveAbrirRecebimentoComTotalZeroQuandoListaDeParcelasVazia() {
		// classe "nenhuma parcela": o service não valida; a tela sempre envia ao menos um código
		when(pessoas.buscaPessoa(1L)).thenReturn(Optional.of(new Pessoa()));
		atribuiCodigoAoSalvar(7L);

		String retorno = recebimentoService.abrirRecebimento(1L, new String[] {});

		assertEquals("7", retorno);
		ArgumentCaptor<Recebimento> salvo = ArgumentCaptor.forClass(Recebimento.class);
		verify(recebimentos).save(salvo.capture());
		assertEquals(0.0, salvo.getValue().getValor_total(), 0.0);
		assertEquals(Collections.<Parcela>emptyList(), salvo.getValue().getParcela());
	}

	@Test
	public void deveLancarExcecaoAoAbrirRecebimentoComParcelaQuitada() {
		Parcela parcela = criaParcelaDoCliente(5L, 0.0, 1L);
		parcela.setQuitado(1);
		when(parcelas.busca(5L)).thenReturn(parcela);

		try {
			recebimentoService.abrirRecebimento(1L, new String[] { "5" });
			fail("deveria recusar parcela quitada");
		} catch (RuntimeException e) {
			assertEquals("Parcela 5 já esta quitada, verifique.", e.getMessage());
		}
		verify(recebimentos, never()).save(any(Recebimento.class));
	}

	@Test
	public void deveInterromperAberturaQuandoSegundaParcelaEstaQuitada() {
		Parcela quitada = criaParcelaDoCliente(6L, 0.0, 1L);
		quitada.setQuitado(1);
		when(parcelas.busca(5L)).thenReturn(criaParcelaDoCliente(5L, 10.0, 1L));
		doReturn(quitada).when(parcelas).busca(6L);

		try {
			recebimentoService.abrirRecebimento(1L, new String[] { "5", "6" });
			fail("deveria recusar a lista com parcela quitada");
		} catch (RuntimeException e) {
			assertEquals("Parcela 6 já esta quitada, verifique.", e.getMessage());
		}
		verify(recebimentos, never()).save(any(Recebimento.class));
	}

	@Test
	public void deveLancarExcecaoAoAbrirRecebimentoComParcelaDeOutroCliente() {
		when(parcelas.busca(5L)).thenReturn(criaParcelaDoCliente(5L, 100.0, 2L));

		try {
			recebimentoService.abrirRecebimento(1L, new String[] { "5" });
			fail("deveria recusar parcela de outro cliente");
		} catch (RuntimeException e) {
			assertEquals("A parcela 5 não pertence ao cliente selecionado", e.getMessage());
		}
		verify(recebimentos, never()).save(any(Recebimento.class));
	}

	@Test
	public void deveAceitarParcelaDoProprioClienteComCodigoAcimaDe127() {
		// O código é Long. Acima de 127 duas instâncias iguais não são o mesmo objeto, então a
		// comparação precisa ser por valor, e não por referência
		Parcela parcela = criaParcelaDoCliente(5L, 100.0, Long.valueOf(200L));
		when(parcelas.busca(5L)).thenReturn(parcela);
		when(pessoas.buscaPessoa(Long.valueOf(200L))).thenReturn(Optional.of(parcela.getReceber().getPessoa()));
		atribuiCodigoAoSalvar(7L);

		String retorno = recebimentoService.abrirRecebimento(Long.valueOf(200L), new String[] { "5" });

		assertEquals("7", retorno);
	}

	@Test
	public void deveLancarExcecaoAoAbrirRecebimentoComClienteInexistente() {
		when(parcelas.busca(5L)).thenReturn(criaParcelaDoCliente(5L, 100.0, 1L));
		when(pessoas.buscaPessoa(1L)).thenReturn(Optional.empty());

		try {
			recebimentoService.abrirRecebimento(1L, new String[] { "5" });
			fail("deveria recusar cliente inexistente");
		} catch (RuntimeException e) {
			assertEquals("Cliente não encontrado", e.getMessage());
		}
		verify(recebimentos, never()).save(any(Recebimento.class));
	}

	@Test
	public void deveLancarExcecaoAoAbrirRecebimentoQuandoRestanteDaParcelaNulo() {
		when(parcelas.busca(5L)).thenReturn(criaParcelaDoCliente(5L, null, 1L));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Erro ao receber, chame o suporte");

		recebimentoService.abrirRecebimento(1L, new String[] { "5" });
	}

	@Test
	public void deveLancarExcecaoQuandoFalhaAoSalvarRecebimentoAberto() {
		Parcela parcela = criaParcelaDoCliente(5L, 100.0, 1L);
		when(parcelas.busca(5L)).thenReturn(parcela);
		when(pessoas.buscaPessoa(1L)).thenReturn(Optional.of(parcela.getReceber().getPessoa()));
		doThrow(new RuntimeException("falha no banco")).when(recebimentos).save(any(Recebimento.class));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Erro ao receber, chame o suporte");

		recebimentoService.abrirRecebimento(1L, new String[] { "5" });
	}

	@Test(expected = NumberFormatException.class)
	public void deveLancarNumberFormatQuandoCodigoDaParcelaInvalido() {
		recebimentoService.abrirRecebimento(1L, new String[] { "abc" });
	}

	// ------------------------------------------- remover

	@Test
	public void deveRemoverRecebimentoNaoProcessado() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));

		String retorno = recebimentoService.remover(1L);

		assertEquals("removido com sucesso", retorno);
		verify(recebimentos).deleteById(1L);
	}

	@Test
	public void deveLancarExcecaoAoRemoverRecebimentoProcessado() {
		Recebimento recebimento = criaRecebimento(100.0);
		recebimento.setData_processamento(new Timestamp(System.currentTimeMillis()));
		when(recebimentos.findById(1L)).thenReturn(Optional.of(recebimento));

		try {
			recebimentoService.remover(1L);
			fail("deveria recusar recebimento processado");
		} catch (RuntimeException e) {
			assertEquals("Esse recebimento não pode ser removido, pois ele já esta processado", e.getMessage());
		}
		verify(recebimentos, never()).deleteById(anyLong());
	}

	@Test
	public void deveLancarExcecaoQuandoFalhaAoRemoverRecebimento() {
		when(recebimentos.findById(1L)).thenReturn(Optional.of(criaRecebimento(100.0)));
		doThrow(new RuntimeException("falha no banco")).when(recebimentos).deleteById(1L);

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Erro ao remover orçamento, chame o suporte");

		recebimentoService.remover(1L);
	}

	@Test
	public void deveTentarRemoverMesmoQuandoRecebimentoNaoExiste() {
		// sem recebimento não há data de processamento, então a exclusão segue para o repositório
		when(recebimentos.findById(1L)).thenReturn(Optional.empty());

		assertEquals("removido com sucesso", recebimentoService.remover(1L));
		verify(recebimentos).deleteById(1L);
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

	private Parcela criaParcelaDoCliente(Long codigo, Double valorRestante, Long codCliente) {
		Pessoa cliente = new Pessoa();
		cliente.setCodigo(codCliente);

		Receber receber = new Receber();
		receber.setPessoa(cliente);

		Parcela parcela = criaParcela(codigo, valorRestante);
		parcela.setReceber(receber);
		return parcela;
	}

	// o repositório real preenche o código ao salvar, e o service depende dele no retorno
	private void atribuiCodigoAoSalvar(Long codigo) {
		when(recebimentos.save(any(Recebimento.class))).thenAnswer(chamada -> {
			Recebimento recebimento = chamada.getArgument(0);
			recebimento.setCodigo(codigo);
			return recebimento;
		});
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
