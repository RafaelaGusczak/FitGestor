package dao;

import db.Banco;
import model.Aluno;
import model.Checkin;
import model.SituacaoAluno;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class CheckinDao {

    private static final String SELECT =
            "select c.data_hora, a.nome as aluno, au.modalidade, i.nome as instrutor "
            + "from checkin c join aluno a on a.id = c.aluno_id join aula au on au.id = c.aula_id "
            + "left join instrutor i on i.id = au.instrutor_id ";

    private Checkin ler(ResultSet rs) throws SQLException {
        OffsetDateTime quando = rs.getObject("data_hora", OffsetDateTime.class);
        return new Checkin(quando.atZoneSameInstant(ZoneId.systemDefault()).toLocalTime(),
                rs.getString("aluno"), rs.getString("modalidade"), rs.getString("instrutor"));
    }

    public List<Checkin> doPeriodo(OffsetDateTime inicio, OffsetDateTime fim) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement(
                SELECT + "where c.data_hora >= ? and c.data_hora < ? order by c.data_hora desc")) {
            ps.setObject(1, inicio);
            ps.setObject(2, fim);
            return lista(ps);
        }
    }

    public List<Checkin> ultimos(int limite) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement(SELECT + "order by c.data_hora desc limit ?")) {
            ps.setInt(1, limite);
            return lista(ps);
        }
    }

    private List<Checkin> lista(PreparedStatement ps) throws SQLException {
        List<Checkin> r = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                r.add(ler(rs));
            }
        }
        return r;
    }

    public int contar(OffsetDateTime inicio, OffsetDateTime fim) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement(
                "select count(*) from checkin where data_hora >= ? and data_hora < ?")) {
            ps.setObject(1, inicio);
            ps.setObject(2, fim);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean jaFez(int alunoId, int aulaId, OffsetDateTime inicio, OffsetDateTime fim) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement(
                "select 1 from checkin where aluno_id = ? and aula_id = ? and data_hora >= ? and data_hora < ? limit 1")) {
            ps.setInt(1, alunoId);
            ps.setInt(2, aulaId);
            ps.setObject(3, inicio);
            ps.setObject(4, fim);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int contarNaAula(int aulaId, OffsetDateTime inicio, OffsetDateTime fim) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement(
                "select count(*) from checkin where aula_id = ? and data_hora >= ? and data_hora < ?")) {
            ps.setInt(1, aulaId);
            ps.setObject(2, inicio);
            ps.setObject(3, fim);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void inserir(int alunoId, int aulaId, OffsetDateTime quando) throws SQLException {
        try (PreparedStatement ps = Banco.conexao().prepareStatement(
                "insert into checkin (aluno_id, aula_id, data_hora) values (?, ?, ?)")) {
            ps.setInt(1, alunoId);
            ps.setInt(2, aulaId);
            ps.setObject(3, quando);
            ps.executeUpdate();
        }
    }

    /** Matrícula vigente e mensalidades em dia, para decidir se o aluno pode entrar. */
    public SituacaoAluno situacao(Aluno aluno, LocalDate hoje) throws SQLException {
        String plano = null;
        LocalDate validoAte = null;
        String sqlMatricula = "select pl.nome, m.data_termino from matricula m join plano pl on pl.id = m.plano_id "
                + "where m.aluno_id = ? and m.status = 'Ativa' and m.data_inicio <= ? and m.data_termino >= ? "
                + "order by m.data_termino desc limit 1";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sqlMatricula)) {
            ps.setInt(1, aluno.id());
            Sql.data(ps, 2, hoje);
            Sql.data(ps, 3, hoje);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    plano = rs.getString("nome");
                    validoAte = Sql.data(rs, "data_termino");
                }
            }
        }
        String sqlAtraso = "select 1 from pagamento p join matricula m on m.id = p.matricula_id "
                + "where m.aluno_id = ? and m.status <> 'Cancelada' and p.data_pagamento is null and p.vencimento < ? limit 1";
        boolean emDia;
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sqlAtraso)) {
            ps.setInt(1, aluno.id());
            Sql.data(ps, 2, hoje);
            try (ResultSet rs = ps.executeQuery()) {
                emDia = !rs.next();
            }
        }
        return new SituacaoAluno(aluno, plano, validoAte, plano != null, emDia);
    }
}
