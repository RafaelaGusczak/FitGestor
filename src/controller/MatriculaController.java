package controller;

import dao.MatriculaDao;
import dao.PagamentoDao;
import db.Banco;
import model.Aluno;
import model.Matricula;
import model.Plano;
import util.Formato;
import util.RegraNegocioException;

import java.sql.SQLException;
import java.time.LocalDate;

public class MatriculaController {

    private final MatriculaDao matriculas = new MatriculaDao();
    private final PagamentoDao pagamentos = new PagamentoDao();

    public static LocalDate calcularTermino(LocalDate inicio, Plano plano) {
        return inicio.plusMonths(plano.duracaoMeses());
    }

    /** Cria a matrícula e, se ela já nascer ativa, todas as mensalidades — tudo ou nada. Roda na thread do banco. */
    public int criar(Aluno aluno, Plano plano, LocalDate inicio, String status) throws SQLException {
        if (aluno == null || plano == null) {
            throw new RegraNegocioException("Escolha o aluno e o plano.");
        }
        if (!"Ativo".equals(aluno.situacao())) {
            throw new RegraNegocioException("O aluno está inativo.");
        }
        LocalDate termino = calcularTermino(inicio, plano);
        return Banco.transacao(() -> {
            if (status.equals("Ativa") && matriculas.existeAtivaSobreposta(aluno.id(), inicio, termino)) {
                throw new RegraNegocioException(aluno.nome() + " já tem uma matrícula ativa nesse período.");
            }
            int id = matriculas.inserir(new Matricula(null, aluno.id(), aluno.nome(), plano.id(), plano.nome(),
                    inicio, termino, plano.valorMensal(), status));
            if (status.equals("Ativa")) {
                pagamentos.gerarMensalidades(id);
            }
            return id;
        });
    }

    /** Trancar/cancelar/reativar. Cancelar apaga as mensalidades futuras em aberto. Roda na thread do banco. */
    public void alterarStatus(Matricula m, String novoStatus, LocalDate hoje) throws SQLException {
        if (m.status().equals(novoStatus)) {
            return;
        }
        if (m.status().equals("Cancelada")) {
            throw new RegraNegocioException("Matrícula cancelada não pode ser reativada. Crie uma nova matrícula.");
        }
        Banco.transacao(() -> {
            if (novoStatus.equals("Ativa") && matriculas.existeAtivaSobreposta(m.alunoId(), m.inicio(), m.termino())) {
                throw new RegraNegocioException(m.alunoNome() + " já tem outra matrícula ativa nesse período.");
            }
            matriculas.atualizarStatus(m.id(), novoStatus);
            if (novoStatus.equals("Cancelada")) {
                pagamentos.removerFuturasEmAberto(m.id(), hoje);
            } else if (novoStatus.equals("Ativa")) {
                pagamentos.gerarMensalidades(m.id());
            }
            return null;
        });
    }

    public static String descreverTotal(Plano plano) {
        return plano.duracaoMeses() + " × " + Formato.moeda(plano.valorMensal());
    }
}
