package dao;

import db.Banco;
import model.Aluno;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AlunoDao {

    private static final String COLUNAS = "id, nome, cpf, nascimento, telefone, email, situacao";

    private Aluno ler(ResultSet rs) throws SQLException {
        return new Aluno(rs.getInt("id"), rs.getString("nome"), rs.getString("cpf"), Sql.data(rs, "nascimento"),
                rs.getString("telefone"), rs.getString("email"), rs.getString("situacao"));
    }

    /** Busca por parte do nome ou do CPF. situacao: "Todos", "Ativo" ou "Inativo". */
    public List<Aluno> buscar(String texto, String situacao) throws SQLException {
        String sql = "select " + COLUNAS + " from aluno "
                + "where (nome ilike ? or (? <> '' and regexp_replace(cpf, '\\D', '', 'g') like ?)) "
                + "and (? = 'Todos' or situacao = ?) order by nome";
        String digitos = util.Formato.soDigitos(texto);
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            ps.setString(1, Banco.parteDe(texto));
            ps.setString(2, digitos);
            ps.setString(3, "%" + digitos + "%");
            ps.setString(4, situacao);
            ps.setString(5, situacao);
            return lista(ps);
        }
    }

    public List<Aluno> ativos() throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement(
                "select " + COLUNAS + " from aluno where situacao = 'Ativo' order by nome")) {
            return lista(ps);
        }
    }

    public int contarAtivos() throws SQLException {
        try (Statement st = Banco.conexao().createStatement();
             ResultSet rs = st.executeQuery("select count(*) from aluno where situacao = 'Ativo'")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private List<Aluno> lista(PreparedStatement ps) throws SQLException {
        List<Aluno> r = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                r.add(ler(rs));
            }
        }
        return r;
    }

    /** Insere (id == null) ou atualiza. Devolve o id. */
    public int salvar(Aluno a) throws SQLException {
        if (a.id() == null) {
            String sql = "insert into aluno (nome, cpf, nascimento, telefone, email, situacao) "
                    + "values (?, ?, ?, ?, ?, ?) returning id";
            try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
                preencher(ps, a);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
        String sql = "update aluno set nome = ?, cpf = ?, nascimento = ?, telefone = ?, email = ?, situacao = ? where id = ?";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            preencher(ps, a);
            ps.setInt(7, a.id());
            ps.executeUpdate();
        }
        return a.id();
    }

    private void preencher(PreparedStatement ps, Aluno a) throws SQLException {
        ps.setString(1, a.nome());
        ps.setString(2, a.cpf());
        Sql.data(ps, 3, a.nascimento());
        ps.setString(4, a.telefone());
        ps.setString(5, a.email());
        ps.setString(6, a.situacao());
    }

    public void excluir(int id) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement("delete from aluno where id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
