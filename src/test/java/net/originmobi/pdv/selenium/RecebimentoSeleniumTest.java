package net.originmobi.pdv.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Testes de sistema de "Receber parcela". Exigem a aplicação e o banco no ar, e o resultado é
 * conferido também no banco, que é uma fonte independente da tela.
 *
 * Propriedades: baseUrl, db.url, chrome.binary (opcional, para navegadores como o Brave) e headless.
 */
class RecebimentoSeleniumTest {

	private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
	private static final String DB_URL = System.getProperty("db.url",
			"jdbc:mysql://localhost:3306/pdv?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
	private static final String DB_USER = "root";
	private static final String DB_PASSWORD = "123456";

	private static final String TITULO = "Dinheiro Massa";
	private static final String SUCESSO = "Recebimento realizado com sucesso";

	private static WebDriver driver;
	private static WebDriverWait wait;
	private static String caixaAbertoPeloTeste;

	@BeforeAll
	static void setUp() throws SQLException {
		Assumptions.assumeTrue(applicationIsRunning(), "Aplicação não está rodando em " + BASE_URL);

		ChromeOptions options = new ChromeOptions();
		options.setHeadless(Boolean.getBoolean("headless"));
		if (System.getProperty("chrome.binary") != null)
			options.setBinary(System.getProperty("chrome.binary"));
		driver = new ChromeDriver(options);
		wait = new WebDriverWait(driver, 10);

		login("gerente", "123");

		if (consultaNumero("select count(*) from caixa where tipo = 'CAIXA' and data_fechamento is null") == 0)
			abreCaixa();
	}

	@AfterAll
	static void tearDown() {
		if (driver == null)
			return;

		if (caixaAbertoPeloTeste != null)
			fechaCaixa();
		driver.quit();
	}

	@BeforeEach
	void recriaMassa() throws Exception {
		aplicaMassa();
	}

	// RC-01: valor igual ao total quita a parcela e entra no caixa
	@Test
	void deveQuitarParcelaQuandoValorIgualAoTotal() throws SQLException {
		abreRecebimento(1, 9001);

		assertEquals(SUCESSO, recebe(TITULO, "10000"));

		assertEquals(0.0, restanteDaParcela(9001), 0.0);
		assertEquals(1, consultaNumero("select quitado from parcela where codigo = 9001"));
		assertEquals(100.0, consultaNumeroDouble("select valor from caixa_lancamento where recebimento_codigo = "
				+ recebimentoAtual() + " and tipo = 'RECEBIMENTO' and estilo = 'ENTRADA'"), 0.0);
	}

	// RC-02: valor parcial mantém a parcela aberta com o saldo restante
	@Test
	void deveManterParcelaAbertaQuandoValorParcial() throws SQLException {
		abreRecebimento(1, 9002);

		assertEquals(SUCESSO, recebe(TITULO, "4000"));

		assertEquals(60.0, restanteDaParcela(9002), 0.0);
		assertEquals(0, consultaNumero("select quitado from parcela where codigo = 9002"));
	}

	// O valor é distribuído na ordem das parcelas selecionadas
	@Test
	void deveDistribuirValorEntreAsParcelasSelecionadas() throws SQLException {
		abreRecebimento(1, 9003, 9004);
		assertEquals("100,00", driver.findElement(By.id("total")).getAttribute("value"));

		assertEquals(SUCESSO, recebe(TITULO, "8000"));

		assertEquals(0.0, restanteDaParcela(9003), 0.0);
		assertEquals(20.0, restanteDaParcela(9004), 0.0);
	}

	// Valor limite: um centavo acima do total
	@Test
	void deveRecusarValorUmCentavoAcimaDoTotal() throws SQLException {
		abreRecebimento(1, 9006);

		assertEquals("Valor de recebimento é superior aos títulos", recebe(TITULO, "10001"));

		assertEquals(100.0, restanteDaParcela(9006), 0.0);
	}

	// Valor limite: zero não é um recebimento válido
	@Test
	void deveRecusarValorZero() throws SQLException {
		abreRecebimento(1, 9006);

		assertEquals("Valor de recebimento inválido", recebe(TITULO, ""));

		assertEquals(100.0, restanteDaParcela(9006), 0.0);
	}

	@Test
	void deveExigirTitulo() throws SQLException {
		abreRecebimento(1, 9006);

		driver.findElement(By.id("vlrecebido")).sendKeys("10000");
		driver.findElement(By.cssSelector(".btn-receber-titulo")).click();

		assertEquals("Selecione um título para realizar o recebimento", aceitaAlerta());
		assertEquals(100.0, restanteDaParcela(9006), 0.0);
	}

	// RC-05: o recebimento fechado não pode ser processado de novo
	@Test
	void deveRecusarRecebimentoJaProcessado() throws SQLException {
		abreRecebimento(1, 9007);
		assertEquals(SUCESSO, recebe(TITULO, "10000"));

		driver.navigate().refresh();

		// a tela desabilita os campos do recebimento fechado, mas o botão continua disponível
		driver.findElement(By.cssSelector(".btn-receber-titulo")).click();

		assertEquals("Recebimento já esta fechado", aceitaAlerta());
		assertEquals(0.0, restanteDaParcela(9007), 0.0);
	}

	// RC-05: a parcela quitada não pode ser selecionada na lista
	@Test
	void naoDeveOferecerSelecaoParaParcelaQuitada() {
		driver.get(BASE_URL + "/receber?codigo=1&pago=false");

		assertTrue(driver.findElements(By.cssSelector("input.marcado[value='9005']")).isEmpty(),
				"parcela quitada não deveria ter checkbox");
		assertFalse(driver.findElements(By.cssSelector("input.marcado[value='9006']")).isEmpty(),
				"parcela aberta deveria ter checkbox");
	}

	@Test
	void deveRemoverRecebimentoAoCancelar() throws SQLException {
		abreRecebimento(1, 9008);
		String recebimento = recebimentoAtual();

		driver.findElement(By.cssSelector(".btn-cancelar-receber")).click();
		wait.until(ExpectedConditions.alertIsPresent()).accept();

		wait.until(d -> {
			try {
				return consultaNumero("select count(*) from recebimento where codigo = " + recebimento) == 0;
			} catch (SQLException e) {
				return false;
			}
		});
		assertEquals(100.0, restanteDaParcela(9008), 0.0);
	}

	// Cliente com código acima de 127: a comparação de código precisa ser por valor
	@Test
	void deveAbrirRecebimentoDeClienteComCodigoAcimaDe127() throws SQLException {
		abreRecebimento(9001, 9011);

		assertEquals(SUCESSO, recebe(TITULO, "10000"));

		assertEquals(0.0, restanteDaParcela(9011), 0.0);
	}

	private static void abreRecebimento(int cliente, int... parcelas) {
		driver.get(BASE_URL + "/receber?pago=false&codigo=" + cliente);
		for (int parcela : parcelas)
			driver.findElement(By.cssSelector("input.marcado[value='" + parcela + "']")).click();

		driver.findElement(By.cssSelector(".btn-receber-varios")).click();
		wait.until(ExpectedConditions.urlMatches("/recebimento/\\d+$"));
	}

	private static String recebe(String titulo, String valorDigitado) {
		new Select(driver.findElement(By.id("titulo"))).selectByVisibleText(titulo);
		driver.findElement(By.id("vlrecebido")).sendKeys(valorDigitado);
		driver.findElement(By.cssSelector(".btn-receber-titulo")).click();
		return aceitaAlerta();
	}

	private static String recebimentoAtual() {
		String url = driver.getCurrentUrl();
		return url.substring(url.lastIndexOf('/') + 1);
	}

	private static String aceitaAlerta() {
		Alert alert = wait.until(ExpectedConditions.alertIsPresent());
		String texto = alert.getText();
		alert.accept();
		return texto;
	}

	private static void login(String usuario, String senha) {
		driver.get(BASE_URL + "/login");
		driver.findElement(By.id("user")).sendKeys(usuario);
		driver.findElement(By.id("password")).sendKeys(senha);
		driver.findElement(By.id("btn-login")).click();
		wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
	}

	private static void abreCaixa() {
		driver.get(BASE_URL + "/caixa");
		driver.findElement(By.linkText("Abrir Novo")).click();

		wait.until(ExpectedConditions.urlContains("/caixa/form"));
		driver.findElement(By.id("descricao")).sendKeys("Caixa Selenium Recebimento");
		new Select(driver.findElement(By.id("caixatipo"))).selectByValue("CAIXA");
		driver.findElement(By.id("valorAbertura")).sendKeys("10000");
		driver.findElement(By.cssSelector(".btn-abrir-caixa")).click();

		wait.until(ExpectedConditions.urlContains("/caixa/gerenciar/"));
		caixaAbertoPeloTeste = driver.getCurrentUrl();
	}

	private static void fechaCaixa() {
		driver.get(caixaAbertoPeloTeste);
		driver.findElement(By.id("btnfechacaixa")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("admsenha"))).sendKeys("123");
		driver.findElement(By.cssSelector(".btn-fechar-caixa")).click();
		assertEquals("Caixa fechado com sucesso", aceitaAlerta());
	}

	private static double restanteDaParcela(int parcela) throws SQLException {
		return consultaNumeroDouble("select valor_restante from parcela where codigo = " + parcela);
	}

	private static double consultaNumeroDouble(String sql) throws SQLException {
		try (Connection conexao = conecta();
				Statement comando = conexao.createStatement();
				ResultSet resultado = comando.executeQuery(sql)) {
			resultado.next();
			return resultado.getDouble(1);
		}
	}

	private static int consultaNumero(String sql) throws SQLException {
		return (int) consultaNumeroDouble(sql);
	}

	private static void aplicaMassa() throws IOException, SQLException {
		String script;
		try (InputStream entrada = RecebimentoSeleniumTest.class.getResourceAsStream("/sql/recebimento-massa.sql");
				BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
			script = leitor.lines().filter(linha -> !linha.startsWith("--")).collect(Collectors.joining("\n"));
		}

		try (Connection conexao = conecta(); Statement comando = conexao.createStatement()) {
			for (String instrucao : script.split(";"))
				if (!instrucao.trim().isEmpty())
					comando.execute(instrucao);
		}
	}

	private static Connection conecta() throws SQLException {
		return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
	}

	private static boolean applicationIsRunning() {
		URI uri = URI.create(BASE_URL);
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()), 1000);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
