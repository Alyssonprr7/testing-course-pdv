package net.originmobi.pdv.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

//import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import net.originmobi.pdv.enumerado.VendaSituacao;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.model.Venda;
import net.originmobi.pdv.repository.UsuarioRepository;
import net.originmobi.pdv.repository.VendaRepository;
import net.originmobi.pdv.singleton.Aplicacao;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class VendaServiceIntegrationTest {

    @Autowired
    private VendaService vendaService;

    // Injetamos os repositórios reais para preparar o banco e validar os resultados
    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;
    

    private MockedStatic<Aplicacao> aplicacaoMockedStatic;
    private Aplicacao aplicacaoMock;

    @BeforeEach
    void setUp() {
        // O mock estático continua necessário apenas para a classe Aplicacao, 
        // pois ela controla a sessão (estado global) fora da injeção do Spring.
        aplicacaoMock = org.mockito.Mockito.mock(Aplicacao.class);
        aplicacaoMockedStatic = mockStatic(Aplicacao.class);
        aplicacaoMockedStatic.when(Aplicacao::getInstancia).thenReturn(aplicacaoMock);
    }

    @AfterEach
    void tearDown() {
        aplicacaoMockedStatic.close();
    }

    @Test
    @DisplayName("Deve abrir uma nova venda e persistir corretamente no banco H2")
    void testAbreVenda_PersistenciaReal() {
        // Criamos e salvamos um usuário de verdade no banco H2
        Usuario usuario = new Usuario();
        usuario.setUser("aluno.teste");
        usuario.setSenha("senha123");
        usuarioRepository.save(usuario); 

        // Dizemos ao Singleton para retornar o login desse usuário que acabamos de salvar
        when(aplicacaoMock.getUsuarioAtual()).thenReturn("aluno.teste");

        Venda novaVenda = new Venda();
        novaVenda.setObservacao("Venda de integração");

        // 2. Act (Ação)
        // Chamamos o serviço real, que executará a lógica de negócio e o insert no banco
        Long codigoVenda = vendaService.abreVenda(novaVenda);

        // 3. Assert (Verificação)
        assertNotNull(codigoVenda, "O código da venda não deve ser nulo após salvar");
        
        // Vamos direto no banco de dados verificar se os dados estão lá
        Venda vendaSalva = vendaRepository.findById(codigoVenda).orElse(null);
        
        assertNotNull(vendaSalva, "A venda deve ser encontrada no banco de dados");
        assertEquals(VendaSituacao.ABERTA, vendaSalva.getSituacao());
        assertEquals("Venda de integração", vendaSalva.getObservacao());
        assertEquals(0.00, vendaSalva.getValor_produtos());
        assertEquals("aluno.teste", vendaSalva.getUsuario().getUser(), "O usuário deve ter sido mapeado no banco");
        assertNotNull(vendaSalva.getData_cadastro());
    }
    
    @Test
    public void testFechaVenda_ComValorZero_DeveLancarExcecao() {
        Venda vendaAberta = new Venda();
        Long codVenda = vendaService.abreVenda(vendaAberta);

        // Act & Assert
        // A regra diz que vlprodutos <= 0 lança execeção "Venda sem valor, verifique"
        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class, 
            () -> vendaService.fechaVenda(codVenda, 1L, 0.0, 0.0, 0.0, new String[]{"0.0"}, new String[]{"1"})
        );

        org.junit.jupiter.api.Assertions.assertEquals("Venda sem valor, verifique", exception.getMessage());
    }
    
    
    @Test
    public void testFechaVenda_JaFechada_DeveLancarExcecao() {
        net.originmobi.pdv.model.Venda venda = new net.originmobi.pdv.model.Venda();
        Long codVenda = vendaService.abreVenda(venda);
        
        // Simula o fecho da venda atualizando o estado diretamente no banco de dados H2
        net.originmobi.pdv.model.Venda vendaSalva = vendaRepository.findByCodigoEquals(codVenda);
        vendaSalva.setSituacao(net.originmobi.pdv.enumerado.VendaSituacao.FECHADA);
        vendaRepository.save(vendaSalva);

        // Act & Assert (Ação e Validação)
        // Tentamos fechar uma venda (com valor válido > 0), mas que já possui o status FECHADA
        RuntimeException exception = assertThrows(
            RuntimeException.class, 
            () -> vendaService.fechaVenda(codVenda, 1L, 10.0, 0.0, 0.0, new String[]{"10.0"}, new String[]{"1"})
        );

        assertEquals("venda fechada", exception.getMessage());
    }
    
    
    @Test
    public void testAddProduto_EmVendaFechada_DeveRetornarMensagemVendaFechada() {
        Venda venda = new Venda();
        Long codVenda = vendaService.abreVenda(venda);
        
        // Simula o fecho da venda no banco H2
        Venda vendaSalva = vendaRepository.findByCodigoEquals(codVenda);
        vendaSalva.setSituacao(VendaSituacao.FECHADA);
        vendaRepository.save(vendaSalva);

        // Tentamos adicionar um produto (ex: código do produto = 1L, peso/valor balança = 1.0) numa venda fechada
        String resultado = vendaService.addProduto(codVenda, 1L, 1.0);

        // Assert (Validação)
        assertEquals("Venda fechada", resultado);
    }
    
    
    @Test
    public void testRemoveProduto_EmVendaFechada_DeveRetornarMensagemVendaFechada() {
        Venda venda = new Venda();
        Long codVenda = vendaService.abreVenda(venda);
        
        // Simula o fecho da venda no banco H2
        Venda vendaSalva = vendaRepository.findByCodigoEquals(codVenda);
        vendaSalva.setSituacao(VendaSituacao.FECHADA);
        vendaRepository.save(vendaSalva);

        // Tentamos remover o produto da posição 1 numa venda que já está fechada
        String resultado = vendaService.removeProduto(1L, codVenda);

        // Assert (Validação)
        assertEquals("Venda fechada", resultado);
    }
}
