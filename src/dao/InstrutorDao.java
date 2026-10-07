package dao;

import db.Banco;
import model.Instrutor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InstrutorDao {

    private Instrutor ler(ResultSet rs) throws SQLException {
        return new Instrutor(rs.getInt("id"), rs.getString("nome"), rs.getString("cpf"),
                rs.getString("especialidade"), rs.getString("telefone"));
    }

    public List<Instrutor> buscar(String texto) throws SQLException {
        String sql = "select id, nome, cpf, especialidade, telefone from instrutor "
                + "where nome ilike ? or (? <> '' and regexp_replace(cpf, '\\D', '', 'g') like ?) order by nome";
        String digitos = util.Formato.soDigitos(texto);
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            ps.setString(1, Banco.parteDe(texto));
            ps.setString(2, digitos);
            ps.setString(3, "%" + digitos + "%");
            List<Instrutor> r = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    r.add(ler(rs));
                }
            }
            return r;
        }
    }

    public int salvar(Instrutor i) throws SQLException {
        if (i.id() == null) {
            String sql = "insert into instrutor (nome, cpf, especialidade, telefone) values (?, ?, ?, ?) returning id";
            try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
                preencher(ps, i);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
        String sql = "update instrutor set nome = ?, cpf = ?, especialidade = ?, telefone = ? where id = ?";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            preencher(ps, i);
            ps.setInt(5, i.id());
            ps.executeUpdate();
        }
        return i.id();
    }

    private void preencher(PreparedStatement ps, Instrutor i) throws SQLException {
        ps.setString(1, i.nome());
        ps.setString(2, i.cpf());
        ps.setString(3, i.especialidade());
        ps.setString(4, i.telefone());
    }

    public void excluir(int id) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement("delete from instrutor where id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
