package controller;

import dao.PagamentoDao;
import util.RegraNegocioException;

import java.sql.SQLException;
import java.time.LocalDate;

public class PagamentoController {

    public static final String[] FORMAS = {"PIX", "Dinheiro", "Cartão de crédito", "Cartão de débito", "Boleto"};

    private final PagamentoDao dao = new PagamentoDao();

    public void darBaixa(int pagamentoId, String forma, LocalDate hoje) throws SQLException {
        if (!dao.darBaixa(pagamentoId, forma, hoje)) {
            throw new RegraNegocioException("Essa mensalidade já estava paga.");
        }
    }

    /** Cria as mensalidades que faltam; devolve quantas foram criadas. */
    public int gerarMensalidades() throws SQLException {
        return dao.gerarMensalidades(null);
    }
}
