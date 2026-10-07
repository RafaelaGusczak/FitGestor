package model;

import java.time.LocalDate;

/** O que a recepção precisa saber para liberar a entrada. plano/validoAte são null sem matrícula ativa. */
public record SituacaoAluno(Aluno aluno, String plano, LocalDate validoAte,
                            boolean matriculaAtiva, boolean pagamentoEmDia) { }
