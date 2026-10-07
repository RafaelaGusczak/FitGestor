package util;

import java.sql.SQLException;

/** Traduz exceções técnicas em mensagens que o usuário entende. */
public final class Erros {

    private Erros() { }

    public static String traduzir(Throwable e) {
        if (e instanceof RegraNegocioException) {
            return e.getMessage();
        }
        if (e instanceof SQLException sql) {
            String estado = sql.getSQLState() == null ? "" : sql.getSQLState();
            if (estado.equals("23505")) {
                return "Já existe um registro com esse valor (CPF, nome ou login repetido).";
            }
            if (estado.equals("23503")) {
                return "Este registro está em uso (matrículas, aulas, pagamentos ou check-ins) e não pode ser excluído.";
            }
            if (estado.startsWith("08") || estado.equals("28P01") || estado.equals("28000") || estado.isEmpty()) {
                return "Não foi possível acessar o banco de dados.\n" + sql.getMessage()
                        + "\n\nConfira o arquivo db.properties e sua conexão com a internet.";
            }
            return "Erro no banco de dados: " + sql.getMessage();
        }
        return "Erro inesperado: " + e;
    }
}
