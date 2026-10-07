package view;

import dao.AlunoDao;
import db.Banco;
import model.Aluno;
import util.Formato;
import util.RegraNegocioException;

import javax.swing.*;
import java.sql.SQLException;
import java.util.List;

public class AlunosPagina extends CadastroPagina<Aluno> {

    private final AlunoDao dao = new AlunoDao();

    private final JComboBox<String> filtroSituacao = Tema.combo("Todos", "Ativo", "Inativo");
    private final JTextField nome = Tema.campo("");
    private final JTextField cpf = Tema.campo("");
    private final JTextField nascimento = Tema.campo("");
    private final JTextField telefone = Tema.campo("");
    private final JTextField email = Tema.campo("");
    private final JComboBox<String> situacao = Tema.combo("Ativo", "Inativo");

    public AlunosPagina() {
        super("Alunos", "Cadastro e manutenção de alunos");
        Mascara.cpf(cpf);
        Mascara.data(nascimento);
        Mascara.telefone(telefone);
        filtroSituacao.addActionListener(e -> pesquisar());
        montar("Nome ou CPF", Tema.rotulado("Situação", filtroSituacao),
                new String[]{"ID", "Nome", "CPF", "Telefone", "Situação"},
                "Dados do aluno",
                Tema.rotulado("Nome completo", nome),
                Tema.rotulado("CPF", cpf),
                Tema.par(Tema.rotulado("Nascimento", nascimento), Tema.rotulado("Telefone", telefone)),
                Tema.rotulado("E-mail", email),
                Tema.rotulado("Situação", situacao));
    }

    @Override
    protected Banco.Operacao<List<Aluno>> consulta(String texto) {
        String filtro = (String) filtroSituacao.getSelectedItem();
        return () -> dao.buscar(texto, filtro);
    }

    @Override
    protected Integer idDe(Aluno a) {
        return a.id();
    }

    @Override
    protected Object[] linha(Aluno a) {
        return new Object[]{a.id(), a.nome(), a.cpf(), a.telefone() == null ? "" : a.telefone(), a.situacao()};
    }

    @Override
    protected void preencher(Aluno a) {
        nome.setText(a.nome());
        cpf.setText(a.cpf());
        nascimento.setText(Formato.data(a.nascimento()));
        telefone.setText(a.telefone() == null ? "" : a.telefone());
        email.setText(a.email() == null ? "" : a.email());
        situacao.setSelectedItem(a.situacao());
    }

    @Override
    protected void limpar() {
        nome.setText("");
        cpf.setText("");
        nascimento.setText("");
        telefone.setText("");
        email.setText("");
        situacao.setSelectedItem("Ativo");
    }

    @Override
    protected Aluno ler(Integer id) {
        String mail = Formato.opcional(email.getText());
        if (mail != null && !mail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new RegraNegocioException("E-mail inválido.");
        }
        return new Aluno(id, Formato.obrigatorio(nome.getText(), "o nome completo"), Formato.lerCpf(cpf.getText()),
                Formato.lerData(nascimento.getText(), "Nascimento", false), Formato.opcional(telefone.getText()),
                mail, (String) situacao.getSelectedItem());
    }

    @Override
    protected int gravar(Aluno a) throws SQLException {
        return dao.salvar(a);
    }

    @Override
    protected void remover(Aluno a) throws SQLException {
        dao.excluir(a.id());
    }

    @Override
    protected String nomeDe(Aluno a) {
        return "o aluno " + a.nome() + " (para quem já tem matrícula, prefira marcar como Inativo)";
    }
}
