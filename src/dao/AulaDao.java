package dao;

import db.Banco;
import model.Aula;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AulaDao {

    private static final String SELECT =
            "select a.id, a.modalidade, a.dia_semana, a.horario, a.instrutor_id, i.nome as instrutor, a.vagas, "
            + "(select count(*) from checkin c where c.aula_id = a.id and c.data_hora >= ? and c.data_hora < ?) as ocupadas "
            + "from aula a left join instrutor i on i.id = a.instrutor_id ";

    private static final String ORDEM = " order by array_position(array['Segunda','Terça','Quarta','Quinta','Sexta','Sábado','Domingo'], a.dia_semana), a.horario";

    private Aula ler(ResultSet rs) throws SQLException {
        return new Aula(rs.getInt("id"), rs.getString("modalidade"), rs.getString("dia_semana"),
                Sql.hora(rs, "horario"), Sql.inteiro(rs, "instrutor_id"), rs.getString("instrutor"),
                rs.getInt("vagas"), rs.getInt("ocupadas"));
    }

    /** Aulas filtradas por modalidade e dia ("Todos" = qualquer). inicioDia/fimDia delimitam "hoje" para contar vagas. */
    public List<Aula> buscar(String modalidade, String dia, OffsetDateTime inicioDia, OffsetDateTime fimDia) throws SQLException {
        String sql = SELECT + "where a.modalidade ilike ? and (? = 'Todos' or a.dia_semana = ?)" + ORDEM;
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            ps.setObject(1, inicioDia);
            ps.setObject(2, fimDia);
            ps.setString(3, Banco.parteDe(modalidade));
            ps.setString(4, dia);
            ps.setString(5, dia);
            return lista(ps);
        }
    }

    public List<Aula> todas(OffsetDateTime inicioDia, OffsetDateTime fimDia) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement(SELECT + ORDEM)) {
            ps.setObject(1, inicioDia);
            ps.setObject(2, fimDia);
            return lista(ps);
        }
    }

    private List<Aula> lista(PreparedStatement ps) throws SQLException {
        List<Aula> r = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                r.add(ler(rs));
            }
        }
        return r;
    }

    public int salvar(Aula a) throws SQLException {
        if (a.id() == null) {
            String sql = "insert into aula (modalidade, dia_semana, horario, instrutor_id, vagas) values (?, ?, ?, ?, ?) returning id";
            try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
                preencher(ps, a);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
        String sql = "update aula set modalidade = ?, dia_semana = ?, horario = ?, instrutor_id = ?, vagas = ? where id = ?";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            preencher(ps, a);
            ps.setInt(6, a.id());
            ps.executeUpdate();
        }
        return a.id();
    }

    private void preencher(PreparedStatement ps, Aula a) throws SQLException {
        ps.setString(1, a.modalidade());
        ps.setString(2, a.diaSemana());
        ps.setTime(3, java.sql.Time.valueOf(a.horario()));
        if (a.instrutorId() == null) {
            ps.setNull(4, java.sql.Types.INTEGER);
        } else {
            ps.setInt(4, a.instrutorId());
        }
        ps.setInt(5, a.vagas());
    }

    public void excluir(int id) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement("delete from aula where id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
