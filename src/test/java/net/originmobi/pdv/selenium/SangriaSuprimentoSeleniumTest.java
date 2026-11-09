package net.originmobi.pdv.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Testes de sistema da funcionalidade Sangria / Suprimento de caixa
 * (CaixaLancamentoService.lancamento pela interface web).
 *
 * Todo teste abre um caixa do tipo CAIXA com saldo de R$ 100,00 e o fecha no
 * final. Na tela, a sangria é o botão "Retirada".
 */
class SangriaSuprimentoSeleniumTest {

	private static final String BASE_URL = "http://localhost:8080";
	private static final String SUCCESS = "Lançamento realizado com sucesso";
	private static final String INSUFFICIENT_BALANCE = "Saldo insuficiente para realizar esta operação";
	private static final By ROWS = By.cssSelector("#tabLancamentos tbody tr");

	private static WebDriver driver;
	private static WebDriverWait wait;

	@BeforeAll
	static void setUp() {
		Assumptions.assumeTrue(applicationIsRunning(), "Aplicação não está rodando em " + BASE_URL);

		ChromeOptions options = new ChromeOptions();
		options.setHeadless(Boolean.getBoolean("headless"));
		driver = new ChromeDriver(options);
		wait = new WebDriverWait(driver, 10);

		login("gerente", "123");
	}

	@AfterAll
	static void tearDown() {
		if (driver != null)
			driver.quit();
	}

	// Pré-condição comum: caixa aberto com R$ 100,00 (o saldo inicial já é a 1ª linha do extrato)
	@BeforeEach
	void openCashRegister() {
		driver.get(BASE_URL + "/caixa");
		driver.findElement(By.linkText("Abrir Novo")).click();

		wait.until(ExpectedConditions.urlContains("/caixa/form"));
		driver.findElement(By.id("descricao")).sendKeys("Caixa Selenium Sangria/Suprimento");
		new Select(driver.findElement(By.id("caixatipo"))).selectByValue("CAIXA");
		driver.findElement(By.id("valorAbertura")).sendKeys("10000");
		driver.findElement(By.cssSelector(".btn-abrir-caixa")).click();

		wait.until(ExpectedConditions.urlContains("/caixa/gerenciar/"));
		wait.until(ExpectedConditions.numberOfElementsToBe(ROWS, 1));
	}

	// Só pode existir um caixa do tipo CAIXA aberto, então o caixa do teste é fechado aqui.
	// Se o teste falhou no meio, pode ter sobrado um alerta ou modal aberto: o alerta é aceito e
	// a tela recarregada antes de fechar, para a falha não se repetir nos testes seguintes.
	@AfterEach
	void closeIfOpen() {
		acceptAlertIfPresent();
		String manageUrl = driver.getCurrentUrl();
		if (!manageUrl.contains("/caixa/gerenciar/"))
			return;

		driver.get(manageUrl);
		if (driver.findElements(By.id("statusFechado")).isEmpty())
			assertEquals("Caixa fechado com sucesso", tryClose("123"));
	}

	// Automatiza o CT-BRU-MAN-002
	@Test
	@DisplayName("CT-BRU-SEL-001 - suprimento com caixa aberto")
	void shouldSupplyCash() {
		assertEquals(SUCCESS, supply("3000", "troco"));

		waitForRows(2);
		assertEquals("130,00", balance("valorTotal"));
		assertRow(lastRow(), "troco", "R$ 30,00", "ENTRADA");
	}

	// Automatiza o CT-BRU-MAN-001
	@Test
	@DisplayName("CT-BRU-SEL-002 - sangria com saldo suficiente")
	void shouldWithdrawCash() {
		assertEquals(SUCCESS, withdraw("5000", "deposito banco"));

		waitForRows(2);
		assertEquals("50,00", balance("valorTotal"));
		assertRow(lastRow(), "deposito banco", "R$ -50,00", "SAIDA");
	}

	// Automatiza o CT-BRU-MAN-003
	@Test
	@DisplayName("CT-BRU-SEL-003 - sangria maior que o saldo é recusada")
	void shouldNotWithdrawMoreThanBalance() {
		assertEquals(INSUFFICIENT_BALANCE, withdraw("50000", "sangria alta"));

		waitForModalToClose(".modalSangria");
		assertEquals(1, driver.findElements(ROWS).size(), "nenhum lançamento deveria ser gravado");
		assertEquals("100,00", balance("valorTotal"));
	}

	// Automatiza o CT-BRU-MAN-004: o extrato e os totais refletem todos os lançamentos
	@Test
	@DisplayName("CT-BRU-SEL-004 - extrato e saldos após suprimento e sangria")
	void shouldShowStatementAndTotals() {
		assertEquals(SUCCESS, supply("3000", "troco"));
		waitForRows(2);
		assertEquals(SUCCESS, withdraw("5000", "deposito banco"));
		waitForRows(3);

		assertEquals("130,00", balance("valorEntrada"));
		assertEquals("50,00", balance("valorSaida"));
		assertEquals("80,00", balance("valorTotal"));
	}

	// Classes de equivalência da observação: vazia recebe o texto padrão de cada tipo
	@Test
	@DisplayName("CT-BRU-SEL-005 - observação vazia recebe o texto padrão")
	void shouldUseDefaultObservationWhenEmpty() {
		assertEquals(SUCCESS, supply("3000", ""));
		waitForRows(2);
		assertEquals(SUCCESS, withdraw("1000", ""));
		waitForRows(3);

		List<WebElement> rows = driver.findElements(ROWS);
		assertEquals("Suprimento de caixa", cell(rows.get(1), 2));
		assertEquals("Sangria de caixa", cell(rows.get(2), 2));
	}

	// Classe inválida do valor: a tela descarta o sinal, então "-50" vira uma sangria de 50.
	// Por isso o defeito do valor negativo (issue #13) só é alcançável chamando o endpoint direto.
	@Test
	@DisplayName("CT-BRU-SEL-006 - valor negativo digitado perde o sinal")
	void shouldIgnoreMinusSignTypedInValue() {
		assertEquals(SUCCESS, withdraw("-5000", "negativo"));

		waitForRows(2);
		assertEquals("50,00", balance("valorTotal"));
		assertRow(lastRow(), "negativo", "R$ -50,00", "SAIDA");
	}

	// Valor limite do suprimento (0,00 / 0,01).
	// DEFEITO: a máscara do campo apaga o valor zero quando o campo perde o foco. O servidor
	// recebe o valor vazio, responde HTTP 500 (NumberFormatException) e a tela mostra o texto
	// técnico "empty String".
	@Test
	@DisplayName("CT-BRU-SEL-007 - suprimento de 0,00 mostra mensagem técnica")
	void shouldShowTechnicalMessageWhenSupplyingZero() {
		driver.findElement(By.id("btnSuprimento")).click();
		WebElement value = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("idvalor")));
		value.sendKeys("000");
		driver.findElement(By.id("idObs")).sendKeys("zero");
		assertEquals("", value.getAttribute("value"), "a máscara deveria apagar o zero ao perder o foco");

		driver.findElement(By.cssSelector(".btn-suprimento-caixa")).click();
		assertEquals("empty String", acceptAlert());

		waitForModalToClose(".modalSuprimento");
		assertEquals(1, driver.findElements(ROWS).size(), "nenhum lançamento deveria ser gravado");
		assertEquals("100,00", balance("valorTotal"));
	}

	@Test
	@DisplayName("CT-BRU-SEL-008 - suprimento de 0,01 é aceito")
	void shouldSupplyOneCent() {
		assertEquals(SUCCESS, supply("1", "um centavo"));

		waitForRows(2);
		assertEquals("100,01", balance("valorTotal"));
		assertRow(lastRow(), "um centavo", "R$ 0,01", "ENTRADA");
	}

	// O service não bloqueia lançamento em caixa fechado (issue #14); quem bloqueia é a tela
	@Test
	@DisplayName("CT-BRU-SEL-009 - caixa fechado desabilita suprimento e retirada")
	void shouldDisableButtonsWhenClosed() {
		assertEquals("Caixa fechado com sucesso", tryClose("123"));
		driver.navigate().refresh();

		assertFalse(driver.findElements(By.id("statusFechado")).isEmpty(), "caixa deveria estar fechado");
		assertFalse(driver.findElement(By.id("btnSuprimento")).isEnabled(), "suprimento deveria estar desabilitado");
		assertFalse(driver.findElement(By.id("btnSangria")).isEnabled(), "retirada deveria estar desabilitada");
	}

	private static String supply(String typedValue, String observation) {
		driver.findElement(By.id("btnSuprimento")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("idvalor"))).sendKeys(typedValue);
		driver.findElement(By.id("idObs")).sendKeys(observation);
		driver.findElement(By.cssSelector(".btn-suprimento-caixa")).click();
		String text = acceptAlert();
		waitForModalToClose(".modalSuprimento");
		return text;
	}

	private static String withdraw(String typedValue, String observation) {
		driver.findElement(By.id("btnSangria")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("idvl"))).sendKeys(typedValue);
		driver.findElement(By.id("idobs")).sendKeys(observation);
		driver.findElement(By.cssSelector(".btn-Sangria-Sangria")).click();
		String text = acceptAlert();
		waitForModalToClose(".modalSangria");
		return text;
	}

	private static void waitForModalToClose(String modal) {
		wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(modal)));
	}

	// Depois do lançamento a tela recarrega o extrato e os saldos por AJAX
	private static void waitForRows(int count) {
		wait.until(ExpectedConditions.numberOfElementsToBe(ROWS, count));
	}

	// O label e o input dos saldos têm o mesmo id, por isso o seletor inclui a tag
	private static String balance(String id) {
		return driver.findElement(By.cssSelector("input#" + id)).getAttribute("value");
	}

	private static WebElement lastRow() {
		List<WebElement> rows = driver.findElements(ROWS);
		return rows.get(rows.size() - 1);
	}

	private static String cell(WebElement row, int column) {
		return row.findElement(By.cssSelector("td:nth-child(" + column + ")")).getText();
	}

	private static void assertRow(WebElement row, String observation, String value, String style) {
		assertEquals(observation, cell(row, 2));
		assertEquals(value, cell(row, 3));
		assertEquals(style, cell(row, 4));
	}

	private static String acceptAlert() {
		Alert alert = wait.until(ExpectedConditions.alertIsPresent());
		String text = alert.getText();
		alert.accept();
		return text;
	}

	private static void acceptAlertIfPresent() {
		try {
			driver.switchTo().alert().accept();
		} catch (NoAlertPresentException e) {
			// nenhum alerta pendente
		}
	}

	private static void login(String user, String password) {
		driver.get(BASE_URL + "/login");
		driver.findElement(By.id("user")).sendKeys(user);
		driver.findElement(By.id("password")).sendKeys(password);
		driver.findElement(By.id("btn-login")).click();
		wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
	}

	private static String tryClose(String password) {
		wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("admsenha")));
		driver.findElement(By.id("btnfechacaixa")).click();

		WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("admsenha")));
		passwordInput.clear();
		passwordInput.sendKeys(password);
		driver.findElement(By.cssSelector(".btn-fechar-caixa")).click();
		return acceptAlert();
	}

	private static boolean applicationIsRunning() {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("localhost", 8080), 1000);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
