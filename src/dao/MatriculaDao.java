package dao;

import db.Banco;
import model.Matricula;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MatriculaDao {

    private static final String SELECT =
            "select m.id, m.aluno_id, a.nome as aluno, m.plano_id, pl.nome as plano, "
            + "m.data_inicio, m.data_termino, m.valor_mensal, m.status "
            + "from matricula m join aluno a on a.id = m.aluno_id join plano pl on pl.id = m.plano_id ";

    private Matricula ler(ResultSet rs) throws SQLException {
        return new Matricula(rs.getInt("id"), rs.getInt("aluno_id"), rs.getString("aluno"),
                rs.getInt("plano_id"), rs.getString("plano"), Sql.data(rs, "data_inicio"),
                Sql.data(rs, "data_termino"), rs.getBigDecimal("valor_mensal"), rs.getString("status"));
    }

    /** As matrículas mais recentes primeiro. */
    public List<Matricula> recentes(int limite) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement(SELECT + "order by m.id desc limit ?")) {
            ps.setInt(1, limite);
            List<Matricula> r = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    r.add(ler(rs));
                }
            }
            return r;
        }
    }

    public int inserir(Matricula m) throws SQLException {
        String sql = "insert into matricula (aluno_id, plano_id, data_inicio, data_termino, valor_mensal, status) "
                + "values (?, ?, ?, ?, ?, ?) returning id";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            ps.setInt(1, m.alunoId());
            ps.setInt(2, m.planoId());
            Sql.data(ps, 3, m.inicio());
            Sql.data(ps, 4, m.termino());
            ps.setBigDecimal(5, m.valorMensal());
            ps.setString(6, m.status());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void atualizarStatus(int id, String status) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement("update matricula set status = ? where id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** O aluno já tem matrícula ativa cujo período cruza com [inicio, termino)? */
    public boolean existeAtivaSobreposta(int alunoId, LocalDate inicio, LocalDate termino) throws SQLException {
        String sql = "select 1 from matricula where aluno_id = ? and status = 'Ativa' "
                + "and data_inicio < ? and data_termino > ? limit 1";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            ps.setInt(1, alunoId);
            Sql.data(ps, 2, termino);
            Sql.data(ps, 3, inicio);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int contarAVencer(LocalDate de, LocalDate ate) throws SQLException {
        String sql = "select count(*) from matricula where status = 'Ativa' and data_termino between ? and ?";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            Sql.data(ps, 1, de);
            Sql.data(ps, 2, ate);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
