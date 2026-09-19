package net.originmobi.pdv.service;

import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.model.Caixa;
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
	public void scaffoldDeveInicializarService() {
		org.junit.Assert.assertNotNull(caixaService);
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
