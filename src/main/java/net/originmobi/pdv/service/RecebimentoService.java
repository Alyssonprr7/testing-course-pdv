package net.originmobi.pdv.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ibm.icu.text.DecimalFormat;

import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.enumerado.TituloTipo;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Parcela;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Recebimento;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.RecebimentoRepository;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;
import net.originmobi.pdv.singleton.Aplicacao;
import net.originmobi.pdv.utilitarios.DataAtual;

@Service
public class RecebimentoService {

	private final RecebimentoRepository recebimentos;
	private final PessoaService pessoas;
	private final RecebimentoParcelaService receParcelas;
	private final ParcelaService parcelas;
	private final CaixaService caixas;
	private final UsuarioService usuarios;
	private final CaixaLancamentoService lancamentos;
	private final TituloService titulos;
	private final CartaoLancamentoService cartaoLancamentos;

	@Autowired
	public RecebimentoService(RecebimentoRepository recebimentos, PessoaService pessoas,
			RecebimentoParcelaService receParcelas, ParcelaService parcelas, CaixaService caixas,
			UsuarioService usuarios, CaixaLancamentoService lancamentos, TituloService titulos,
			CartaoLancamentoService cartaoLancamentos) {
		this.recebimentos = recebimentos;
		this.pessoas = pessoas;
		this.receParcelas = receParcelas;
		this.parcelas = parcelas;
		this.caixas = caixas;
		this.usuarios = usuarios;
		this.lancamentos = lancamentos;
		this.titulos = titulos;
		this.cartaoLancamentos = cartaoLancamentos;
	}

	public String abrirRecebimento(Long codpes, String[] arrayParcelas) {
		List<Parcela> lista = new ArrayList<>();

		DataAtual dataAtual = new DataAtual();
		Double vlTotal = 0.0;

		for (int i = 0; i < arrayParcelas.length; i++) {
			Parcela parcela = parcelas.busca(Long.decode(arrayParcelas[i]));

			if (parcela.getQuitado() == 1)
				throw new RuntimeException("Parcela " + parcela.getCodigo() + " já esta quitada, verifique.");

			if (!parcela.getReceber().getPessoa().getCodigo().equals(codpes))
				throw new RuntimeException("A parcela " + parcela.getCodigo() + " não pertence ao cliente selecionado");

			try {
				lista.add(parcela);

				vlTotal = vlTotal + parcela.getValor_restante();

			} catch (Exception e) {
				throw new RuntimeException("Erro ao receber, chame o suporte", e);
			}
		}

		Optional<Pessoa> pessoa = pessoas.buscaPessoa(codpes);

		if (!pessoa.isPresent())
			throw new RuntimeException("Cliente não encontrado");

		Recebimento recebimento = new Recebimento(vlTotal, dataAtual.dataAtualTimeStamp(), pessoa.get(), lista);

		try {
			recebimentos.save(recebimento);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao receber, chame o suporte", e);
		}

		return recebimento.getCodigo().toString();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String receber(Long codreceber, Double vlrecebido, Double vlacrescimo, Double vldesconto, Long codtitulo) {
		if (codtitulo == null || codtitulo == 0)
			throw new RuntimeException("Selecione um título para realizar o recebimento");

		Recebimento recebimento = recebimentos.findById(codreceber)
				.orElseThrow(() -> new RuntimeException("Recebimento não encontrado"));

		if (recebimento.getData_processamento() != null)
			throw new RuntimeException("Recebimento já esta fechado");

		Titulo titulo = titulos.busca(codtitulo).orElseThrow(() -> new RuntimeException("Título não encontrado"));

		// vincula o titulo ao recebimento
		recebimento.setTitulo(titulo);

		DecimalFormat formata = new DecimalFormat("0.00");
		Double vlrecebimento = Double.valueOf(formata.format(recebimento.getValor_total()).replaceAll(",", "."));

		if (vlrecebido > vlrecebimento)
			throw new RuntimeException("Valor de recebimento é superior aos títulos");

		List<Parcela> listParcelas = receParcelas.parcelasDoReceber(codreceber);

		if (listParcelas.isEmpty())
			throw new RuntimeException("Recebimento não possue parcelas");

		if (vlrecebido <= 0.0)
			throw new RuntimeException("Valor de recebimento inválido");

		// guarda o valor do lançamento de caixa
		Double vllancamento = vlrecebido;

		// verifica cada parcela que veio e realiza o seu recebimento individual
		for (int i = 0; i < listParcelas.size(); i++) {

			if (vlrecebido > 0) {
				Double vlsobra = vlrecebido - listParcelas.get(i).getValor_restante();
				vlsobra = vlsobra < 0 ? 0 : vlsobra;

				Double vlquitado = vlsobra - vlrecebido;
				vlquitado = vlquitado < 0 ? vlquitado * -1 : vlquitado;

				vlrecebido = vlsobra;

				Long parcela = listParcelas.get(i).getCodigo();

				try {
					parcelas.receber(parcela, vlquitado, 0.00, 0.00);
				} catch (Exception e) {
					throw new RuntimeException("Ocorreu um erro ao realizar o recebimento, chame o suporte", e);
				}
			}

		}

		Aplicacao aplicacao = new Aplicacao();
		Usuario usuario = usuarios.buscaUsuario(aplicacao.getUsuarioAtual());

		// pega a sigla do titulo
		String sigla = titulo.getTipo().getSigla();

		// verifica se é um lançamento do tipo cartão para lançar o cartao_lancamento
		if (sigla.equals(TituloTipo.CARTDEB.toString()) || sigla.equals(TituloTipo.CARTCRED.toString())) {
			cartaoLancamentos.lancamento(vllancamento, Optional.of(titulo));

		} else {
			Caixa caixa = caixas.caixaAberto().orElseThrow(() -> new RuntimeException("Nenhum caixa aberto"));
			CaixaLancamento lancamento = new CaixaLancamento("Referente ao recebimento " + codreceber, vllancamento,
					TipoLancamento.RECEBIMENTO, EstiloLancamento.ENTRADA, caixa, usuario);

			// vincula o recebimento ao caixa_lancamento
			lancamento.setRecebimento(recebimento);

			// Faz um caixa lançamento no valor do receber;
			try {
				lancamentos.lancamento(lancamento);
			} catch (Exception e) {
				throw new RuntimeException("Ocorreu um erro ao realizar o recebimento, chame o suporte", e);
			}
		}

		try {
			DataAtual dataAtual = new DataAtual();

			recebimento.setValor_recebido(vllancamento);
			recebimento.setValor_acrescimo(vlacrescimo);
			recebimento.setValor_desconto(vldesconto);
			recebimento.setData_processamento(dataAtual.dataAtualTimeStamp());

			recebimentos.save(recebimento);
		} catch (Exception e) {
			throw new RuntimeException("Ocorreu um erro ao realizar o recebimento, chame o suporte", e);
		}

		return "Recebimento realizado com sucesso";
	}

	public String remover(Long codigo) {
		Optional<Recebimento> recebimento = recebimentos.findById(codigo);

		if (recebimento.map(Recebimento::getData_processamento).isPresent())
			throw new RuntimeException("Esse recebimento não pode ser removido, pois ele já esta processado");

		try {
			recebimentos.deleteById(codigo);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao remover orçamento, chame o suporte", e);
		}

		return "removido com sucesso";
	}

}
