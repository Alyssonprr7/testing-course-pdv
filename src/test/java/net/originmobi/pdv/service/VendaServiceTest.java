package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.enumerado.TituloTipo;
import net.originmobi.pdv.model.PagamentoTipo;
import net.originmobi.pdv.model.Receber;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.Venda;
import net.originmobi.pdv.repository.VendaRepository;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    @InjectMocks
    private VendaService vendaService;

    @Mock
    private VendaRepository vendas;

    @Mock
    private PagamentoTipoService formaPagamentos;

    @Mock
    private CaixaService caixas;

    @Mock
    private ReceberService receberServ;

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
        vendaMock = mock(Venda.class);
        pagamentoTipoMock = mock(PagamentoTipo.class);
        tituloMock = mock(Titulo.class, RETURNS_DEEP_STUBS);
    }

    // 1. Venda já fechada
    @Test
    void testFechaVenda_VendaJaFechada() {
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        when(vendaMock.isAberta()).thenReturn(false); // Venda fechada

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"100.0"}, new String[]{"1"})
        );

        assertEquals("venda fechada", exception.getMessage());
    }

    // 2. Valor de produtos <= 0
    @Test
    void testFechaVenda_ValorProdutosMenorOuIgualZero() {
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        when(vendaMock.isAberta()).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            vendaService.fechaVenda(1L, 1L, 00.0, 00.0, 0.0, new String[]{"0.0"}, new String[]{"1"})
        );

        assertEquals("Venda sem valor, verifique", exception.getMessage());
    }

    // 3. Venda à vista em dinheiro sem caixa aberto
    @Test
    void testFechaVenda_AVistaDinheiro_SemCaixaAberto() {
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        when(vendaMock.isAberta()).thenReturn(true);
        
        when(pagamentoTipoMock.getFormaPagamento()).thenReturn("00"); // 00 = à vista
        when(formaPagamentos.busca(1L)).thenReturn(pagamentoTipoMock);
        
        when(tituloMock.getTipo().getSigla()).thenReturn(TituloTipo.DIN.toString()); // DIN = Dinheiro
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloMock));
        
        when(caixas.caixaIsAberto()).thenReturn(false); // Caixa fechado

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"100.0"}, new String[]{"1"})
        );

        assertEquals("nenhum caixa aberto", exception.getMessage());
    }

    // 4. Venda à vista no cartão
    @Test
    void testFechaVenda_AVista_CartaoCredito() throws Exception {
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        when(vendaMock.isAberta()).thenReturn(true);
        
        when(pagamentoTipoMock.getFormaPagamento()).thenReturn("00"); // 00 = à vista
        when(formaPagamentos.busca(1L)).thenReturn(pagamentoTipoMock);
        
        // Simula cartão de crédito
        when(tituloMock.getTipo().getSigla()).thenReturn(TituloTipo.CARTCRED.toString()); 
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloMock));

        // Executa o método
        String resultado = vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"100.0"}, new String[]{"1"});

        // Verifica se o lançamento no cartão foi chamado
        verify(cartaoLancamento).lancamento(eq(100.0), eq(Optional.of(tituloMock)));
        assertEquals("Venda finalizada com sucesso", resultado);
    }

    // 5. Venda a prazo sem cliente cadastrado
    @Test
    void testFechaVenda_APrazo_SemCliente() {
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        when(vendaMock.isAberta()).thenReturn(true);
        
        // Retorna null para simular falta de cliente
        when(vendaMock.getPessoa()).thenReturn(null); 
        
        // Diferente de "00" simula venda a prazo (ex: "30")
        when(pagamentoTipoMock.getFormaPagamento()).thenReturn("30"); 
        when(formaPagamentos.busca(1L)).thenReturn(pagamentoTipoMock);
        
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloMock));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"100.0"}, new String[]{"1"})
        );

        assertEquals("Venda sem cliente, verifique", exception.getMessage());
    }

    // 6. Soma das parcelas diferente do valor total (em dinheiro à vista)
    @Test
    void testFechaVenda_SomaParcelasDiferenteValorTotal() {
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        when(vendaMock.isAberta()).thenReturn(true);
        
        when(pagamentoTipoMock.getFormaPagamento()).thenReturn("00"); // 00 = à vista
        when(formaPagamentos.busca(1L)).thenReturn(pagamentoTipoMock);
        
        when(tituloMock.getTipo().getSigla()).thenReturn(TituloTipo.DIN.toString()); // DIN = Dinheiro
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloMock));
        
        when(caixas.caixaIsAberto()).thenReturn(true); // Caixa precisa estar aberto para validar valores

        // Valor dos produtos = 100.0, mas a parcela informada é = 50.0
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"50.0"}, new String[]{"1"})
        );

        assertEquals("Valor das parcelas diferente do valor total de produtos, verifique", exception.getMessage());
    }
}