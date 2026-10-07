package dao;

import db.Banco;
import model.Pagamento;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PagamentoDao {

    /** Totais do painel de pagamentos. */
    public record Resumo(BigDecimal recebidoNoMes, BigDecimal aReceberNoMes, int aReceberQtd,
                         BigDecimal emAtraso, int emAtrasoQtd) { }

    private Pagamento ler(ResultSet rs) throws SQLException {
        return new Pagamento(rs.getInt("id"), rs.getInt("matricula_id"), rs.getString("aluno"), rs.getString("plano"),
                Sql.data(rs, "vencimento"), rs.getBigDecimal("valor"), rs.getString("forma"),
                Sql.data(rs, "data_pagamento"));
    }

    /**
     * Lista mensalidades. situacao: Todas | Pago | Pendente | Atrasado.
     * venceDe/venceAte (inclusive) podem ser null = sem limite.
     */
    public List<Pagamento> buscar(String aluno, String situacao, LocalDate venceDe, LocalDate venceAte,
                                  LocalDate hoje) throws SQLException {
        String sql = "select p.id, p.matricula_id, a.nome as aluno, pl.nome as plano, p.vencimento, p.valor, "
                + "p.forma, p.data_pagamento "
                + "from pagamento p join matricula m on m.id = p.matricula_id "
                + "join aluno a on a.id = m.aluno_id join plano pl on pl.id = m.plano_id "
                + "where a.nome ilike ? "
                + "and (?::date is null or p.vencimento >= ?::date) "
                + "and (?::date is null or p.vencimento <= ?::date) "
                + "and (? = 'Todas' "
                + "  or (? = 'Pago' and p.data_pagamento is not null) "
                + "  or (? = 'Pendente' and p.data_pagamento is null and p.vencimento >= ?) "
                + "  or (? = 'Atrasado' and p.data_pagamento is null and p.vencimento < ?)) "
                + "order by p.vencimento, a.nome limit 500";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, Banco.parteDe(aluno));
            Sql.data(ps, i++, venceDe);
            Sql.data(ps, i++, venceDe);
            Sql.data(ps, i++, venceAte);
            Sql.data(ps, i++, venceAte);
            ps.setString(i++, situacao);
            ps.setString(i++, situacao);
            ps.setString(i++, situacao);
            Sql.data(ps, i++, hoje);
            ps.setString(i++, situacao);
            Sql.data(ps, i, hoje);
            List<Pagamento> r = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    r.add(ler(rs));
                }
            }
            return r;
        }
    }

    public Resumo resumo(LocalDate hoje, LocalDate inicioMes, LocalDate fimMes) throws SQLException {
        String sql = "select "
                + "coalesce(sum(valor) filter (where data_pagamento between ? and ?), 0) as recebido, "
                + "coalesce(sum(valor) filter (where data_pagamento is null and vencimento between ? and ?), 0) as a_receber, "
                + "count(*) filter (where data_pagamento is null and vencimento between ? and ?) as a_receber_qtd, "
                + "coalesce(sum(valor) filter (where data_pagamento is null and vencimento < ?), 0) as atraso, "
                + "count(*) filter (where data_pagamento is null and vencimento < ?) as atraso_qtd "
                + "from pagamento";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            Sql.data(ps, 1, inicioMes);
            Sql.data(ps, 2, fimMes);
            Sql.data(ps, 3, hoje);
            Sql.data(ps, 4, fimMes);
            Sql.data(ps, 5, hoje);
            Sql.data(ps, 6, fimMes);
            Sql.data(ps, 7, hoje);
            Sql.data(ps, 8, hoje);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Resumo(rs.getBigDecimal("recebido"), rs.getBigDecimal("a_receber"),
                        rs.getInt("a_receber_qtd"), rs.getBigDecimal("atraso"), rs.getInt("atraso_qtd"));
            }
        }
    }

    /** Marca como paga. Só atualiza se ainda estiver em aberto; devolve false se já estava paga. */
    public boolean darBaixa(int id, String forma, LocalDate data) throws SQLException {
        String sql = "update pagamento set forma = ?, data_pagamento = ? where id = ? and data_pagamento is null";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            ps.setString(1, forma);
            Sql.data(ps, 2, data);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Cria as mensalidades que faltam das matrículas ativas (uma por mês, a partir do início, até o término).
     * Não duplica as que já existem. matriculaId == null: todas as matrículas ativas.
     */
    public int gerarMensalidades(Integer matriculaId) throws SQLException {
        String sql = "insert into pagamento (matricula_id, vencimento, valor) "
                + "select m.id, (m.data_inicio + make_interval(months => g))::date, m.valor_mensal "
                + "from matricula m cross join lateral generate_series(0, 600) g "
                + "where m.status = 'Ativa' and (?::int is null or m.id = ?::int) "
                + "and (m.data_inicio + make_interval(months => g))::date < m.data_termino "
                + "and not exists (select 1 from pagamento x where x.matricula_id = m.id "
                + "  and x.vencimento = (m.data_inicio + make_interval(months => g))::date)";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            if (matriculaId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, matriculaId);
                ps.setInt(2, matriculaId);
            }
            return ps.executeUpdate();
        }
    }

    /** Ao cancelar uma matrícula: apaga as mensalidades futuras ainda não pagas. */
    public int removerFuturasEmAberto(int matriculaId, LocalDate hoje) throws SQLException {
        String sql = "delete from pagamento where matricula_id = ? and data_pagamento is null and vencimento >= ?";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            ps.setInt(1, matriculaId);
            Sql.data(ps, 2, hoje);
            return ps.executeUpdate();
        }
    }
}
