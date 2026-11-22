package net.originmobi.pdv.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.WebDriverException;

/**
 * Testes de sistema (Selenium) da funcionalidade da Entrega 1: adicionar item
 * com tributação válida a uma nota fiscal de saída.
 *
 * Propriedades opcionais: -DbaseUrl, -Ddb.url, -Ddb.user, -Ddb.password,
 * -Dchrome.binary (caminho do navegador), -Dwebdriver.chrome.driver (caminho do
 * driver) e -Dheadless=true.
 */
class NotaFiscalItemSeleniumTest {

	private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
	private static final String DB_URL = System.getProperty("db.url",
			"jdbc:mysql://localhost:3306/pdv?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
	private static final String DB_USER = System.getProperty("db.user", "root");
	private static final String DB_PASSWORD = System.getProperty("db.password", "123456");

	private static final String NATUREZA = "SELENIUM NFI venda";
	private static final String DESTINATARIO = "9101";
	private static final String PRODUTO_VALIDO = "9101";
	private static final String PRODUTO_SO_ENTRADA = "9102";
	private static final String PRODUTO_OUTRA_UF = "9103";

	private static WebDriver driver;
	private static WebDriverWait wait;

	@BeforeAll
	static void setUp() throws Exception {
		Assumptions.assumeTrue(applicationIsRunning(), "Aplicação não está rodando em " + BASE_URL);

        driver = criaDriver();
		wait = new WebDriverWait(driver, 10);

        abreComEspera(BASE_URL + "/login");
		driver.findElement(By.id("user")).sendKeys("gerente");
		driver.findElement(By.id("password")).sendKeys("123");
		driver.findElement(By.id("btn-login")).click();
		wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
	}

	@AfterAll
	static void tearDown() throws Exception {
		if (driver == null)
			return;
		executaScript("/sql/notafiscalitem-limpeza.sql");
		driver.quit();
	}

    	// Selenium 3: Edge usa EdgeDriver (-Dwebdriver.edge.driver); Chrome usa ChromeDriver (-Dwebdriver.chrome.driver)
	private static WebDriver criaDriver() {
		boolean headless = Boolean.getBoolean("headless");
		String binario = System.getProperty("chrome.binary");

        String driverChrome = System.getProperty("webdriver.chrome.driver");
		if (System.getProperty("webdriver.edge.driver") == null && driverChrome != null
				&& driverChrome.toLowerCase().contains("msedgedriver"))
			System.setProperty("webdriver.edge.driver", driverChrome);

		if (System.getProperty("webdriver.edge.driver") != null) {
			Map<String, Object> opcoesEdge = new HashMap<>();
			if (binario != null && !binario.isEmpty())
				opcoesEdge.put("binary", binario);
			if (headless)
				opcoesEdge.put("args", Arrays.asList("--headless", "--disable-gpu"));

			EdgeOptions edge = new EdgeOptions();
			if (!opcoesEdge.isEmpty())
				edge.setCapability("ms:edgeOptions", opcoesEdge);
			return new EdgeDriver(edge);
		}

		ChromeOptions options = new ChromeOptions();
		options.setHeadless(headless);
		if (binario != null && !binario.isEmpty())
			options.setBinary(binario);
		return new ChromeDriver(options);
	}

    	// a aplicação pode estar reiniciando (devtools); tenta de novo por até 60 segundos
	private static void abreComEspera(String url) throws InterruptedException {
		long limite = System.currentTimeMillis() + 60000;
		while (true) {
			try {
				driver.get(url);
				return;
			} catch (WebDriverException e) {
				boolean recusada = String.valueOf(e.getMessage()).contains("ERR_CONNECTION_REFUSED");
				if (!recusada || System.currentTimeMillis() > limite)
					throw e;
				Thread.sleep(1000);
			}
		}
	}

	@BeforeEach
	void preparaMassa() throws Exception {
		executaScript("/sql/notafiscalitem-limpeza.sql");
		executaScript("/sql/notafiscalitem-massa.sql");
	}

	// ST-NFI-01
	@Test
	void deveAdicionarItemComTributacaoValidaEmNotaDeSaida() throws Exception {
		long nota = criaNotaDeSaida();

		abreAbaProdutos();
		insereProduto(PRODUTO_VALIDO, "3");

		String qtd = "select qtd from nota_fiscal_item where nota_fiscal_codigo = " + nota;
		esperaNumero(qtd, 3);

		assertEquals(1, consultaNumero("select count(*) from nota_fiscal_item where nota_fiscal_codigo = " + nota));
		assertEquals(30.0, consultaDouble("select vl_total from nota_fiscal_item where nota_fiscal_codigo = " + nota), 0.001);
		assertEquals("5102", consultaTexto("select cfop from nota_fiscal_item where nota_fiscal_codigo = " + nota));
		assertEquals(5.4, consultaDouble("select i.v_icms from nota_fiscal_item_imposto i join nota_fiscal_item it "
				+ "on it.imposto_codigo = i.codigo where it.nota_fiscal_codigo = " + nota), 0.001);
		assertEquals(30.0, consultaDouble("select t.v_prod from nota_fiscal_totais t join nota_fiscal n "
				+ "on n.totais_codigo = t.codigo where n.codigo = " + nota), 0.001);

		recarregaNotaEAbreAbaProdutos(nota);
		assertEquals(1, driver.findElements(By.cssSelector("#nf-produtos tbody tr")).size());
		String tabela = driver.findElement(By.id("nf-produtos")).getText();
		assertTrue(tabela.contains("Produto Selenium NFI A"), tabela);
		assertTrue(tabela.contains("5102"), tabela);
		assertTrue(tabela.contains("R$ 10,00"), tabela);
		assertTrue(tabela.contains("R$ 30,00"), tabela);
		assertTrue(tabela.contains("R$ 5,40"), tabela);
	}

	// ST-NFI-02
	@Test
	void deveSomarQuantidadeQuandoOMesmoProdutoEhInseridoDeNovo() throws Exception {
		long nota = criaNotaDeSaida();
		String qtd = "select qtd from nota_fiscal_item where nota_fiscal_codigo = " + nota;

		abreAbaProdutos();
		insereProduto(PRODUTO_VALIDO, "2");
		esperaNumero(qtd, 2);

		recarregaNotaEAbreAbaProdutos(nota);
		insereProduto(PRODUTO_VALIDO, "3");
		esperaNumero(qtd, 5);

		assertEquals(1, consultaNumero("select count(*) from nota_fiscal_item where nota_fiscal_codigo = " + nota));
		assertEquals(50.0, consultaDouble("select vl_total from nota_fiscal_item where nota_fiscal_codigo = " + nota), 0.001);
	}

	// ST-NFI-03
	@Test
	void deveRecusarProdutoCujaTributacaoSoTemRegraDeEntrada() throws Exception {
		long nota = criaNotaDeSaida();

		abreAbaProdutos();
		insereProduto(PRODUTO_SO_ENTRADA, "1");

		assertEquals("Tributação sem regra de saída, verifique", aceitaAlerta());
		assertEquals(0, consultaNumero("select count(*) from nota_fiscal_item where nota_fiscal_codigo = " + nota));
	}

	// ST-NFI-04
	@Test
	void deveRecusarProdutoSemRegraParaAUfDoDestinatario() throws Exception {
		long nota = criaNotaDeSaida();

		abreAbaProdutos();
		insereProduto(PRODUTO_OUTRA_UF, "1");

		assertEquals("Nenhuma regra de tributação cadastrada para a UF do destinatário", aceitaAlerta());
		assertEquals(0, consultaNumero("select count(*) from nota_fiscal_item where nota_fiscal_codigo = " + nota));
	}

	// ------------------------------------------------------------------
	// apoio: telas
	// ------------------------------------------------------------------

	private long criaNotaDeSaida() {
		driver.get(BASE_URL + "/notafiscal/form");
		driver.findElement(By.name("natureza_operacao")).sendKeys(NATUREZA);
		new Select(driver.findElement(By.id("tipo"))).selectByValue("SAIDA");
		defineValorPorJs(By.cssSelector("select[name='destinatario']"), DESTINATARIO);

		wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-cria-nota"))).click();
		wait.until(ExpectedConditions.urlMatches(".*/notafiscal/\\d+$"));

		String url = driver.getCurrentUrl();
		return Long.parseLong(url.substring(url.lastIndexOf('/') + 1));
	}

	private void abreAbaProdutos() {
		driver.findElement(By.cssSelector("a[href='#produtos']")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".btn-addnota-produto")));
	}

	private void recarregaNotaEAbreAbaProdutos(long nota) {
		driver.get(BASE_URL + "/notafiscal/" + nota);
		abreAbaProdutos();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("nf-produtos")));
	}

	private void insereProduto(String codigoProduto, String quantidade) {
		defineValorPorJs(By.id("codigoProduto"), codigoProduto);
		driver.findElement(By.cssSelector(".btn-addnota-produto")).click();

		Alert prompt = wait.until(ExpectedConditions.alertIsPresent());
		prompt.sendKeys(quantidade);
		prompt.accept();
	}

	private String aceitaAlerta() {
		Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
		String texto = alerta.getText();
		alerta.accept();
		return texto;
	}

	// os selects usam o componente selectpicker, que esconde o <select> original
	private void defineValorPorJs(By localizador, String valor) {
		WebElement elemento = driver.findElement(localizador);
		((JavascriptExecutor) driver).executeScript(
				"arguments[0].value = arguments[1]; if (window.jQuery) { window.jQuery(arguments[0]).trigger('change'); }",
				elemento, valor);
		assertEquals(valor, elemento.getAttribute("value"));
	}

	// ------------------------------------------------------------------
	// apoio: banco
	// ------------------------------------------------------------------

	private static Connection conecta() throws SQLException {
		return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
	}

	private static long consultaNumero(String sql) throws SQLException {
		try (Connection c = conecta(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
			return rs.next() ? rs.getLong(1) : -1;
		}
	}

	private static double consultaDouble(String sql) throws SQLException {
		try (Connection c = conecta(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
			return rs.next() ? rs.getDouble(1) : -1;
		}
	}

	private static String consultaTexto(String sql) throws SQLException {
		try (Connection c = conecta(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
			return rs.next() ? rs.getString(1) : null;
		}
	}

	private static void esperaNumero(String sql, long esperado) throws Exception {
		long limite = System.currentTimeMillis() + 10000;
		long atual = -1;
		while (System.currentTimeMillis() < limite) {
			atual = consultaNumero(sql);
			if (atual == esperado)
				return;
			Thread.sleep(200);
		}
		assertEquals(esperado, atual);
	}

	private static void executaScript(String recurso) throws IOException, SQLException {
		String script;
		try (InputStream entrada = NotaFiscalItemSeleniumTest.class.getResourceAsStream(recurso);
				BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
			script = leitor.lines().filter(linha -> !linha.trim().startsWith("--")).collect(Collectors.joining("\n"));
		}
		try (Connection c = conecta(); Statement s = c.createStatement()) {
			for (String instrucao : script.split(";"))
				if (!instrucao.trim().isEmpty())
					s.execute(instrucao);
		}
	}

	private static boolean applicationIsRunning() {
		try {
			URI uri = URI.create(BASE_URL);
			int porta = uri.getPort() == -1 ? 80 : uri.getPort();
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(uri.getHost(), porta), 1000);
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}
}