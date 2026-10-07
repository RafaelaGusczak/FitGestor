package view;

import dao.PlanoDao;
import db.Banco;
import model.Plano;
import util.Formato;

import javax.swing.*;
import java.sql.SQLException;
import java.util.List;

public class PlanosPagina extends CadastroPagina<Plano> {

    private final PlanoDao dao = new PlanoDao();

    private final JTextField nome = Tema.campo("");
    private final JTextField descricao = Tema.campo("");
    private final JTextField valor = Tema.campo("");
    private final JTextField duracao = Tema.campo("");

    public PlanosPagina() {
        super("Planos", "Planos de assinatura oferecidos pela academia");
        montar("Nome do plano", null,
                new String[]{"ID", "Plano", "Descrição", "Valor mensal", "Duração"},
                "Dados do plano",
                Tema.rotulado("Nome do plano", nome),
                Tema.rotulado("Descrição", descricao),
                Tema.par(Tema.rotulado("Valor mensal (R$)", valor), Tema.rotulado("Duração (meses)", duracao)));
    }

    @Override
    protected Banco.Operacao<List<Plano>> consulta(String texto) {
        return () -> dao.buscar(texto);
    }

    @Override
    protected Integer idDe(Plano p) {
        return p.id();
    }

    @Override
    protected Object[] linha(Plano p) {
        return new Object[]{p.id(), p.nome(), p.descricao() == null ? "" : p.descricao(),
                Formato.moeda(p.valorMensal()), p.duracaoMeses() + (p.duracaoMeses() == 1 ? " mês" : " meses")};
    }

    @Override
    protected void preencher(Plano p) {
        nome.setText(p.nome());
        descricao.setText(p.descricao() == null ? "" : p.descricao());
        valor.setText(Formato.decimal(p.valorMensal()));
        duracao.setText(String.valueOf(p.duracaoMeses()));
    }

    @Override
    protected void limpar() {
        nome.setText("");
        descricao.setText("");
        valor.setText("");
        duracao.setText("");
    }

    @Override
    protected Plano ler(Integer id) {
        return new Plano(id, Formato.obrigatorio(nome.getText(), "o nome do plano"),
                Formato.opcional(descricao.getText()), Formato.lerValor(valor.getText(), "Valor mensal"),
                Formato.lerInteiro(duracao.getText(), "Duração (meses)"));
    }

    @Override
    protected int gravar(Plano p) throws SQLException {
        return dao.salvar(p);
    }

    @Override
    protected void remover(Plano p) throws SQLException {
        dao.excluir(p.id());
    }

    @Override
    protected String nomeDe(Plano p) {
        return "o plano " + p.nome();
    }
}
