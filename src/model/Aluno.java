package model;

import java.time.LocalDate;

/** id == null: ainda não gravado. */
public record Aluno(Integer id, String nome, String cpf, LocalDate nascimento,
                    String telefone, String email, String situacao) { }
