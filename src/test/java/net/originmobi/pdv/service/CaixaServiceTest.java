package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Timestamp;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.CaixaRepository;
import net.originmobi.pdv.singleton.Aplicacao;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CaixaServiceTest {

	private static final String USUARIO_LOGADO = "usuario_teste";
	private static final String SENHA_PLANA = "123456";

	@Mock
	private CaixaRepository caixas;

	@Mock
	private UsuarioService usuarios;

	@Mock
	private CaixaLancamentoService lancamentos;

	@InjectMocks
	private CaixaService caixaService;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Usuario usuario;

	@Before
	public void setUp() throws Exception {
		// Aplicacao é um singleton que lê o SecurityContext uma única vez,
		// então é preciso limpar a instância antes de cada teste
		resetAplicacaoSingleton();
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(USUARIO_LOGADO, null));

		usuario = criaUsuario();
		when(usuarios.buscaUsuario(USUARIO_LOGADO)).thenReturn(usuario);

		// por padrão não há caixa aberto
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
	}

	@After
	public void tearDown() throws Exception {
		SecurityContextHolder.clearContext();
		resetAplicacaoSingleton();
	}

	@Test
	public void deveLancarExcecaoQuandoJaExisteCaixaAberto() {
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Existe caixa de dias anteriores em aberto, favor verifique");

		caixaService.cadastro(criaCaixa(CaixaTipo.CAIXA, 10.0, "Caixa"));
	}

	@Test
	public void deveLancarExcecaoQuandoValorAberturaNegativo() {
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Valor informado é inválido");

		caixaService.cadastro(criaCaixa(CaixaTipo.CAIXA, -1.0, "Caixa"));
	}

	@Test
	public void deveLancarExcecaoQuandoSaveFalha() {
		when(caixas.save(any(Caixa.class))).thenThrow(new RuntimeException("falha qualquer no save"));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Erro no processo de abertura, chame o suporte técnico");

		caixaService.cadastro(criaCaixa(CaixaTipo.CAIXA, 10.0, "Caixa"));
	}

	@Test
	public void deveLancarExcecaoQuandoLancamentoFalha() {
		doThrow(new RuntimeException("falha qualquer no lançamento")).when(lancamentos)
				.lancamento(any(CaixaLancamento.class));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Erro no processo, chame o suporte");

		caixaService.cadastro(criaCaixa(CaixaTipo.CAIXA, 10.0, "Caixa"));
	}

	@Test
	public void deveCadastrarCaixaComValorPositivoERetornarCodigo() {
		Caixa caixa = criaCaixa(CaixaTipo.CAIXA, 100.0, "Caixa");
		// o repository é mock e não gera id, então o código é definido previamente
		caixa.setCodigo(1L);

		Long codigo = caixaService.cadastro(caixa);

		assertNotNull(codigo);
		assertEquals(Long.valueOf(1L), codigo);
	}

	@Test
	public void deveZerarValoresQuandoValorAberturaNulo() {
		Caixa caixa = criaCaixa(CaixaTipo.CAIXA, null, "Caixa");
		caixa.setCodigo(1L);

		Long codigo = caixaService.cadastro(caixa);

		assertNotNull(codigo);
		assertEquals(Double.valueOf(0.0), caixa.getValor_abertura());
		assertEquals(Double.valueOf(0.0), caixa.getValor_total());
	}

	@Test
	public void deveUsarDescricaoPadraoParaCaixa() {
		Caixa caixa = criaCaixa(CaixaTipo.CAIXA, 0.0, "");

		caixaService.cadastro(caixa);

		assertEquals("Caixa diário", caixa.getDescricao());
	}

	@Test
	public void deveUsarDescricaoPadraoParaCofre() {
		Caixa caixa = criaCaixa(CaixaTipo.COFRE, 0.0, "");

		caixaService.cadastro(caixa);

		assertEquals("Cofre", caixa.getDescricao());
	}

	@Test
	public void deveUsarDescricaoPadraoParaBanco() {
		Caixa caixa = criaBanco(0.0, "1234", "5678");

		caixaService.cadastro(caixa);

		assertEquals("Banco", caixa.getDescricao());
	}

	@Test
	public void deveManterDescricaoInformadaParaCaixa() {
		Caixa caixa = criaCaixa(CaixaTipo.CAIXA, 0.0, "Minha descrição");

		caixaService.cadastro(caixa);

		assertEquals("Minha descrição", caixa.getDescricao());
	}

	@Test
	public void deveManterDescricaoInformadaParaCofre() {
		Caixa caixa = criaCaixa(CaixaTipo.COFRE, 0.0, "Minha descrição");

		caixaService.cadastro(caixa);

		assertEquals("Minha descrição", caixa.getDescricao());
	}

	@Test
	public void deveManterDescricaoInformadaParaBanco() {
		Caixa caixa = criaBanco(0.0, "1234", "5678");
		caixa.setDescricao("Minha descrição");

		caixaService.cadastro(caixa);

		assertEquals("Minha descrição", caixa.getDescricao());
	}

	@Test
	public void devePropagarExcecaoQuandoBuscaUsuarioFalha() {
		when(usuarios.buscaUsuario(USUARIO_LOGADO))
				.thenThrow(new RuntimeException("ERRO 0x8F: conexao com o banco perdida @@##"));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("ERRO 0x8F: conexao com o banco perdida @@##");

		caixaService.cadastro(criaCaixa(CaixaTipo.CAIXA, 10.0, "Caixa"));
	}

	@Test
	public void deveRemoverHifenDeAgenciaEContaDoBanco() {
		Caixa caixa = criaBanco(0.0, "1234-5", "98765-4");

		caixaService.cadastro(caixa);

		assertEquals("12345", caixa.getAgencia());
		assertEquals("987654", caixa.getConta());
	}

	// ------------------------------------------- cenários adicionais

	@Test
	public void naoDeveVerificarCaixaAbertoParaCofre() {
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));
		Caixa cofre = criaCaixa(CaixaTipo.COFRE, 10.0, "Cofre");
		cofre.setCodigo(2L);

		assertEquals(Long.valueOf(2L), caixaService.cadastro(cofre));
	}

	@Test
	public void naoDeveVerificarCaixaAbertoParaBanco() {
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));
		Caixa banco = criaBanco(10.0, "1234", "5678");
		banco.setCodigo(3L);

		assertEquals(Long.valueOf(3L), caixaService.cadastro(banco));
	}

	@Test
	public void naoDeveSalvarNemLancarQuandoJaExisteCaixaAberto() {
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));

		try {
			caixaService.cadastro(criaCaixa(CaixaTipo.CAIXA, 10.0, "Caixa"));
		} catch (RuntimeException e) {
			// esperado
		}

		verify(caixas, never()).save(any(Caixa.class));
		verify(lancamentos, never()).lancamento(any(CaixaLancamento.class));
	}

	@Test
	public void naoDeveSalvarQuandoValorAberturaNegativo() {
		try {
			caixaService.cadastro(criaCaixa(CaixaTipo.CAIXA, -0.01, "Caixa"));
		} catch (RuntimeException e) {
			// esperado
		}

		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void naoDeveLancarQuandoSaveFalha() {
		when(caixas.save(any(Caixa.class))).thenThrow(new RuntimeException("erro"));

		try {
			caixaService.cadastro(criaCaixa(CaixaTipo.CAIXA, 10.0, "Caixa"));
		} catch (RuntimeException e) {
			// esperado
		}

		verify(lancamentos, never()).lancamento(any(CaixaLancamento.class));
	}

	@Test
	public void deveAceitarValorAberturaZeroSemGerarLancamento() {
		Caixa caixa = criaCaixa(CaixaTipo.CAIXA, 0.0, "Caixa");
		caixa.setCodigo(1L);

		assertEquals(Long.valueOf(1L), caixaService.cadastro(caixa));

		assertEquals(Double.valueOf(0.0), caixa.getValor_total());
		verify(caixas, times(1)).save(caixa);
		verify(lancamentos, never()).lancamento(any(CaixaLancamento.class));
	}

	@Test
	public void deveGerarLancamentoParaMenorValorPositivo() {
		caixaService.cadastro(criaCaixa(CaixaTipo.CAIXA, 0.01, "Caixa"));

		verify(lancamentos, times(1)).lancamento(any(CaixaLancamento.class));
	}

	@Test
	public void deveSalvarCaixaUmaVezComUsuarioEDataPreenchidos() {
		Caixa caixa = criaCaixa(CaixaTipo.CAIXA, 50.0, "Caixa");

		caixaService.cadastro(caixa);

		verify(caixas, times(1)).save(caixa);
		assertSame(usuario, caixa.getUsuario());
		assertNotNull(caixa.getData_cadastro());
	}

	@Test
	public void deveGerarLancamentoDeSaldoInicialParaCaixa() {
		assertLancamentoAbertura(criaCaixa(CaixaTipo.CAIXA, 75.5, "Caixa"), "Abertura de caixa");
	}

	@Test
	public void deveGerarLancamentoDeSaldoInicialParaCofre() {
		assertLancamentoAbertura(criaCaixa(CaixaTipo.COFRE, 75.5, "Cofre"), "Abertura de cofre");
	}

	@Test
	public void deveGerarLancamentoDeSaldoInicialParaBanco() {
		assertLancamentoAbertura(criaBanco(75.5, "1234", "5678"), "Abertura de banco");
	}

	@Test
	public void deveRemoverTodosCaracteresNaoNumericosDeAgenciaEConta() {
		Caixa caixa = criaBanco(0.0, "12.34 -a5", " 98/76-x4 ");

		caixaService.cadastro(caixa);

		assertEquals("12345", caixa.getAgencia());
		assertEquals("98764", caixa.getConta());
	}

	@Test
	public void naoDeveAlterarAgenciaEContaQuandoTipoNaoForBanco() {
		Caixa caixa = criaCaixa(CaixaTipo.CAIXA, 0.0, "Caixa");
		caixa.setAgencia("12-3");
		caixa.setConta("45-6");

		caixaService.cadastro(caixa);

		assertEquals("12-3", caixa.getAgencia());
		assertEquals("45-6", caixa.getConta());
	}

	@Test
	public void deveLancarNullPointerQuandoTipoNulo() {
		thrown.expect(NullPointerException.class);

		caixaService.cadastro(criaCaixa(null, 10.0, "Caixa"));
	}

	@Test
	public void deveLancarNullPointerQuandoDescricaoNula() {
		thrown.expect(NullPointerException.class);

		caixaService.cadastro(criaCaixa(CaixaTipo.CAIXA, 10.0, null));
	}

	@Test
	public void deveLancarNullPointerQuandoBancoSemAgencia() {
		thrown.expect(NullPointerException.class);

		caixaService.cadastro(criaBanco(0.0, null, "5678"));
	}

	@Test
	public void deveLancarNullPointerQuandoBancoSemConta() {
		thrown.expect(NullPointerException.class);

		caixaService.cadastro(criaBanco(0.0, "1234", null));
	}

	private void assertLancamentoAbertura(Caixa caixa, String observacaoEsperada) {
		caixaService.cadastro(caixa);

		ArgumentCaptor<CaixaLancamento> captor = ArgumentCaptor.forClass(CaixaLancamento.class);
		verify(lancamentos, times(1)).lancamento(captor.capture());
		CaixaLancamento lancamento = captor.getValue();

		assertEquals(observacaoEsperada, lancamento.getObservacao());
		assertEquals(Double.valueOf(75.5), lancamento.getValor());
		assertEquals(TipoLancamento.SALDOINICIAL, lancamento.getTipo());
		assertEquals(EstiloLancamento.ENTRADA, lancamento.getEstilo());
		assertSame(usuario, lancamento.getUsuario());
	}

	// ------------------------------------------- fechaCaixa

	@Test
	public void fechaCaixaDeveInformarSenhaQuandoVazia() {
		assertEquals("Favor, informe a senha", caixaService.fechaCaixa(1L, ""));
	}

	@Test
	public void fechaCaixaDeveLancarNullPointerQuandoSenhaNula() {
		// senha.equals("") sem validação de null
		thrown.expect(NullPointerException.class);

		caixaService.fechaCaixa(1L, null);
	}

	@Test
	public void fechaCaixaDeveRetornarSenhaIncorretaQuandoNaoConfere() {
		usuario.setSenha("1234");

		assertEquals("Senha incorreta, favor verifique", caixaService.fechaCaixa(1L, "123"));
	}

	@Test
	public void fechaCaixaDeveLancarExcecaoQuandoCaixaJaFechado() {
		Caixa caixa = criaCaixaAberto(100.0);
		caixa.setData_fechamento(new Timestamp(System.currentTimeMillis()));
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Caixa já esta fechado");

		caixaService.fechaCaixa(1L, SENHA_PLANA);
	}

	@Test
	public void fechaCaixaDeveLancarExcecaoQuandoSaveFalha() {
		Caixa caixa = criaCaixaAberto(100.0);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));
		when(caixas.save(caixa)).thenThrow(new RuntimeException("falha genérica"));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Ocorreu um erro ao fechar o caixa, chame o suporte");

		caixaService.fechaCaixa(1L, SENHA_PLANA);
	}

	@Test
	public void fechaCaixaDeveZerarValorFechamentoQuandoValorTotalNulo() {
		Caixa caixa = criaCaixaAberto(null);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		String retorno = caixaService.fechaCaixa(1L, SENHA_PLANA);

		assertEquals("Caixa fechado com sucesso", retorno);
		assertEquals(Double.valueOf(0.0), caixa.getValor_fechamento());
	}

	@Test
	public void fechaCaixaDevePropagarExcecaoQuandoBuscaUsuarioFalha() {
		when(usuarios.buscaUsuario(USUARIO_LOGADO))
				.thenThrow(new RuntimeException("ERRO 0x8F: conexao com o banco perdida @@##"));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("ERRO 0x8F: conexao com o banco perdida @@##");

		caixaService.fechaCaixa(1L, SENHA_PLANA);
	}

	@Test
	public void fechaCaixaDeveFecharComSucessoQuandoSenhaConfere() {
		Caixa caixa = criaCaixaAberto(250.0);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		String retorno = caixaService.fechaCaixa(1L, SENHA_PLANA);

		assertEquals("Caixa fechado com sucesso", retorno);
		assertEquals(Double.valueOf(250.0), caixa.getValor_fechamento());
		assertNotNull(caixa.getData_fechamento());
		verify(caixas, times(1)).save(caixa);
	}

	// ------------------------------------------- fechaCaixa: cenários adicionais

	@Test
	public void fechaCaixaNaoDeveBuscarNemSalvarCaixaQuandoSenhaVazia() {
		caixaService.fechaCaixa(1L, "");

		verify(caixas, never()).findById(any());
		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void fechaCaixaNaoDeveBuscarNemSalvarCaixaQuandoSenhaIncorreta() {
		caixaService.fechaCaixa(1L, "senha_errada");

		verify(caixas, never()).findById(any());
		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void fechaCaixaNaoDeveSalvarQuandoCaixaJaFechado() {
		Caixa caixa = criaCaixaAberto(100.0);
		caixa.setData_fechamento(new Timestamp(System.currentTimeMillis()));
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		try {
			caixaService.fechaCaixa(1L, SENHA_PLANA);
		} catch (RuntimeException e) {
			// esperado
		}

		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void fechaCaixaDeveTratarSenhaApenasComEspacoComoIncorreta() {
		assertEquals("Senha incorreta, favor verifique", caixaService.fechaCaixa(1L, " "));
	}

	@Test
	public void fechaCaixaDeveDiferenciarSenhaComEspacoNoFinal() {
		assertEquals("Senha incorreta, favor verifique", caixaService.fechaCaixa(1L, SENHA_PLANA + " "));
	}

	@Test
	public void fechaCaixaDeveRetornarSenhaIncorretaQuandoUsuarioSemSenha() {
		usuario.setSenha(null);

		assertEquals("Senha incorreta, favor verifique", caixaService.fechaCaixa(1L, SENHA_PLANA));
	}

	@Test
	public void fechaCaixaDeveLancarNullPointerQuandoUsuarioNaoEncontrado() {
		when(usuarios.buscaUsuario(USUARIO_LOGADO)).thenReturn(null);

		thrown.expect(NullPointerException.class);

		caixaService.fechaCaixa(1L, SENHA_PLANA);
	}

	@Test
	public void fechaCaixaDeveLancarExcecaoQuandoCaixaNaoExiste() {
		when(caixas.findById(99L)).thenReturn(Optional.empty());

		thrown.expect(java.util.NoSuchElementException.class);

		caixaService.fechaCaixa(99L, SENHA_PLANA);
	}

	@Test
	public void fechaCaixaDevePropagarExcecaoQuandoBuscaDoCaixaFalha() {
		when(caixas.findById(1L)).thenThrow(new RuntimeException("erro ao consultar caixa"));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("erro ao consultar caixa");

		caixaService.fechaCaixa(1L, SENHA_PLANA);
	}

	@Test
	public void fechaCaixaDeveManterValorFechamentoZeroQuandoValorTotalZero() {
		Caixa caixa = criaCaixaAberto(0.0);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		assertEquals("Caixa fechado com sucesso", caixaService.fechaCaixa(1L, SENHA_PLANA));
		assertEquals(Double.valueOf(0.0), caixa.getValor_fechamento());
	}

	@Test
	public void fechaCaixaDeveCopiarValorTotalNegativoParaFechamento() {
		Caixa caixa = criaCaixaAberto(-30.5);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		caixaService.fechaCaixa(1L, SENHA_PLANA);

		assertEquals(Double.valueOf(-30.5), caixa.getValor_fechamento());
	}

	@Test
	public void fechaCaixaDeveRegistrarDataFechamentoNoMomentoDaChamada() {
		Caixa caixa = criaCaixaAberto(10.0);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));
		long antes = System.currentTimeMillis();

		caixaService.fechaCaixa(1L, SENHA_PLANA);

		long depois = System.currentTimeMillis();
		long fechamento = caixa.getData_fechamento().getTime();
		org.junit.Assert.assertTrue(fechamento >= antes && fechamento <= depois);
	}

	@Test
	public void fechaCaixaDeveSalvarOMesmoCaixaRecuperadoUmaUnicaVez() {
		Caixa caixa = criaCaixaAberto(10.0);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		caixaService.fechaCaixa(1L, SENHA_PLANA);

		verify(caixas, times(1)).findById(1L);
		verify(caixas, times(1)).save(caixa);
	}

	private Caixa criaCaixaAberto(Double valorTotal) {
		Caixa caixa = criaCaixa(CaixaTipo.CAIXA, 0.0, "Caixa");
		caixa.setValor_total(valorTotal);
		return caixa;
	}

	// ---------------------------------------------------------------- helpers

	private static void resetAplicacaoSingleton() throws Exception {
		Field instancia = Aplicacao.class.getDeclaredField("aplicacao");
		instancia.setAccessible(true);
		instancia.set(null, null);
	}

	private Usuario criaUsuario() {
		Usuario u = new Usuario();
		u.setCodigo(1L);
		u.setUser(USUARIO_LOGADO);
		u.setSenha(new BCryptPasswordEncoder().encode(SENHA_PLANA));
		return u;
	}

	private Caixa criaCaixa(CaixaTipo tipo, Double valorAbertura, String descricao) {
		Caixa caixa = new Caixa();
		caixa.setTipo(tipo);
		caixa.setValor_abertura(valorAbertura);
		caixa.setDescricao(descricao);
		return caixa;
	}

	private Caixa criaBanco(Double valorAbertura, String agencia, String conta) {
		Caixa caixa = criaCaixa(CaixaTipo.BANCO, valorAbertura, "");
		caixa.setAgencia(agencia);
		caixa.setConta(conta);
		return caixa;
	}
}
