package dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

/** Pequenos utilitários para ler/gravar tipos opcionais no JDBC. */
final class Sql {

    private Sql() { }

    static LocalDate data(ResultSet rs, String coluna) throws SQLException {
        Date d = rs.getDate(coluna);
        return d == null ? null : d.toLocalDate();
    }

    static LocalTime hora(ResultSet rs, String coluna) throws SQLException {
        java.sql.Time t = rs.getTime(coluna);
        return t == null ? null : t.toLocalTime();
    }

    static Integer inteiro(ResultSet rs, String coluna) throws SQLException {
        int v = rs.getInt(coluna);
        return rs.wasNull() ? null : v;
    }

    static void data(PreparedStatement ps, int indice, LocalDate d) throws SQLException {
        if (d == null) {
            ps.setNull(indice, java.sql.Types.DATE);
        } else {
            ps.setDate(indice, Date.valueOf(d));
        }
    }
}
