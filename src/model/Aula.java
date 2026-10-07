package model;

import java.time.LocalTime;

/** ocupadas = check-ins de hoje nessa aula (só interessa no dia da própria aula). */
public record Aula(Integer id, String modalidade, String diaSemana, LocalTime horario,
                   Integer instrutorId, String instrutorNome, int vagas, int ocupadas) {

    public int vagasLivres() {
        return Math.max(0, vagas - ocupadas);
    }
}
