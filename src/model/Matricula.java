package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Matricula(Integer id, int alunoId, String alunoNome, int planoId, String planoNome,
                        LocalDate inicio, LocalDate termino, BigDecimal valorMensal, String status) {

    /** Matrícula "Ativa" cujo término já passou aparece como "Vencida". */
    public String statusExibido(LocalDate hoje) {
        return status.equals("Ativa") && termino.isBefore(hoje) ? "Vencida" : status;
    }
}
