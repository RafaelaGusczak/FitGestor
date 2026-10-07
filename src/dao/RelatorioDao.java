package dao;

import db.Banco;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Consultas gerenciais. planoId == null significa "todos os planos". */
public class RelatorioDao {

    public record Inadimplente(String aluno, String telefone, String plano, LocalDate vencimento, BigDecimal valor) { }

    public record Frequencia(String aluno, String plano, int checkins, LocalDate ultimaVisita) { }

    public record AVencer(String aluno, String plano, LocalDate termino) { }

    private static void planoId(PreparedStatement ps, int indice, Integer planoId) throws SQLException {
        if (planoId == null) {
            ps.setNull(indice, java.sql.Types.INTEGER);
        } else {
            ps.setInt(indice, planoId);
        }
    }

    /** Mensalidades em aberto com vencimento entre de e ate (inclusive) e antes de hoje. */
    public List<Inadimplente> inadimplentes(LocalDate de, LocalDate ate, Integer planoId, LocalDate hoje) throws SQLException {
        String sql = "select a.nome, a.telefone, pl.nome as plano, p.vencimento, p.valor "
                + "from pagamento p join matricula m on m.id = p.matricula_id "
                + "join aluno a on a.id = m.aluno_id join plano pl on pl.id = m.plano_id "
                + "where p.data_pagamento is null and p.vencimento < ? and p.vencimento between ? and ? "
                + "and (?::int is null or pl.id = ?::int) order by p.vencimento, a.nome";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            Sql.data(ps, 1, hoje);
            Sql.data(ps, 2, de);
            Sql.data(ps, 3, ate);
            planoId(ps, 4, planoId);
            planoId(ps, 5, planoId);
            List<Inadimplente> r = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    r.add(new Inadimplente(rs.getString("nome"), rs.getString("telefone"), rs.getString("plano"),
                            Sql.data(rs, "vencimento"), rs.getBigDecimal("valor")));
                }
            }
            return r;
        }
    }

    /** Check-ins de cada aluno ativo no período [inicio, fim). O plano é o da matrícula ativa mais recente. */
    public List<Frequencia> frequencia(OffsetDateTime inicio, OffsetDateTime fim, Integer planoId) throws SQLException {
        String sql = "select a.nome, pl.nome as plano, count(c.id) as qtd, "
                + "(select max(u.data_hora) from checkin u where u.aluno_id = a.id) as ultima "
                + "from aluno a "
                + "left join checkin c on c.aluno_id = a.id and c.data_hora >= ? and c.data_hora < ? "
                + "left join lateral (select p2.id, p2.nome from matricula m join plano p2 on p2.id = m.plano_id "
                + "  where m.aluno_id = a.id and m.status = 'Ativa' order by m.data_termino desc limit 1) pl on true "
                + "where a.situacao = 'Ativo' and (?::int is null or pl.id = ?::int) "
                + "group by a.id, a.nome, pl.id, pl.nome order by qtd desc, a.nome";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            ps.setObject(1, inicio);
            ps.setObject(2, fim);
            planoId(ps, 3, planoId);
            planoId(ps, 4, planoId);
            List<Frequencia> r = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OffsetDateTime ultima = rs.getObject("ultima", OffsetDateTime.class);
                    r.add(new Frequencia(rs.getString("nome"), rs.getString("plano"), rs.getInt("qtd"),
                            ultima == null ? null : ultima.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()));
                }
            }
            return r;
        }
    }

    /** Matrículas ativas que terminam entre de e ate (inclusive). */
    public List<AVencer> aVencer(LocalDate de, LocalDate ate, Integer planoId) throws SQLException {
        String sql = "select a.nome, pl.nome as plano, m.data_termino "
                + "from matricula m join aluno a on a.id = m.aluno_id join plano pl on pl.id = m.plano_id "
                + "where m.status = 'Ativa' and m.data_termino between ? and ? "
                + "and (?::int is null or pl.id = ?::int) order by m.data_termino, a.nome";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            Sql.data(ps, 1, de);
            Sql.data(ps, 2, ate);
            planoId(ps, 3, planoId);
            planoId(ps, 4, planoId);
            List<AVencer> r = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    r.add(new AVencer(rs.getString("nome"), rs.getString("plano"), Sql.data(rs, "data_termino")));
                }
            }
            return r;
        }
    }
}
