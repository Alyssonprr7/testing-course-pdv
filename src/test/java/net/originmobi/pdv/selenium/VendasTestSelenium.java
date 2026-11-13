package net.originmobi.pdv.selenium;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VendasTestSelenium {
	
	private static final String BASE_URL = "http://127.0.0.1:8081";

	private static WebDriver navegador;
	private static WebDriverWait wait;
	
	@BeforeAll
	static void setUp() {
		Assumptions.assumeTrue(applicationIsRunning(), "Aplicação não está rodando em " + BASE_URL);
		System.setProperty("webdriver.chrome.driver", "C:\\Drivers\\chromedriver-win64\\chromedriver.exe");
		ChromeOptions options = new ChromeOptions();
		if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
			options.addArguments("--headless=new");
		}
		navegador = new ChromeDriver(options);
		wait = new WebDriverWait(navegador, 10);

		//login("gerente", "123");
	}

	@AfterAll
	static void tearDown() {
		if (navegador != null)
			navegador.quit();
	}
	
	
	
	private static boolean applicationIsRunning() {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("127.0.0.1", 8081), 1000);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	@Test
	@Order(1)
	@DisplayName("Verificar abertura da página de login corretamente")
	public void testAberturaPagina() {
		navegador.get(BASE_URL + "/login");
		String nome = navegador.findElement(By.xpath("//*[@id=\"painelLogin\"]")).getText();
		assertTrue(nome.contains("Entrar no Sistema"));
	}
	
	@Test
	@Order(2)
	@DisplayName("Verificar login corretamente")
	public void testLogin() {
		navegador.get(BASE_URL);
		login("gerente", "123");
		String nome = navegador.findElement(By.xpath("/html/body/div[2]/div/form")).getText();
		assertTrue(nome.contains("Usuário: gerente"));
	}
	
	@Test
	@Order(3)
	@DisplayName("Selecionar o cliente da compra")
	public void testSelecaoDeCliente() {
		navegador.get(BASE_URL);
		navegador.findElement(By.xpath("/html/body/div[3]/div/div[2]/a")).click();
		navegador.findElement(By.xpath("//*[@id=\"grupoPainelCaixa\"]/div[3]/a")).click();
		
		navegador.findElement(By.xpath("//*[@id=\"cliente\"]")).click();
		
		WebElement opcaoCliente = wait.until(ExpectedConditions.visibilityOfElementLocated(
				By.xpath("//*[contains(text(), 'João Rafael Mendes Nogueira')]")
		));
		opcaoCliente.click();
		
		navegador.findElement(By.xpath("//*[@id=\"btn-salva\"]")).click();
		
		WebElement mensagemSucesso = wait.until(ExpectedConditions.visibilityOfElementLocated(
				By.xpath("//*[@id=\"formPrincipal\"]/div[1]/div/span")
		));
		
		assertTrue(mensagemSucesso.getText().contains("Pedido Salvo"));
		
		
	}
	
	@Test
	@Order(4)
	@DisplayName("Selecionar o produto")
	public void testSelecaoDeProduto() {

		WebElement botaoProduto = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//button[@title='O que procura?']")
		));
		botaoProduto.click();

		WebElement opcaoProduto = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//div[contains(@class, 'dropdown-menu')]//span[contains(normalize-space(), 'COD: 1 - Picolé')]")
		));
		opcaoProduto.click();

		navegador.findElement(By.xpath("//*[@id=\"js-url\"]")).click();
		
		// 4. Valida se o produto foi adicionado conferindo o valor total
		Boolean atualizouValor = wait.until(ExpectedConditions.textToBePresentInElementLocated(
				By.xpath("//*[@id=\"total_produtos\"]/h4"), "6,50"
		)); 
		
		assertTrue(atualizouValor, "O valor total não atualizou para 6,50 a tempo.");
	}
	
	@Test
	@Order(5)
	@DisplayName("Gerar a venda")
	public void testGerarVenda() {
		navegador.findElement(By.xpath("//*[@id=\"btn-venda\"]")).click();
		WebElement opcaoPagamento = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//*[@id=\"pagamento\"]")
		));
		opcaoPagamento.click();
		
		org.openqa.selenium.support.ui.Select comboPagamento = new org.openqa.selenium.support.ui.Select(opcaoPagamento);
		comboPagamento.selectByVisibleText("À Vista");
		
		WebElement pagar = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//*[@id=\"form_pagamento\"]/div[5]/a")
		));
		pagar.click();
		
		wait.until(ExpectedConditions.alertIsPresent());
		Alert alertaNativo = navegador.switchTo().alert();

		String mensagemAlerta = alertaNativo.getText();
		assertTrue(mensagemAlerta.contains("Venda finalizada com sucesso"), "A mensagem do alerta não é a esperada.");

		alertaNativo.accept();
		
	}
	
	
	
	private static void login(String user, String password) {
		navegador.get(BASE_URL + "/login");
		navegador.findElement(By.id("user")).sendKeys(user);
		navegador.findElement(By.id("password")).sendKeys(password);
		navegador.findElement(By.id("btn-login")).click();
		wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
	}

}
