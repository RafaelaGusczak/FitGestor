package view;

import dao.InstrutorDao;
import db.Banco;
import model.Instrutor;
import util.Formato;

import javax.swing.*;
import java.sql.SQLException;
import java.util.List;

public class InstrutoresPagina extends CadastroPagina<Instrutor> {

    private final InstrutorDao dao = new InstrutorDao();

    private final JTextField nome = Tema.campo("");
    private final JTextField cpf = Tema.campo("");
    private final JTextField especialidade = Tema.campo("");
    private final JTextField telefone = Tema.campo("");

    public InstrutoresPagina() {
        super("Instrutores", "Cadastro da equipe de instrutores");
        Mascara.cpf(cpf);
        Mascara.telefone(telefone);
        montar("Nome ou CPF", null,
                new String[]{"ID", "Nome", "CPF", "Especialidade", "Telefone"},
                "Dados do instrutor",
                Tema.rotulado("Nome completo", nome),
                Tema.rotulado("CPF", cpf),
                Tema.rotulado("Especialidade", especialidade),
                Tema.rotulado("Telefone", telefone));
    }

    @Override
    protected Banco.Operacao<List<Instrutor>> consulta(String texto) {
        return () -> dao.buscar(texto);
    }

    @Override
    protected Integer idDe(Instrutor i) {
        return i.id();
    }

    @Override
    protected Object[] linha(Instrutor i) {
        return new Object[]{i.id(), i.nome(), i.cpf(), i.especialidade() == null ? "" : i.especialidade(),
                i.telefone() == null ? "" : i.telefone()};
    }

    @Override
    protected void preencher(Instrutor i) {
        nome.setText(i.nome());
        cpf.setText(i.cpf());
        especialidade.setText(i.especialidade() == null ? "" : i.especialidade());
        telefone.setText(i.telefone() == null ? "" : i.telefone());
    }

    @Override
    protected void limpar() {
        nome.setText("");
        cpf.setText("");
        especialidade.setText("");
        telefone.setText("");
    }

    @Override
    protected Instrutor ler(Integer id) {
        return new Instrutor(id, Formato.obrigatorio(nome.getText(), "o nome completo"),
                Formato.lerCpf(cpf.getText()), Formato.opcional(especialidade.getText()),
                Formato.opcional(telefone.getText()));
    }

    @Override
    protected int gravar(Instrutor i) throws SQLException {
        return dao.salvar(i);
    }

    @Override
    protected void remover(Instrutor i) throws SQLException {
        dao.excluir(i.id());
    }

    @Override
    protected String nomeDe(Instrutor i) {
        return "o instrutor " + i.nome();
    }
}
