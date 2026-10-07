package util;

/** Erro de validação/regra de negócio: a mensagem já é própria para mostrar ao usuário. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
