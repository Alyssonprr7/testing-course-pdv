package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import net.originmobi.pdv.enumerado.EntradaSaida;
import net.originmobi.pdv.enumerado.notafiscal.NotaFiscalTipo;
import net.originmobi.pdv.enumerado.produto.ProdutoSubstTributaria;
import net.originmobi.pdv.model.CFOP;
import net.originmobi.pdv.model.Cidade;
import net.originmobi.pdv.model.CstCsosn;
import net.originmobi.pdv.model.Endereco;
import net.originmobi.pdv.model.Estado;
import net.originmobi.pdv.model.ModBcIcms;
import net.originmobi.pdv.model.NotaFiscal;
import net.originmobi.pdv.model.NotaFiscalItem;
import net.originmobi.pdv.model.NotaFiscalItemImposto;
import net.originmobi.pdv.model.NotaFiscalTotais;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Produto;
import net.originmobi.pdv.model.Tributacao;
import net.originmobi.pdv.model.TributacaoRegra;
import net.originmobi.pdv.repository.notafiscal.NotaFiscalItemRepository;
import net.originmobi.pdv.service.ProdutoService;
import net.originmobi.pdv.service.notafiscal.NotaFiscalItemImpostoService;
import net.originmobi.pdv.service.notafiscal.NotaFiscalItemService;
import net.originmobi.pdv.service.notafiscal.NotaFiscalService;
import net.originmobi.pdv.service.notafiscal.NotaFiscalTotaisServer;

/**
 * Testes unitários de NotaFiscalItemService (insere e verificaRegraDeTributacao).
 *
 * verificaRegraDeTributacao é privado, então é exercitado por meio de insere.
 * As entidades (Produto, Tributacao, NotaFiscal...) são montadas como objetos
 * reais; só as dependências do serviço são mocks. Assim as cadeias de Optional
 * do código rodam de verdade e o Mockito fica restrito a cinco colaboradores.
 *
 * Os identificadores UT-NFI-xx correspondem à tabela de casos de teste.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class NotaFiscalItemServiceTest {

	public NotaFiscalItemServiceTest() {
		super();
	}

	private static final Long COD_PROD = 1L;
	private static final Long COD_NOTA = 10L;
	private static final Double VALOR_VENDA = 10.0;
	private static final String UF_DESTINO = "RJ";
	private static final String CFOP_SAIDA = "5102";
	private static final String CFOP_ENTRADA = "1102";

	@Mock
	private NotaFiscalItemRepository itemServer;

	@Mock
	private NotaFiscalItemImpostoService impostos;

	@Mock
	private NotaFiscalTotaisServer totais;

	@Mock
	private ProdutoService produtos;

	@Mock
	private NotaFiscalService notas;

	@InjectMocks
	private NotaFiscalItemService service;

	private Produto produto;
	private NotaFiscal nota;
	private TributacaoRegra regraSaida;

	@Before
	public void setUp() {
		// cenário padrão válido: produto completo, nota de saída para o RJ e
		// uma regra de saída para o RJ
		regraSaida = criaRegra(UF_DESTINO, EntradaSaida.SAIDA, CFOP_SAIDA);
		produto = criaProdutoValido(regraSaida);
		nota = criaNota(NotaFiscalTipo.SAIDA, UF_DESTINO);

		when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.of(produto));
		when(notas.busca(COD_NOTA)).thenReturn(Optional.of(nota));
		when(impostos.calcula(any(), any(), any(), anyChar(), anyInt())).thenReturn(new NotaFiscalItemImposto());
	}

	// ------------------------------------------------------------------
	// verificaRegraDeTributacao (via insere)
	// ------------------------------------------------------------------

	// UT-NFI-01
	@Test
	public void deveLancarExcecaoQuandoProdutoNaoExiste() {
		when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.empty());

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Nenhum produto encontrado, favor verifique", erro.getMessage());
		verificaNadaPersistido();
	}

	// UT-NFI-02
	@Test
	public void deveLancarExcecaoQuandoProdutoSemTributacao() {
		produto.setTributacao(null);

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Produto sem tributação, favor verifique", erro.getMessage());
		verificaNadaPersistido();
	}

	// UT-NFI-03
	@Test
	public void deveLancarExcecaoQuandoProdutoSemNcm() {
		produto.setNcm("");

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Produto sem código NCM, favor verifique", erro.getMessage());
		verificaNadaPersistido();
	}

	// UT-NFI-04
	@Test
	public void deveLancarExcecaoQuandoSubstituicaoTributariaSemCest() {
		produto.setSubtributaria(ProdutoSubstTributaria.SIM);
		produto.setCest("");

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Produto de substituição tributária sem código CEST, favor verifique", erro.getMessage());
		verificaNadaPersistido();
	}

	// UT-NFI-05
	@Test
	public void deveLancarExcecaoQuandoProdutoSemUnidade() {
		produto.setUnidade("");

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Produto sem unidade, favor verifique", erro.getMessage());
		verificaNadaPersistido();
	}

	// UT-NFI-06
	@Test
	public void deveLancarExcecaoQuandoTributacaoSemRegraDeSaida() {
		produto.getTributacao().setRegra(Arrays.asList(criaRegra(UF_DESTINO, EntradaSaida.ENTRADA, CFOP_ENTRADA)));

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Tributação sem regra de saída, verifique", erro.getMessage());
		verificaNadaPersistido();
	}

	// UT-NFI-07
	@Test
	public void deveLancarExcecaoQuandoTributacaoSemRegraDeEntrada() {
		// a tributação padrão só tem regra de saída
		nota.setTipo(NotaFiscalTipo.ENTRADA);

		RuntimeException erro = insereComErro(NotaFiscalTipo.ENTRADA);

		assertEquals("Tributação sem regra de entrada, verifique", erro.getMessage());
		verificaNadaPersistido();
	}

	// UT-NFI-08
	@Test
	public void deveLancarExcecaoQuandoTributacaoNaoTemNenhumaRegra() {
		produto.getTributacao().setRegra(Collections.<TributacaoRegra>emptyList());

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Tributação sem regra de saída, verifique", erro.getMessage());
		verificaNadaPersistido();
	}

	// UT-NFI-09: a ordem das validações é respeitada (tributação vem antes do NCM)
	@Test
	public void deveInformarFaltaDeTributacaoAntesDaFaltaDeNcm() {
		produto.setTributacao(null);
		produto.setNcm("");

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Produto sem tributação, favor verifique", erro.getMessage());
	}

	// UT-NFI-10: CEST só é exigido quando o produto é de substituição tributária
	@Test
	public void deveIgnorarCestQuandoProdutoNaoEhSubstituicaoTributaria() {
		produto.setSubtributaria(ProdutoSubstTributaria.NAO);
		produto.setCest("");

		String retorno = service.insere(COD_PROD, COD_NOTA, 1, NotaFiscalTipo.SAIDA);

		assertEquals("ok", retorno);
	}

	// UT-NFI-11
	@Test
	public void deveAceitarProdutoDeSubstituicaoTributariaComCest() {
		produto.setSubtributaria(ProdutoSubstTributaria.SIM);
		produto.setCest("1234567");

		String retorno = service.insere(COD_PROD, COD_NOTA, 1, NotaFiscalTipo.SAIDA);

		assertEquals("ok", retorno);
	}

	// ------------------------------------------------------------------
	// Casos que revelam DEFEITO SUSPEITO (esperado: falham até a correção)
	//
	// verificaRegraDeTributacao usa produto.map(Produto::getNcm).get().isEmpty().
	// Quando o campo é NULL (caso dos produtos da carga inicial do banco), o
	// map devolve Optional vazio e o get() lança NoSuchElementException
	// ("No value present"), em vez da mensagem de negócio prevista.
	// O mesmo vale para unidade e para CEST de produto de substituição
	// tributária. Registrar em issue (módulo NotaFiscal).
	// ------------------------------------------------------------------

	// UT-NFI-12
	@Test
	public void deveInformarNcmAusenteQuandoNcmNulo() {
		produto.setNcm(null);

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Produto sem código NCM, favor verifique", erro.getMessage());
	}

	// UT-NFI-13
	@Test
	public void deveInformarUnidadeAusenteQuandoUnidadeNula() {
		produto.setUnidade(null);

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Produto sem unidade, favor verifique", erro.getMessage());
	}

	// UT-NFI-14
	@Test
	public void deveInformarCestAusenteQuandoSubstituicaoTributariaComCestNulo() {
		produto.setSubtributaria(ProdutoSubstTributaria.SIM);
		produto.setCest(null);

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Produto de substituição tributária sem código CEST, favor verifique", erro.getMessage());
	}

	// ------------------------------------------------------------------
	// insere: escolha da regra
	// ------------------------------------------------------------------

	// UT-NFI-15
	@Test
	public void deveLancarExcecaoQuandoNaoHaRegraParaUfDoDestinatario() {
		// o produto só tem regra de saída para o RJ e o destinatário é de SP
		trocaNota(criaNota(NotaFiscalTipo.SAIDA, "SP"));

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Nenhuma regra de tributação cadastrada para a UF do destinatário", erro.getMessage());
		verificaNadaPersistido();
	}

	// UT-NFI-16: existe regra de saída (passa na validação), mas não para a UF
	// do destinatário; a regra da UF do destinatário é de outro tipo
	@Test
	public void deveLancarExcecaoQuandoRegraDaUfTemTipoDiferenteDoDaNota() {
		produto.getTributacao().setRegra(Arrays.asList(criaRegra(UF_DESTINO, EntradaSaida.ENTRADA, CFOP_ENTRADA),
				criaRegra("SP", EntradaSaida.SAIDA, "6102")));

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Nenhuma regra de tributação cadastrada para a UF do destinatário", erro.getMessage());
		verificaNadaPersistido();
	}

	// UT-NFI-17
	@Test
	public void deveUsarARegraQueCombinaUfETipoDaNota() {
		TributacaoRegra entradaRj = criaRegra(UF_DESTINO, EntradaSaida.ENTRADA, CFOP_ENTRADA);
		TributacaoRegra saidaSp = criaRegra("SP", EntradaSaida.SAIDA, "6102");
		produto.getTributacao().setRegra(Arrays.asList(entradaRj, saidaSp, regraSaida));

		service.insere(COD_PROD, COD_NOTA, 1, NotaFiscalTipo.SAIDA);

		assertEquals(CFOP_SAIDA, capturaItemSalvo().getCfop());
		verify(impostos).calcula(any(), any(), same(regraSaida), anyChar(), anyInt());
	}

	// UT-NFI-18
	@Test
	public void deveUsarRegraDeEntradaEmNotaDeEntrada() {
		TributacaoRegra regraEntrada = criaRegra(UF_DESTINO, EntradaSaida.ENTRADA, CFOP_ENTRADA);
		produto.getTributacao().setRegra(Arrays.asList(regraSaida, regraEntrada));
		trocaNota(criaNota(NotaFiscalTipo.ENTRADA, UF_DESTINO));

		String retorno = service.insere(COD_PROD, COD_NOTA, 1, NotaFiscalTipo.ENTRADA);

		assertEquals("ok", retorno);
		assertEquals(CFOP_ENTRADA, capturaItemSalvo().getCfop());
		verify(impostos).calcula(any(), any(), same(regraEntrada), anyChar(), anyInt());
	}

	// ------------------------------------------------------------------
	// insere: item novo, item repetido e efeitos
	// ------------------------------------------------------------------

	// UT-NFI-19
	@Test
	public void deveInserirItemNovoComValoresCalculados() {
		String retorno = service.insere(COD_PROD, COD_NOTA, 2, NotaFiscalTipo.SAIDA);

		assertEquals("ok", retorno);

		NotaFiscalItem item = capturaItemSalvo();
		assertEquals(COD_PROD, item.getCodProd());
		assertEquals(2, item.getQtd());
		assertEquals(Double.valueOf(20.0), item.getVlTotal());
		assertEquals(VALOR_VENDA, item.getV_uniTribu());
		assertEquals("UN", item.getUnidade_tribu());
		assertEquals(CFOP_SAIDA, item.getCfop());
		assertNull("item novo não deve ter código (é uma inclusão)", item.getCodigo());
		assertSame(nota, item.getNotaFiscal());

		// sem item anterior, não há imposto a atualizar
		verify(impostos).calcula(isNull(), eq(20.0), same(regraSaida), anyChar(), anyInt());
	}

	// UT-NFI-20
	@Test
	public void deveSomarQuantidadeQuandoProdutoJaEstaNaNota() {
		nota.getItens().add(criaItemExistente(COD_PROD, 2, 5L, 7L));

		service.insere(COD_PROD, COD_NOTA, 3, NotaFiscalTipo.SAIDA);

		NotaFiscalItem item = capturaItemSalvo();
		assertEquals(5, item.getQtd());
		assertEquals(Double.valueOf(50.0), item.getVlTotal());
		// reaproveita o código do item existente, o que transforma o save em atualização
		assertEquals(Long.valueOf(5L), item.getCodigo());
		// e o código do imposto existente, para atualizar em vez de criar outro
		verify(impostos).calcula(eq(7L), eq(50.0), same(regraSaida), anyChar(), anyInt());
	}

	// UT-NFI-21
	@Test
	public void naoDeveSomarQuantidadeDeOutroProdutoDaNota() {
		nota.getItens().add(criaItemExistente(99L, 4, 5L, 7L));

		service.insere(COD_PROD, COD_NOTA, 3, NotaFiscalTipo.SAIDA);

		NotaFiscalItem item = capturaItemSalvo();
		assertEquals(3, item.getQtd());
		assertEquals(Double.valueOf(30.0), item.getVlTotal());
		assertNull(item.getCodigo());
		verify(impostos).calcula(isNull(), eq(30.0), same(regraSaida), anyChar(), anyInt());
	}

	// UT-NFI-22
	@Test
	public void deveAtualizarTotaisDaNotaAposSalvarItem() {
		service.insere(COD_PROD, COD_NOTA, 1, NotaFiscalTipo.SAIDA);

		verify(totais).atualiza(eq(COD_NOTA), same(nota.getTotais()));
	}

	// UT-NFI-23
	@Test
	public void deveLancarExcecaoENaoAtualizarTotaisQuandoSaveDoItemFalha() {
		when(itemServer.save(any(NotaFiscalItem.class))).thenThrow(new RuntimeException("falha qualquer no save"));

		RuntimeException erro = insereComErro(NotaFiscalTipo.SAIDA);

		assertEquals("Erro ao salvar item na nota, chame o suporte", erro.getMessage());
		verify(totais, never()).atualiza(any(), any());
	}

	// ------------------------------------------------------------------
	// apoio
	// ------------------------------------------------------------------

	private RuntimeException insereComErro(NotaFiscalTipo tipo) {
		try {
			service.insere(COD_PROD, COD_NOTA, 1, tipo);
		} catch (RuntimeException e) {
			return e;
		}
		fail("Era esperada uma RuntimeException");
		return null;
	}

	private void verificaNadaPersistido() {
		verify(itemServer, never()).save(any(NotaFiscalItem.class));
		verify(impostos, never()).calcula(any(), any(), any(), anyChar(), anyInt());
		verify(totais, never()).atualiza(any(), any());
	}

	private NotaFiscalItem capturaItemSalvo() {
		ArgumentCaptor<NotaFiscalItem> captor = ArgumentCaptor.forClass(NotaFiscalItem.class);
		verify(itemServer).save(captor.capture());
		return captor.getValue();
	}

	private void trocaNota(NotaFiscal novaNota) {
		nota = novaNota;
		when(notas.busca(COD_NOTA)).thenReturn(Optional.of(nota));
	}

	private Produto criaProdutoValido(TributacaoRegra regra) {
		Tributacao tributacao = new Tributacao();
		tributacao.setRegra(new ArrayList<TributacaoRegra>(Arrays.asList(regra)));

		ModBcIcms modBcIcms = new ModBcIcms();
		modBcIcms.setTipo(3);

		Produto p = new Produto();
		p.setCodigo(COD_PROD);
		p.setDescricao("Produto de teste");
		p.setValor_venda(VALOR_VENDA);
		p.setUnidade("UN");
		p.setNcm("12345678");
		p.setSubtributaria(ProdutoSubstTributaria.NAO);
		p.setModBcIcms(modBcIcms);
		p.setTributacao(tributacao);
		return p;
	}

	private TributacaoRegra criaRegra(String uf, EntradaSaida tipo, String cfop) {
		Estado estado = new Estado();
		estado.setSigla(uf);

		CFOP cfopRegra = new CFOP();
		cfopRegra.setCfop(cfop);

		CstCsosn cstCsosn = new CstCsosn();
		cstCsosn.setCst_csosn("102");

		TributacaoRegra regra = new TributacaoRegra();
		regra.setUf(estado);
		regra.setTipo(tipo);
		regra.setCfop(cfopRegra);
		regra.setCst_csosn(cstCsosn);
		return regra;
	}

	private NotaFiscal criaNota(NotaFiscalTipo tipo, String ufDestinatario) {
		Estado estado = new Estado();
		estado.setSigla(ufDestinatario);

		Cidade cidade = new Cidade();
		cidade.setEstado(estado);

		Endereco endereco = new Endereco();
		endereco.setCidade(cidade);

		Pessoa destinatario = new Pessoa();
		destinatario.setEndereco(endereco);

		NotaFiscal n = new NotaFiscal();
		n.setCodigo(COD_NOTA);
		n.setTipo(tipo);
		n.setDestinatario(destinatario);
		n.setItens(new ArrayList<NotaFiscalItem>());
		n.setTotais(new NotaFiscalTotais());
		return n;
	}

	private NotaFiscalItem criaItemExistente(Long codProd, int qtd, Long codItem, Long codImposto) {
		NotaFiscalItemImposto imposto = new NotaFiscalItemImposto();
		imposto.setCodigo(codImposto);

		NotaFiscalItem item = new NotaFiscalItem();
		item.setCodigo(codItem);
		item.setCodProd(codProd);
		item.setQtd(qtd);
		item.setImpostos(imposto);
		return item;
	}
}
