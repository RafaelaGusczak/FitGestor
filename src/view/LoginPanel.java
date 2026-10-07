package view;

import controller.AuthController;
import db.Banco;
import model.Usuario;
import util.Erros;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class LoginPanel extends JPanel {

    private final AuthController auth = new AuthController();
    private final JTextField usuario = Tema.campo("");
    private final JPasswordField senha = Tema.campoSenha("");
    private final JLabel mensagem = Tema.rotulo(" ", 13, false, Tema.VERMELHO);
    private JButton entrar;

    public LoginPanel(Consumer<Usuario> aoEntrar) {
        setLayout(new GridLayout(1, 2));
        add(painelMarca());
        add(painelFormulario(aoEntrar));
    }

    private JPanel painelMarca() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Tema.NAVY);

        JPanel col = Tema.colunaTransparente();
        col.add(Tema.rotulo("FitGestor", 46, true, Color.WHITE));
        col.add(Box.createVerticalStrut(10));
        col.add(Tema.rotulo("Sistema de gestão de academia", 18, false, new Color(0x94A3B8)));
        col.add(Box.createVerticalStrut(14));
        JPanel barra = new JPanel();
        barra.setBackground(Tema.PRIMARIA);
        barra.setMaximumSize(new Dimension(64, 4));
        barra.setPreferredSize(new Dimension(64, 4));
        barra.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(barra);
        col.add(Box.createVerticalStrut(14));
        for (String s : new String[]{"Alunos, planos e matrículas", "Controle de mensalidades",
                "Check-in nas aulas", "Relatórios de inadimplência e frequência"}) {
            col.add(Tema.rotulo("•  " + s, 15, false, new Color(0xCBD5E1)));
            col.add(Box.createVerticalStrut(8));
        }
        p.add(col);
        return p;
    }

    private JPanel painelFormulario(Consumer<Usuario> aoEntrar) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);

        Tema.Cartao col = new Tema.Cartao(0) {
            @Override
            protected void paintComponent(Graphics g) { /* sem fundo nem borda */ }
        };
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.add(Tema.rotulo("Entrar", 30, true, Tema.TEXTO));
        col.add(Box.createVerticalStrut(8));
        col.add(Tema.rotulo("Acesse com seu usuário e senha", 13, false, Tema.APAGADO));
        col.add(Box.createVerticalStrut(22));
        col.add(Tema.larguraTotal(Tema.rotulado("Usuário", usuario)));
        col.add(Box.createVerticalStrut(16));
        col.add(Tema.larguraTotal(Tema.rotulado("Senha", senha)));
        col.add(Box.createVerticalStrut(8));
        col.add(Tema.larguraTotal(mensagem));
        col.add(Box.createVerticalStrut(8));
        entrar = Tema.botao("Entrar", Tema.TipoBotao.PRIMARIO);
        Runnable tentar = () -> tentarEntrar(aoEntrar);
        entrar.addActionListener(e -> tentar.run());
        usuario.addActionListener(e -> senha.requestFocusInWindow());
        senha.addActionListener(e -> tentar.run());
        col.add(Tema.larguraTotal(entrar));
        col.add(Box.createVerticalStrut(16));
        col.add(Tema.rotulo("Esqueci minha senha", 13, true, Tema.PRIMARIA));
        col.setPreferredSize(new Dimension(400, col.getPreferredSize().height));
        p.add(col);
        return p;
    }

    private void tentarEntrar(Consumer<Usuario> aoEntrar) {
        if (!entrar.isEnabled()) {
            return;
        }
        String login = usuario.getText();
        String pwd = new String(senha.getPassword());
        entrar.setEnabled(false);
        entrar.setText("Entrando...");
        mostrar(" ");
        Banco.async(() -> auth.entrar(login, pwd), u -> {
            entrar.setEnabled(true);
            entrar.setText("Entrar");
            limpar();
            aoEntrar.accept(u);
        }, e -> {
            entrar.setEnabled(true);
            entrar.setText("Entrar");
            senha.setText("");
            mostrar(Erros.traduzir(e));
        });
    }

    private void mostrar(String texto) {
        String html = texto.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
        mensagem.setText("<html><body style='width:380px'>" + html + "</body></html>");
    }

    /** Volta o formulário ao estado inicial (ao entrar e ao sair). */
    public void limpar() {
        usuario.setText("");
        senha.setText("");
        mostrar(" ");
    }
}
