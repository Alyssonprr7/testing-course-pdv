package net.originmobi.pdv.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

class CaixaSeleniumTest {

	private static final String BASE_URL = "http://localhost:8080";

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

	// Só pode existir um caixa do tipo CAIXA aberto, então todo caixa aberto por um teste é fechado aqui
	@AfterEach
	void closeIfOpen() {
		if (driver.getCurrentUrl().contains("/caixa/gerenciar/") && driver.findElements(By.id("statusFechado")).isEmpty())
			close("123");
	}

	@Test
	void shouldOpenWithOpeningValue() {
		open("10000");

		wait.until(ExpectedConditions.urlContains("/caixa/gerenciar/"));
		assertEquals("Gerenciar Caixa", driver.findElement(By.cssSelector("h2.titulo-h1")).getText());
		assertTrue(driver.findElements(By.id("statusFechado")).isEmpty(), "caixa deveria estar aberto");
	}

	@Test
	void shouldOpenWithZeroValue() {
		open("000"); // R$ 0,00

		wait.until(ExpectedConditions.urlContains("/caixa/gerenciar/"));
		assertTrue(driver.findElements(By.id("statusFechado")).isEmpty(), "caixa deveria estar aberto");
		assertEquals("0,00", totalBalance());
	}

	@Test
	void shouldNotOpenWithNegativeValue() {
		open("-1000"); // R$ -10,00

		Alert alert = wait.until(ExpectedConditions.alertIsPresent());
		assertEquals("Valor informado é inválido", alert.getText());
		alert.accept();
		assertFalse(driver.getCurrentUrl().contains("/caixa/gerenciar/"), "caixa não deveria ter sido aberto");
	}

	@Test
	void shouldOpenWithVeryHighValue() {
		open("200000000000000000"); // R$ 2.000.000.000.000.000,00

		wait.until(ExpectedConditions.urlContains("/caixa/gerenciar/"));
		assertTrue(driver.findElements(By.id("statusFechado")).isEmpty(), "caixa deveria estar aberto");
		assertEquals("2.000.000.000.000.000,00", totalBalance());
	}

	// Particionamento + valor limite: o limite do valor de abertura é 0 (negativo é inválido)
	@Test
	void shouldNotOpenWithValueJustBelowZero() {
		open("-1"); // R$ -0,01

		Alert alert = wait.until(ExpectedConditions.alertIsPresent());
		assertEquals("Valor informado é inválido", alert.getText());
		alert.accept();
	}

	@Test
	void shouldOpenWithValueJustAboveZero() {
		open("1"); // R$ 0,01

		wait.until(ExpectedConditions.urlContains("/caixa/gerenciar/"));
		assertEquals("0,01", totalBalance());
	}

	// Classe "observação vazia": o sistema usa a descrição padrão
	@Test
	void shouldUseDefaultDescriptionWhenEmpty() {
		open("", "10000");
		wait.until(ExpectedConditions.urlContains("/caixa/gerenciar/"));

		assertEquals("Caixa diário", descriptionInList());
	}

	// Valor limite da observação: máximo de 250 caracteres (@Size no model)
	@Test
	void shouldOpenWithDescriptionAtMaxLength() {
		open(repeat("a", 250), "10000");

		wait.until(ExpectedConditions.urlContains("/caixa/gerenciar/"));
		assertEquals(repeat("a", 250), descriptionInList());
	}

	@Test
	void shouldNotOpenWithDescriptionAboveMaxLength() {
		open(repeat("a", 251), "10000");

		Alert alert = wait.until(ExpectedConditions.alertIsPresent());
		assertEquals("Erro no processo de abertura, chame o suporte técnico", alert.getText());
		alert.accept();
	}

	// Valor limite da sangria: o máximo permitido é o saldo do caixa (R$ 100,00)
	@Test
	void shouldWithdrawExactlyTheBalance() {
		open("10000");
		wait.until(ExpectedConditions.urlContains("/caixa/gerenciar/"));

		assertEquals("Lançamento realizado com sucesso", withdraw("10000"));
	}

	@Test
	void shouldNotWithdrawMoreThanTheBalance() {
		open("10000");
		wait.until(ExpectedConditions.urlContains("/caixa/gerenciar/"));

		assertEquals("Saldo insuficiente para realizar esta operação", withdraw("10001"));
	}

	// Classes da senha de fechamento: vazia, incorreta (a correta é usada no @AfterEach)
	@Test
	void shouldNotCloseWithEmptyOrWrongPassword() {
		open("10000");
		wait.until(ExpectedConditions.urlContains("/caixa/gerenciar/"));

		assertEquals("Favor, informe a senha", tryClose(""));
		assertEquals("Senha incorreta, favor verifique", tryClose("1234"));
		assertTrue(driver.findElements(By.id("statusFechado")).isEmpty(), "caixa deveria continuar aberto");
	}

	private static void open(String typedValue) {
		open("Caixa Selenium", typedValue);
	}

	private static void open(String description, String typedValue) {
		driver.get(BASE_URL + "/caixa");
		driver.findElement(By.linkText("Abrir Novo")).click();

		wait.until(ExpectedConditions.urlContains("/caixa/form"));
		driver.findElement(By.id("descricao")).sendKeys(description);
		new Select(driver.findElement(By.id("caixatipo"))).selectByValue("CAIXA");
		driver.findElement(By.id("valorAbertura")).sendKeys(typedValue);
		driver.findElement(By.cssSelector(".btn-abrir-caixa")).click();
	}

	private static String totalBalance() {
		return driver.findElement(By.cssSelector("input#valorTotal")).getAttribute("value");
	}

	// A observação não aparece na tela de gerenciar, só na listagem; volta para o caixa depois de ler
	private static String descriptionInList() {
		String manageUrl = driver.getCurrentUrl();
		String code = manageUrl.substring(manageUrl.lastIndexOf('/') + 1);

		driver.get(BASE_URL + "/caixa");
		String description = driver.findElement(By.xpath("//tr[td[1]='" + code + "']/td[2]")).getText();
		driver.get(manageUrl);
		return description;
	}

	private static String withdraw(String typedValue) {
		driver.findElement(By.id("btnSangria")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("idvl"))).sendKeys(typedValue);
		driver.findElement(By.cssSelector(".btn-Sangria-Sangria")).click();
		return acceptAlert();
	}

	private static String acceptAlert() {
		Alert alert = wait.until(ExpectedConditions.alertIsPresent());
		String text = alert.getText();
		alert.accept();
		return text;
	}

	private static String repeat(String s, int times) {
		return new String(new char[times]).replace("\0", s);
	}

	private static void login(String user, String password) {
		driver.get(BASE_URL + "/login");
		driver.findElement(By.id("user")).sendKeys(user);
		driver.findElement(By.id("password")).sendKeys(password);
		driver.findElement(By.id("btn-login")).click();
		wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
	}

	private static void close(String password) {
		assertEquals("Caixa fechado com sucesso", tryClose(password));
	}

	private static String tryClose(String password) {
		// espera o modal de uma tentativa anterior terminar de fechar
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
