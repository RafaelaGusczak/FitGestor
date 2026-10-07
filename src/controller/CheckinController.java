package controller;

import dao.AlunoDao;
import dao.CheckinDao;
import db.Banco;
import model.Aluno;
import model.Aula;
import model.SituacaoAluno;
import util.Datas;
import util.Formato;
import util.RegraNegocioException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class CheckinController {

    private final AlunoDao alunos = new AlunoDao();
    private final CheckinDao checkins = new CheckinDao();

    /** Procura o aluno por nome ou CPF e devolve sua situação. Roda na thread do banco. */
    public SituacaoAluno consultar(String nomeOuCpf) throws SQLException {
        String texto = nomeOuCpf == null ? "" : nomeOuCpf.trim();
        if (texto.isEmpty()) {
            throw new RegraNegocioException("Digite o nome ou CPF do aluno.");
        }
        List<Aluno> achados = alunos.buscar(texto, "Todos");
        if (achados.isEmpty()) {
            throw new RegraNegocioException("Nenhum aluno encontrado para \"" + texto + "\".");
        }
        if (achados.size() > 1) {
            throw new RegraNegocioException("Mais de um aluno encontrado (" + achados.size()
                    + "). Digite o nome completo ou o CPF.");
        }
        return checkins.situacao(achados.get(0), Datas.hoje());
    }

    /** Valida as regras e grava o check-in. Roda na thread do banco. */
    public void registrar(SituacaoAluno situacao, Aula aula) throws SQLException {
        if (situacao == null) {
            throw new RegraNegocioException("Busque o aluno antes de confirmar.");
        }
        if (aula == null) {
            throw new RegraNegocioException("Escolha a aula.");
        }
        Aluno aluno = situacao.aluno();
        LocalDate hoje = Datas.hoje();
        if (!aluno.situacao().equals("Ativo")) {
            throw new RegraNegocioException(aluno.nome() + " está inativo.");
        }
        if (!situacao.matriculaAtiva()) {
            throw new RegraNegocioException(aluno.nome() + " não tem matrícula ativa.");
        }
        if (!situacao.pagamentoEmDia()) {
            throw new RegraNegocioException(aluno.nome() + " tem mensalidade em atraso.");
        }
        if (!aula.diaSemana().equals(Formato.diaSemana(hoje))) {
            throw new RegraNegocioException("Essa aula é de " + aula.diaSemana() + "; o check-in só vale no dia da aula.");
        }
        Banco.transacao(() -> {
            OffsetDateTime inicio = Datas.inicioDoDia(hoje);
            OffsetDateTime fim = Datas.fimDoDia(hoje);
            if (checkins.jaFez(aluno.id(), aula.id(), inicio, fim)) {
                throw new RegraNegocioException(aluno.nome() + " já fez check-in nessa aula hoje.");
            }
            if (checkins.contarNaAula(aula.id(), inicio, fim) >= aula.vagas()) {
                throw new RegraNegocioException("Não há vagas nessa aula.");
            }
            checkins.inserir(aluno.id(), aula.id(), OffsetDateTime.now());
            return null;
        });
    }
}
