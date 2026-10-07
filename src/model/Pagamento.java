package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Pagamento(int id, int matriculaId, String alunoNome, String planoNome, LocalDate vencimento,
                        BigDecimal valor, String forma, LocalDate dataPagamento) {

    public String situacao(LocalDate hoje) {
        if (dataPagamento != null) {
            return "Pago";
        }
        return vencimento.isBefore(hoje) ? "Atrasado" : "Pendente";
    }
}
