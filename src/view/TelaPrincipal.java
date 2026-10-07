package view;

import model.Usuario;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Menu lateral + área de conteúdo que troca de página (CardLayout). */
public class TelaPrincipal extends JPanel {

    private final CardLayout cartas = new CardLayout();
    private final JPanel conteudo = new JPanel(cartas);
    private final Map<String, ItemMenu> itens = new LinkedHashMap<>();
    private final Map<String, Pagina> paginas = new LinkedHashMap<>();
    private final JLabel nomeUsuario = Tema.rotulo(" ", 13, true, Color.WHITE);
    private final JLabel emailUsuario = Tema.rotulo(" ", 11, false, new Color(0x94A3B8));

    public TelaPrincipal(Runnable aoSair) {
        setLayout(new BorderLayout());

        paginas.put("Início", new InicioPagina(this::navegar));
        paginas.put("Alunos", new AlunosPagina());
        paginas.put("Planos", new PlanosPagina());
        paginas.put("Instrutores", new InstrutoresPagina());
        paginas.put("Aulas", new AulasPagina());
        paginas.put("Matrículas", new MatriculasPagina());
        paginas.put("Pagamentos", new PagamentosPagina());
        paginas.put("Check-in", new CheckinPagina());
        paginas.put("Relatórios", new RelatoriosPagina());

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Tema.NAVY);
        menu.setBorder(BorderFactory.createEmptyBorder(24, 16, 24, 16));
        menu.setPreferredSize(new Dimension(240, 0));

        JPanel logo = Tema.colunaTransparente();
        logo.setBorder(BorderFactory.createEmptyBorder(0, 12, 24, 0));
        logo.add(Tema.rotulo("FitGestor", 22, true, Color.WHITE));
        logo.add(Box.createVerticalStrut(2));
        logo.add(Tema.rotulo("Gestão de academia", 12, false, new Color(0x94A3B8)));
        menu.add(logo);

        for (Map.Entry<String, Pagina> e : paginas.entrySet()) {
            conteudo.add(e.getValue(), e.getKey());
            ItemMenu item = new ItemMenu(e.getKey());
            item.addActionListener(ev -> navegar(e.getKey()));
            itens.put(e.getKey(), item);
            menu.add(item);
            menu.add(Box.createVerticalStrut(4));
        }

        menu.add(Box.createVerticalGlue());
        JPanel usuario = Tema.colunaTransparente();
        usuario.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 0));
        usuario.add(nomeUsuario);
        usuario.add(emailUsuario);
        menu.add(usuario);

        JButton sair = new JButton("Sair");
        sair.setFont(Tema.fonte(13, false));
        sair.setForeground(new Color(0xCBD5E1));
        sair.setContentAreaFilled(false);
        sair.setFocusPainted(false);
        sair.setHorizontalAlignment(SwingConstants.LEFT);
        sair.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sair.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0x334155), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        sair.setAlignmentX(Component.LEFT_ALIGNMENT);
        sair.setMaximumSize(new Dimension(Integer.MAX_VALUE, sair.getPreferredSize().height));
        sair.addActionListener(e -> aoSair.run());
        menu.add(sair);

        add(menu, BorderLayout.WEST);
        add(conteudo, BorderLayout.CENTER);
        itens.get("Início").setSelecionado(true);
    }

    public void setUsuario(Usuario u) {
        nomeUsuario.setText(u.nome());
        emailUsuario.setText(u.email() == null ? u.login() : u.email());
    }

    /** Mostra a página e pede que ela busque os dados atuais no banco. */
    public void navegar(String pagina) {
        itens.forEach((nome, item) -> item.setSelecionado(nome.equals(pagina)));
        cartas.show(conteudo, pagina);
        paginas.get(pagina).aoExibir();
    }

    /** Botão do menu lateral; o item atual fica azul. */
    private static class ItemMenu extends JButton {
        private boolean selecionado;

        ItemMenu(String texto) {
            super(texto);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height + 6));
            setSelecionado(false);
        }

        void setSelecionado(boolean s) {
            selecionado = s;
            setFont(Tema.fonte(13, s));
            setForeground(s ? Color.WHITE : new Color(0xCBD5E1));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selecionado) {
                g2.setColor(Tema.PRIMARIA);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(0x1E293B));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
