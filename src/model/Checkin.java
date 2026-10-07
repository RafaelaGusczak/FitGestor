package model;

import java.time.LocalTime;

/** Linha de listagem de check-in (já com nomes). */
public record Checkin(LocalTime hora, String aluno, String aula, String instrutor) { }
