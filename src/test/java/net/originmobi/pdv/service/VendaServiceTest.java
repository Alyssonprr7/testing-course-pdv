package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import net.originmobi.pdv.enumerado.TituloTipo;
import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.enumerado.EntradaSaida;
import net.originmobi.pdv.enumerado.VendaSituacao;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.PagamentoTipo;
import net.originmobi.pdv.model.Receber;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.model.Venda;
import net.originmobi.pdv.model.VendaProduto;
import net.originmobi.pdv.repository.VendaRepository;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;
import net.originmobi.pdv.singleton.Aplicacao;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    @InjectMocks
    private VendaService vendaService;

    @Mock
    private VendaRepository vendas;

    @Mock
    private UsuarioService usuarios;

    @Mock
    private VendaProdutoService vendaProdutos;

    @Mock
    private PagamentoTipoService formaPagamentos;

    @Mock
    private CaixaService caixas;

    @Mock
    private ReceberService receberServ;

    @Mock
    private CaixaLancamentoService lancamentos;

    @Mock
    private TituloService tituloService;

    @Mock
    private CartaoLancamentoService cartaoLancamento;

    @Mock
    private ProdutoService produtos;

    private Venda vendaMock;
    private PagamentoTipo pagamentoTipoMock;
    private Titulo tituloMock;


    @BeforeEach
    void setUp() {
        vendaMock = new Venda();
        pagamentoTipoMock = new PagamentoTipo();
        tituloMock = org.mockito.Mockito.mock(Titulo.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    }

    // =========================================================================
    // TESTES PARA O MÉTODO: abreVenda()
    // =========================================================================

    @Test
    void deveAbrirNovaVendaComSucesso() {
        // Mock do Singleton Aplicacao usando MockedStatic (requer Mockito 3.4.0+)
        try (MockedStatic<Aplicacao> aplicacaoMocked = mockStatic(Aplicacao.class)) {
            Aplicacao appMock = org.mockito.Mockito.mock(Aplicacao.class);
            aplicacaoMocked.when(Aplicacao::getInstancia).thenReturn(appMock);
            when(appMock.getUsuarioAtual()).thenReturn("admin");
            
            Usuario usuarioMock = new Usuario();
            when(usuarios.buscaUsuario("admin")).thenReturn(usuarioMock);

            Long resultado = vendaService.abreVenda(vendaMock);

            verify(vendas, times(1)).save(vendaMock);
            assertEquals(VendaSituacao.ABERTA, vendaMock.getSituacao());
            assertEquals(0.00, vendaMock.getValor_produtos());
            assertNotNull(vendaMock.getData_cadastro());
        }
    }

    @Test
    void deveAtualizarVendaExistente() {
        vendaMock.setCodigo(1L); // Venda já tem código, significa que já existe
        vendaMock.setObservacao("Teste de observação");
        
        Long resultado = vendaService.abreVenda(vendaMock);

        verify(vendas, times(1)).updateDadosVenda(vendaMock.getPessoa(), "Teste de observação", 1L);
        assertEquals(1L, resultado);
    }

    // =========================================================================
    // TESTES PARA O MÉTODO: addProduto() e removeProduto()
    // =========================================================================

    @Test
    void deveAdicionarProdutoEmVendaAberta() {
        when(vendas.verificaSituacao(1L)).thenReturn(VendaSituacao.ABERTA.toString());

        String resultado = vendaService.addProduto(1L, 2L, 10.5);

        verify(vendaProdutos, times(1)).salvar(any(VendaProduto.class));
        assertEquals("ok", resultado);
    }

    @Test
    void naoDeveAdicionarProdutoEmVendaFechada() {
        when(vendas.verificaSituacao(1L)).thenReturn(VendaSituacao.FECHADA.toString());

        String resultado = vendaService.addProduto(1L, 2L, 10.5);

        verify(vendaProdutos, times(0)).salvar(any(VendaProduto.class));
        assertEquals("Venda fechada", resultado);
    }

    @Test
    void deveRemoverProdutoDeVendaAberta() {
        vendaMock.setSituacao(VendaSituacao.ABERTA);
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);

        String resultado = vendaService.removeProduto(100L, 1L);

        verify(vendaProdutos, times(1)).removeProduto(100L);
        assertEquals("ok", resultado);
    }

    // =========================================================================
    // TESTES PARA O MÉTODO: fechaVenda() - Caminhos de Exceção
    // =========================================================================

    @Test
    void deveLancarExcecaoSeVendaFechada() {
        vendaMock.setSituacao(VendaSituacao.FECHADA);
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"100.0"}, new String[]{"1"})
        );

        assertEquals("venda fechada", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoSeCaixaFechadoNoPagamentoEmDinheiro() {
        vendaMock.setSituacao(VendaSituacao.ABERTA);
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        
        pagamentoTipoMock.setFormaPagamento("00"); // À vista
        when(formaPagamentos.busca(1L)).thenReturn(pagamentoTipoMock);
        
        when(tituloMock.getTipo().getSigla()).thenReturn(TituloTipo.DIN.toString());
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloMock));
        
        when(caixas.caixaIsAberto()).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"100.0"}, new String[]{"1"})
        );

        assertEquals("nenhum caixa aberto", exception.getMessage());
    }

    // =========================================================================
    // TESTES PARA O MÉTODO: fechaVenda() - Caminhos Felizes (Happy Paths)
    // =========================================================================

    @Test
    void deveFecharVendaComSucesso_AVista_Dinheiro() throws Exception {
        // Configuração Inicial
        vendaMock.setSituacao(VendaSituacao.ABERTA);
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        
        pagamentoTipoMock.setFormaPagamento("00");
        when(formaPagamentos.busca(1L)).thenReturn(pagamentoTipoMock);
        
        when(tituloMock.getTipo().getSigla()).thenReturn(TituloTipo.DIN.toString());
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloMock));
        
        when(caixas.caixaIsAberto()).thenReturn(true);
        when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));

        try (MockedStatic<Aplicacao> aplicacaoMocked = mockStatic(Aplicacao.class)) {
            Aplicacao appMock = org.mockito.Mockito.mock(Aplicacao.class);
            aplicacaoMocked.when(Aplicacao::getInstancia).thenReturn(appMock);
            when(appMock.getUsuarioAtual()).thenReturn("admin");
            when(usuarios.buscaUsuario("admin")).thenReturn(new Usuario());

            String resultado = vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"100.0"}, new String[]{"1"});

            verify(receberServ, times(1)).cadastrar(any(Receber.class));
            verify(lancamentos, times(1)).lancamento(any(CaixaLancamento.class)); // Lançamento no caixa
            verify(vendas, times(1)).fechaVenda(eq(1L), eq(VendaSituacao.FECHADA), eq(100.0), eq(0.0), eq(0.0), any(), eq(pagamentoTipoMock));
            verify(produtos, times(1)).movimentaEstoque(1L, EntradaSaida.SAIDA); // Confirma que retirou do stock
            
            assertEquals("Venda finalizada com sucesso", resultado);
        }
    }

    @Test
    void deveFecharVendaComSucesso_AVista_CartaoCredito() throws Exception {
        vendaMock.setSituacao(VendaSituacao.ABERTA);
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        
        pagamentoTipoMock.setFormaPagamento("00");
        when(formaPagamentos.busca(1L)).thenReturn(pagamentoTipoMock);
        
        when(tituloMock.getTipo().getSigla()).thenReturn(TituloTipo.CARTCRED.toString());
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloMock));

        String resultado = vendaService.fechaVenda(1L, 1L, 50.0, 0.0, 0.0, new String[]{"50.0"}, new String[]{"1"});

        verify(receberServ, times(1)).cadastrar(any(Receber.class));
        verify(cartaoLancamento, times(1)).lancamento(eq(50.0), eq(Optional.of(tituloMock))); // Confirma integração com maquininha
        verify(produtos, times(1)).movimentaEstoque(1L, EntradaSaida.SAIDA);
        
        assertEquals("Venda finalizada com sucesso", resultado);
    }
}