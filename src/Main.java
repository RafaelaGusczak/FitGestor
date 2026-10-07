import view.LoginPanel;
import view.TelaPrincipal;

import javax.swing.*;
import java.awt.*;

/** Ponto de entrada: uma janela que alterna entre o login e a tela principal. */
public class Main {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(Main::criarJanela);
    }

    private static void criarJanela() {
        JFrame janela = new JFrame("FitGestor — Sistema de Gestão de Academia");
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CardLayout cartas = new CardLayout();
        JPanel raiz = new JPanel(cartas);

        LoginPanel[] login = new LoginPanel[1];
        TelaPrincipal principal = new TelaPrincipal(() -> {
            login[0].limpar();
            cartas.show(raiz, "login");
        });
        login[0] = new LoginPanel(usuario -> {
            principal.setUsuario(usuario);
            cartas.show(raiz, "app");
            principal.navegar("Início");
        });
        raiz.add(login[0], "login");
        raiz.add(principal, "app");

        janela.setContentPane(raiz);
        janela.setSize(1280, 800);
        janela.setMinimumSize(new Dimension(1100, 700));
        janela.setLocationRelativeTo(null);
        janela.setVisible(true);
    }
}
