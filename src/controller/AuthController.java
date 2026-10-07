package controller;

import dao.UsuarioDao;
import model.Usuario;
import util.RegraNegocioException;

import java.sql.SQLException;

public class AuthController {

    private final UsuarioDao dao = new UsuarioDao();

    /** Roda na thread do banco. */
    public Usuario entrar(String login, String senha) throws SQLException {
        if (login == null || login.isBlank() || senha == null || senha.isEmpty()) {
            throw new RegraNegocioException("Informe usuário e senha.");
        }
        Usuario u = dao.autenticar(login.trim(), senha);
        if (u == null) {
            throw new RegraNegocioException("Usuário ou senha inválidos.");
        }
        return u;
    }
}
