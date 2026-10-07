package dao;

import db.Banco;
import model.Plano;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlanoDao {

    private Plano ler(ResultSet rs) throws SQLException {
        return new Plano(rs.getInt("id"), rs.getString("nome"), rs.getString("descricao"),
                rs.getBigDecimal("valor_mensal"), rs.getInt("duracao_meses"));
    }

    public List<Plano> buscar(String texto) throws SQLException {
        String sql = "select id, nome, descricao, valor_mensal, duracao_meses from plano "
                + "where nome ilike ? order by duracao_meses, nome";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            ps.setString(1, Banco.parteDe(texto));
            List<Plano> r = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    r.add(ler(rs));
                }
            }
            return r;
        }
    }

    public int salvar(Plano p) throws SQLException {
        if (p.id() == null) {
            String sql = "insert into plano (nome, descricao, valor_mensal, duracao_meses) values (?, ?, ?, ?) returning id";
            try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
                preencher(ps, p);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
        String sql = "update plano set nome = ?, descricao = ?, valor_mensal = ?, duracao_meses = ? where id = ?";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            preencher(ps, p);
            ps.setInt(5, p.id());
            ps.executeUpdate();
        }
        return p.id();
    }

    private void preencher(PreparedStatement ps, Plano p) throws SQLException {
        ps.setString(1, p.nome());
        ps.setString(2, p.descricao());
        ps.setBigDecimal(3, p.valorMensal());
        ps.setInt(4, p.duracaoMeses());
    }

    public void excluir(int id) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement("delete from plano where id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
