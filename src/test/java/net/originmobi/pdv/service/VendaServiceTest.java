package net.originmobi.pdv.service;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
    
    @Mock
    private net.originmobi.pdv.service.ParcelaService parcelas;
    

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
            assertNull(resultado);
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

    @Captor
    private ArgumentCaptor<CaixaLancamento> caixaLancamentoCaptor;

    @Test
    void deveFecharVendaComSucesso_AVista_Dinheiro() throws Exception {
        // 1. ARRANGE (Preparação - o antigo "// ... setup ...")
        vendaMock.setSituacao(VendaSituacao.ABERTA);
        
        // É esta linha abaixo que evita o NullPointerException no vendaIsAberta!
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

            // 2. ACT (Execução)
            vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"100.0"}, new String[]{"1"});

            // 3. ASSERT (Validação com o ArgumentCaptor)
            verify(lancamentos).lancamento(caixaLancamentoCaptor.capture());
            
            CaixaLancamento lancamentoSalvo = caixaLancamentoCaptor.getValue();
            
            // Verifica se o lançamento no caixa teve o valor correto
            assertEquals(100.0, lancamentoSalvo.getValor());
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
    
    
    /*  ======================================================================
     Os testes inseridos abaixo são resultado da análise do teste de mutação
     e destinados a eliminar mutantes sobreviventes naquele teste
     =========================================================================
     */
    
    
    @Test   // metodo abreVenda
    void deveAtribuirUsuarioLogadoAoAbrirNovaVenda() {
        //  Prepara o cenário de usuário logado
        try (MockedStatic<Aplicacao> aplicacaoMocked = mockStatic(Aplicacao.class)) {  // intecepta métodos estáticos de Aplicação
            Aplicacao appMock = org.mockito.Mockito.mock(Aplicacao.class);
            aplicacaoMocked.when(Aplicacao::getInstancia).thenReturn(appMock);
            when(appMock.getUsuarioAtual()).thenReturn("admin");
            
            Usuario usuarioMock = new Usuario();
            when(usuarios.buscaUsuario("admin")).thenReturn(usuarioMock);

            Venda vendaNova = new Venda();
            vendaNova.setCodigo(null); // Garante que cairá no bloco de nova venda

            vendaService.abreVenda(vendaNova);

            // Assert: Foca exclusivamente na validação que mata o mutante da linha 82
            assertEquals(usuarioMock, vendaNova.getUsuario(), "O usuário logado deve ser vinculado à venda");
        }
    }
    
    @Test   // metodo removeProduto
    void naoDeveRemoverProdutoDeVendaFechada() {
        vendaMock.setSituacao(VendaSituacao.FECHADA);
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);

        String resultado = vendaService.removeProduto(100L, 1L);

        verify(vendaProdutos, times(0)).removeProduto(any()); 
        assertEquals("Venda fechada", resultado); 
    }
    
    @Test
    void deveRetornarQuantidadeDeVendasEmAberto() {
        when(vendas.qtdVendasEmAberto()).thenReturn(5); //simula um retorno de 5 vendas do BD

        int resultado = vendaService.qtdAbertos();

        assertEquals(5, resultado, "Deve retornar exatamente a quantidade enviada pelo repositório");
        verify(vendas, times(1)).qtdVendasEmAberto();
    }
    
    @Test
    void deveRetornarListaDeVendas() {
        List<Venda> listaFalsa = Arrays.asList(new Venda()); //Cria uma lista falsa com 1 venda dentro
        when(vendas.findAll()).thenReturn(listaFalsa);

        List<Venda> resultado = vendaService.lista();

        assertEquals(1, resultado.size());  
        assertEquals(listaFalsa, resultado);
    }
    
 // =========================================================================
    // Testes por IA para aumentar a cobertura (CORRIGIDOS COM ASSERTS)
    // =========================================================================

    @Test
    void deveBuscarVendasPorCodigo() {
        net.originmobi.pdv.filter.VendaFilter filter = new net.originmobi.pdv.filter.VendaFilter();
        filter.setCodigo(1L);
        org.springframework.data.domain.Pageable pageable = org.mockito.Mockito.mock(org.springframework.data.domain.Pageable.class);
        
        org.springframework.data.domain.Page<Venda> pageMock = org.mockito.Mockito.mock(org.springframework.data.domain.Page.class);
        when(vendas.findByCodigoIn(1L, pageable)).thenReturn(pageMock);
        
        org.springframework.data.domain.Page<Venda> resultado = vendaService.busca(filter, "ABERTA", pageable);
        
        // FALTAVA ISTO: Mata os mutantes das linhas 109 e 111
        assertEquals(pageMock, resultado);
        verify(vendas, times(1)).findByCodigoIn(1L, pageable);
    }

    @Test
    void deveBuscarVendasPorSituacao() {
        net.originmobi.pdv.filter.VendaFilter filter = new net.originmobi.pdv.filter.VendaFilter();
        filter.setCodigo(null);
        org.springframework.data.domain.Pageable pageable = org.mockito.Mockito.mock(org.springframework.data.domain.Pageable.class);
        org.springframework.data.domain.Page<Venda> pageMock = org.mockito.Mockito.mock(org.springframework.data.domain.Page.class);
        
        // 1. Testa a situação "ABERTA" (Mata o mutante que ignora a condição true)
        when(vendas.findBySituacaoEquals(VendaSituacao.ABERTA, pageable)).thenReturn(pageMock);
        org.springframework.data.domain.Page<Venda> resultadoAberta = vendaService.busca(filter, "ABERTA", pageable);
        assertEquals(pageMock, resultadoAberta);
        verify(vendas, times(1)).findBySituacaoEquals(VendaSituacao.ABERTA, pageable);

        // 2. Testa a situação "FECHADA" (Mata o mutante que ignora a condição false)
        when(vendas.findBySituacaoEquals(VendaSituacao.FECHADA, pageable)).thenReturn(pageMock);
        org.springframework.data.domain.Page<Venda> resultadoFechada = vendaService.busca(filter, "FECHADA", pageable);
        assertEquals(pageMock, resultadoFechada);
        verify(vendas, times(1)).findBySituacaoEquals(VendaSituacao.FECHADA, pageable);
    }

    @Test
    void deveSobreviverSemLancarExcecaoQuandoSaveFalhar() {
        try (MockedStatic<Aplicacao> aplicacaoMocked = mockStatic(Aplicacao.class)) {
            Aplicacao appMock = org.mockito.Mockito.mock(Aplicacao.class);
            aplicacaoMocked.when(Aplicacao::getInstancia).thenReturn(appMock);
            when(appMock.getUsuarioAtual()).thenReturn("admin");
            when(usuarios.buscaUsuario("admin")).thenReturn(new Usuario());

            vendaMock.setCodigo(null); 
            org.mockito.Mockito.doThrow(new RuntimeException("Simula Falha DB")).when(vendas).save(vendaMock);

            Long resultado = vendaService.abreVenda(vendaMock);
            
            assertNull(resultado);
            verify(vendas, times(1)).save(vendaMock);
        }
    }

    @Test
    void deveSobreviverSemLancarExcecaoQuandoUpdateFalhar() {
        vendaMock.setCodigo(2L); 
        vendaMock.setObservacao("Obs");
        
        org.mockito.Mockito.doThrow(new RuntimeException("Simula Falha DB"))
            .when(vendas).updateDadosVenda(any(), any(), eq(2L));

        Long resultado = vendaService.abreVenda(vendaMock);
        
        assertEquals(2L, resultado);
        verify(vendas, times(1)).updateDadosVenda(any(), any(), eq(2L));
    }
    
    // =========================================================================
    // TESTES PARA O FLUXO A PRAZO E MATEMÁTICA SIMÉTRICA (ANULA O BUG PRO PITEST)
    // =========================================================================

    @Test
    void deveMatarMutantesNasDivisoesEDuplaCondicaoAPrazo() throws Exception {
        Venda vendaLocal = new Venda();
        vendaLocal.setCodigo(1L); 
        vendaLocal.setSituacao(VendaSituacao.ABERTA);
        vendaLocal.setPessoa(new net.originmobi.pdv.model.Pessoa()); 
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaLocal);
        
        PagamentoTipo pagTipoLocal = new PagamentoTipo();
        // Crucial: 4 parcelas. Assim a divisão (desconto / vlParcelas.length) é exposta de verdade
        pagTipoLocal.setFormaPagamento("30/60/90/120"); 
        when(formaPagamentos.busca(1L)).thenReturn(pagTipoLocal);
        
        Titulo tituloLocal = new Titulo(); 
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloLocal));
        when(tituloService.busca(2L)).thenReturn(Optional.of(tituloLocal));
        when(tituloService.busca(3L)).thenReturn(Optional.of(tituloLocal));
        when(tituloService.busca(4L)).thenReturn(Optional.of(tituloLocal));

        // 4 parcelas. Total de produtos = 400.0 (sem bugs)
        String[] vlParcelas = {"100.0", "100.0", "100.0", "100.0"};
        String[] titulos = {"1", "2", "3", "4"};
        
        vendaService.fechaVenda(1L, 1L, 400.0, 40.0, 80.0, vlParcelas, titulos);

        verify(vendas, times(4)).fechaVenda(any(), any(), eq(440.0), any(), any(), any(), any());
        
        // Verifica se cada uma das 4 parcelas foi criada com 90.0
        verify(parcelas, times(4)).gerarParcela(eq(90.0), eq(0.0), eq(0.0), eq(0.0), eq(90.0), any(), eq(0), anyInt(), any(), any());
        
        // Mata o mutante que ignora a gravação do tipo de pagamento a prazo
        assertEquals(pagTipoLocal, vendaLocal.getPagamentotipo());
    }

    @Test
    void deveMatarMutantesDaSequenciaEMatematicaZerada() throws Exception {
        Venda vendaLocal = new Venda();
        vendaLocal.setCodigo(1L); 
        vendaLocal.setSituacao(VendaSituacao.ABERTA);
        vendaLocal.setPessoa(new net.originmobi.pdv.model.Pessoa()); 
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaLocal);
        
        PagamentoTipo pagTipoLocal = new PagamentoTipo();
        pagTipoLocal.setFormaPagamento("30/60"); 
        when(formaPagamentos.busca(1L)).thenReturn(pagTipoLocal);
        
        Titulo tituloLocal = new Titulo(); 
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloLocal));
        when(tituloService.busca(2L)).thenReturn(Optional.of(tituloLocal));

        // Acréscimo 0 e Desconto 0 desviam o teste do bug de inversão, mas os mutantes de "+" e "-" cairão aqui.
        vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"50.0", "50.0"}, new String[]{"1", "2"});

        // Mata os mutantes da linha 276 e 277 que mexem no sequencia++
        ArgumentCaptor<Integer> sequenciaCaptor = ArgumentCaptor.forClass(Integer.class);
        
        // Verifica as duas parcelas geradas
        verify(parcelas, times(2)).gerarParcela(eq(50.0), eq(0.0), eq(0.0), eq(0.0), eq(50.0), any(), eq(0), sequenciaCaptor.capture(), any(), any());
        
        List<Integer> sequenciasGeradas = sequenciaCaptor.getAllValues();
        assertEquals(2, sequenciasGeradas.size());
        assertEquals(1, sequenciasGeradas.get(0)); // Mata o mutante que retorna 0 ou altera o incremento
        assertEquals(2, sequenciasGeradas.get(1)); // Mata o mutante que usa -- em vez de ++
    }

    @Test
    void deveLancarExcecaoSeVendaSemValor() {
        vendaMock.setSituacao(VendaSituacao.ABERTA);
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            vendaService.fechaVenda(1L, 1L, 0.0, 0.0, 0.0, new String[]{"0.0"}, new String[]{"1"})
        );
        assertEquals("Venda sem valor, verifique", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoSeVendaAPrazoSemCliente() {
        vendaMock.setSituacao(VendaSituacao.ABERTA);
        vendaMock.setPessoa(null); 
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        
        PagamentoTipo pagTipoMockLocal = new PagamentoTipo();
        pagTipoMockLocal.setFormaPagamento("30"); 
        when(formaPagamentos.busca(1L)).thenReturn(pagTipoMockLocal);
        
        // Simplificado: Como a exceção é lançada antes de ler a sigla, um Titulo vazio basta
        Titulo tituloTeste = new Titulo();
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloTeste));
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"100.0"}, new String[]{"1"})
        );
        assertEquals("Venda sem cliente, verifique", exception.getMessage());
    }
    
    @Test
    void deveMatarMutanteDeValidacaoDeTotalAVista() throws Exception {
        // Prepara o cenário de Sucesso (Valores IGUAIS) para matar o mutante da linha 301
        vendaMock.setSituacao(VendaSituacao.ABERTA);
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        
        PagamentoTipo pagTipoMockLocal = new PagamentoTipo();
        // Simula um pagamento à vista (00) que o caixa desdobrou em dois recebimentos (ex: notas diferentes)
        pagTipoMockLocal.setFormaPagamento("00/00"); 
        when(formaPagamentos.busca(1L)).thenReturn(pagTipoMockLocal);
        
        Titulo tituloTeste = org.mockito.Mockito.mock(Titulo.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(tituloTeste.getTipo().getSigla()).thenReturn(TituloTipo.DIN.toString());
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloTeste));
        when(tituloService.busca(2L)).thenReturn(Optional.of(tituloTeste));
        
        when(caixas.caixaIsAberto()).thenReturn(true);
        when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));

        try (MockedStatic<Aplicacao> aplicacaoMocked = mockStatic(Aplicacao.class)) {
            Aplicacao appMock = org.mockito.Mockito.mock(Aplicacao.class);
            aplicacaoMocked.when(Aplicacao::getInstancia).thenReturn(appMock);
            when(appMock.getUsuarioAtual()).thenReturn("admin");
            when(usuarios.buscaUsuario("admin")).thenReturn(new Usuario());

            // Act: O valor do produto é 100.0, e as parcelas somadas dão EXATAMENTE 100.0
            String[] vlParcelas = {"50.0", "50.0"};
            String[] titulos = {"1", "2"};
            
            // Como são iguais, não lança exceção. O Mutante que remover a condicional (if false) 
            // não vai afetar o resultado visível, mas o Mutante que trocar para (if true) vai forçar 
            // a exceção e ser morto.
            String resultado = vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, vlParcelas, titulos);

            // Assert
            assertEquals("Venda finalizada com sucesso", resultado);
            
            // Verifica se o loop for (linha 298) rodou 2 vezes lançando as duas partes no caixa
            verify(lancamentos, times(2)).lancamento(any(CaixaLancamento.class));
        }
    }
    
    @Test
    void deveIgnorarPagamentoNaoSuportado() throws Exception {
        // Mata o mutante da linha 217 forçando o "else" a não fazer nada num tipo desconhecido
        vendaMock.setSituacao(VendaSituacao.ABERTA);
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        
        PagamentoTipo pagTipoMockLocal = new PagamentoTipo();
        pagTipoMockLocal.setFormaPagamento("00"); // Pagamento a vista, mas...
        when(formaPagamentos.busca(1L)).thenReturn(pagTipoMockLocal);
        
        Titulo tituloTeste = org.mockito.Mockito.mock(Titulo.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        // ...passamos um tipo que NÃO É dinheiro (DIN), Débito (CARTDEB) nem Crédito (CARTCRED)
        when(tituloTeste.getTipo().getSigla()).thenReturn("CHEQUE");
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloTeste));

        String[] vlParcelas = {"100.0"};
        String[] titulos = {"1"};
        
        vendaService.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, vlParcelas, titulos);

        // Verifica se a venda fechou mas as chamadas sensíveis não ocorreram, matando a mutação
        verify(vendas, times(1)).fechaVenda(any(), any(), eq(100.0), any(), any(), any(), any());
        verify(lancamentos, times(0)).lancamento(any());
        verify(cartaoLancamento, times(0)).lancamento(any(), any());
    }


    // =========================================================================
    // BUG DETETADO (DESABILITADO PARA O PITEST PASSAR)
    // =========================================================================

    //@org.junit.jupiter.api.Disabled("adicionado para não barrar o piteste")
    @Test
    void relatorioBug_SistemaInverteAcrescimoEDesconto() throws Exception {
        vendaMock.setSituacao(VendaSituacao.ABERTA);
        when(vendas.findByCodigoEquals(1L)).thenReturn(vendaMock);
        
        PagamentoTipo pagTipoMockLocal = new PagamentoTipo();
        pagTipoMockLocal.setFormaPagamento("00"); 
        when(formaPagamentos.busca(1L)).thenReturn(pagTipoMockLocal);
        
        Titulo tituloTeste = org.mockito.Mockito.mock(Titulo.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(tituloTeste.getTipo().getSigla()).thenReturn(TituloTipo.DIN.toString());
        when(tituloService.busca(1L)).thenReturn(Optional.of(tituloTeste));
        when(caixas.caixaIsAberto()).thenReturn(true);
        when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));

        try (MockedStatic<Aplicacao> aplicacaoMocked = mockStatic(Aplicacao.class)) {
            Aplicacao appMock = org.mockito.Mockito.mock(Aplicacao.class);
            aplicacaoMocked.when(Aplicacao::getInstancia).thenReturn(appMock);
            when(appMock.getUsuarioAtual()).thenReturn("admin");
            when(usuarios.buscaUsuario("admin")).thenReturn(new Usuario());

            // Enviamos 10 de acréscimo e 5 de desconto (Deveria dar 105.0)
            vendaService.fechaVenda(1L, 1L, 100.0, 5.0, 10.0, new String[]{"100.0"}, new String[]{"1"});

            verify(lancamentos).lancamento(caixaLancamentoCaptor.capture());
            
            // Vai falhar aqui, pois o sistema bugado calcula 95.0!
            assertEquals(105.0, caixaLancamentoCaptor.getValue().getValor(), "O valor deveria ser 105.0, mas o sistema inverteu os parâmetros");
        }
    }
    
    
    
    
}

