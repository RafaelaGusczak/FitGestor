package dao;

import db.Banco;
import model.Usuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDao {

    /** Confere a senha no próprio banco (bcrypt via pgcrypto). Devolve null se login/senha não conferem. */
    public Usuario autenticar(String login, String senha) throws SQLException {
        String sql = "select id, login, nome, email from usuario "
                + "where login = ? and senha_hash = extensions.crypt(?, senha_hash)";
        try (PreparedStatement ps = Banco.conexao().prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, senha);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Usuario(rs.getInt("id"), rs.getString("login"),
                        rs.getString("nome"), rs.getString("email")) : null;
            }
        }
    }
}
