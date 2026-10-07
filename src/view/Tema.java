package view;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.Map;

/**
 * Cores, fontes e fábrica de componentes reutilizáveis das telas.
 * Centralizar o visual aqui evita repetir estilo em cada tela.
 */
public final class Tema {

    private Tema() { }

    // ---------- Cores ----------
    public static final Color NAVY = new Color(0x0F172A);
    public static final Color PRIMARIA = new Color(0x2563EB);
    public static final Color PRIMARIA_ESCURA = new Color(0x1D4ED8);
    public static final Color FUNDO = new Color(0xF1F5F9);
    public static final Color LINHA = new Color(0xE2E8F0);
    public static final Color BORDA_CAMPO = new Color(0xCBD5E1);
    public static final Color TEXTO = new Color(0x0F172A);
    public static final Color TEXTO_RELEVO = new Color(0x334155);
    public static final Color APAGADO = new Color(0x64748B);
    public static final Color VERDE = new Color(0x16A34A);
    public static final Color VERMELHO = new Color(0xDC2626);
    public static final Color LARANJA = new Color(0xD97706);
    public static final Color CABECALHO = new Color(0xF8FAFC);
    public static final Color SELECAO = new Color(0xEFF6FF);

    public enum TipoBotao { PRIMARIO, SECUNDARIO, PERIGO }

    public enum TipoPilula { OK, ALERTA, ERRO, INFO }

    public static Font fonte(int tamanho, boolean negrito) {
        return new Font("SansSerif", negrito ? Font.BOLD : Font.PLAIN, tamanho);
    }

    // ---------- Rótulos ----------
    public static JLabel rotulo(String texto, int tamanho, boolean negrito, Color cor) {
        JLabel l = new JLabel(texto);
        l.setFont(fonte(tamanho, negrito));
        l.setForeground(cor);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    public static JPanel transparente(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static JPanel colunaTransparente() {
        JPanel p = transparente(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /** Impede que um componente cresça além da altura preferida dentro de um BoxLayout vertical. */
    public static <T extends JComponent> T larguraTotal(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    // ---------- Botão ----------
    public static JButton botao(String texto, TipoBotao tipo) {
        return new Botao(texto, tipo);
    }

    private static class Botao extends JButton {
        private final TipoBotao tipo;

        Botao(String texto, TipoBotao tipo) {
            super(texto);
            this.tipo = tipo;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(fonte(13, true));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            setForeground(switch (tipo) {
                case PRIMARIO -> Color.WHITE;
                case SECUNDARIO -> TEXTO;
                case PERIGO -> VERMELHO;
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean sobre = getModel().isRollover();
            switch (tipo) {
                case PRIMARIO -> {
                    g2.setColor(sobre ? PRIMARIA_ESCURA : PRIMARIA);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                case SECUNDARIO -> {
                    g2.setColor(sobre ? CABECALHO : Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(BORDA_CAMPO);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
                case PERIGO -> {
                    g2.setColor(sobre ? new Color(0xFEF2F2) : Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(new Color(0xFCA5A5));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---------- Cartão ----------
    public static class Cartao extends JPanel {
        public Cartao(int margem) {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(margem, margem, margem, margem));
        }

        public Cartao() {
            this(20);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.setColor(LINHA);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Cartão vertical com título (usado em formulários e painéis). */
    public static Cartao cartaoColuna(String titulo, int margem) {
        Cartao c = new Cartao(margem);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        if (titulo != null) {
            c.add(rotulo(titulo, 16, true, TEXTO));
            c.add(Box.createVerticalStrut(14));
        }
        return c;
    }

    // ---------- Campos de formulário ----------
    private static final Border BORDA_INPUT = BorderFactory.createCompoundBorder(
            new LineBorder(BORDA_CAMPO, 1, true), BorderFactory.createEmptyBorder(8, 10, 8, 10));

    private static class CampoTexto extends JTextField {
        private final String dica;

        CampoTexto(String valor, String dica) {
            super(valor);
            this.dica = dica;
            setFont(fonte(13, false));
            setForeground(TEXTO);
            setBorder(BORDA_INPUT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && dica != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(0x94A3B8));
                g2.setFont(getFont());
                Insets in = getInsets();
                g2.drawString(dica, in.left, in.top + g2.getFontMetrics().getAscent());
                g2.dispose();
            }
        }
    }

    public static JTextField campo(String valor) {
        return new CampoTexto(valor, null);
    }

    public static JTextField campoComDica(String dica) {
        return new CampoTexto("", dica);
    }

    public static JTextField campoLeitura(String valor) {
        JTextField c = new CampoTexto(valor, null);
        c.setEditable(false);
        c.setBackground(FUNDO);
        return c;
    }

    public static JPasswordField campoSenha(String valor) {
        JPasswordField c = new JPasswordField(valor);
        c.setFont(fonte(13, false));
        c.setBorder(BORDA_INPUT);
        return c;
    }

    public static JComboBox<String> combo(String... opcoes) {
        JComboBox<String> c = new JComboBox<>(opcoes);
        c.setFont(fonte(13, false));
        c.setBackground(Color.WHITE);
        c.setForeground(TEXTO);
        c.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle area, boolean foco) {
                g.setColor(Color.WHITE);
                g.fillRect(area.x, area.y, area.width, area.height);
            }

            @Override
            protected JButton createArrowButton() {
                BasicArrowButton b = new BasicArrowButton(SwingConstants.SOUTH,
                        Color.WHITE, Color.WHITE, APAGADO, Color.WHITE);
                b.setBorder(BorderFactory.createEmptyBorder());
                return b;
            }
        });
        c.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean sel, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, sel, foco);
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                setBackground(sel && indice >= 0 ? SELECAO : Color.WHITE);
                setForeground(TEXTO);
                return this;
            }
        });
        c.setBorder(new LineBorder(BORDA_CAMPO, 1, true));
        c.setPreferredSize(new Dimension(100, 38));
        c.setBackground(Color.WHITE);
        return c;
    }

    /** Campo com rótulo em cima, como nos protótipos. */
    public static JPanel rotulado(String rotulo, JComponent campo) {
        JPanel p = colunaTransparente();
        p.add(rotulo(rotulo, 12, true, TEXTO_RELEVO));
        p.add(Box.createVerticalStrut(6));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, campo.getPreferredSize().height));
        p.add(campo);
        return larguraTotal(p);
    }

    /** Dois campos lado a lado. */
    public static JPanel par(JComponent a, JComponent b) {
        JPanel p = transparente(new GridLayout(1, 2, 12, 0));
        p.add(a);
        p.add(b);
        return larguraTotal(p);
    }

    /** Monta um cartão de formulário com os itens separados por espaço. */
    public static Cartao formulario(String titulo, JComponent... itens) {
        Cartao c = cartaoColuna(titulo, 20);
        for (JComponent item : itens) {
            c.add(larguraTotal(item));
            c.add(Box.createVerticalStrut(14));
        }
        return c;
    }

    public static JPanel barraAcoes(Runnable salvar, Runnable novo, Runnable excluir) {
        JPanel p = transparente(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton bSalvar = botao("Salvar", TipoBotao.PRIMARIO);
        JButton bNovo = botao("Novo", TipoBotao.SECUNDARIO);
        JButton bExcluir = botao("Excluir", TipoBotao.PERIGO);
        bSalvar.addActionListener(e -> salvar.run());
        bNovo.addActionListener(e -> novo.run());
        bExcluir.addActionListener(e -> excluir.run());
        p.add(bSalvar);
        p.add(bNovo);
        p.add(bExcluir);
        p.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
        return larguraTotal(p);
    }

    // ---------- Pílulas (status) ----------
    public static JLabel pilula(String texto, TipoPilula tipo) {
        Color[] cores = switch (tipo) {
            case OK -> new Color[]{new Color(0xDCFCE7), new Color(0x166534)};
            case ALERTA -> new Color[]{new Color(0xFEF3C7), new Color(0x92400E)};
            case ERRO -> new Color[]{new Color(0xFEE2E2), new Color(0x991B1B)};
            case INFO -> new Color[]{new Color(0xDBEAFE), new Color(0x1E40AF)};
        };
        JLabel l = new JLabel(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cores[0]);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(fonte(12, true));
        l.setForeground(cores[1]);
        l.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        l.setOpaque(false);
        return l;
    }

    private static final Map<String, TipoPilula> STATUS = Map.ofEntries(
            Map.entry("Ativo", TipoPilula.OK), Map.entry("Ativa", TipoPilula.OK),
            Map.entry("Pago", TipoPilula.OK), Map.entry("Inativo", TipoPilula.ERRO),
            Map.entry("Atrasado", TipoPilula.ERRO), Map.entry("Vencida", TipoPilula.ERRO),
            Map.entry("Pendente", TipoPilula.ALERTA), Map.entry("Trancada", TipoPilula.ALERTA));

    // ---------- Tabelas ----------
    private static class RenderizadorCelula extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                                                       boolean foco, int linha, int coluna) {
            String texto = valor == null ? "" : valor.toString();
            Color fundo = sel ? SELECAO : Color.WHITE;
            TipoPilula tipo = STATUS.get(texto);
            if (tipo != null) {
                JPanel p = new JPanel(new GridBagLayout());
                p.setBackground(fundo);
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1;
                gbc.insets = new Insets(0, 14, 0, 0);
                p.add(pilula(texto, tipo), gbc);
                return p;
            }
            super.getTableCellRendererComponent(t, valor, sel, foco, linha, coluna);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            setBackground(fundo);
            setForeground(TEXTO);
            setFont(fonte(13, false));
            if (texto.equals("Dar baixa")) {
                setForeground(PRIMARIA);
                setFont(fonte(13, true));
            } else if (texto.endsWith(" dias") && texto.length() < 10) {
                setForeground(VERMELHO);
                setFont(fonte(13, true));
            }
            return this;
        }
    }

    private static class RenderizadorCabecalho extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                                                       boolean foco, int linha, int coluna) {
            super.getTableCellRendererComponent(t, valor, false, false, linha, coluna);
            setBackground(CABECALHO);
            setForeground(APAGADO);
            setFont(fonte(12, true));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, LINHA),
                    BorderFactory.createEmptyBorder(0, 14, 0, 14)));
            return this;
        }
    }

    /** Tabela estilizada e vazia; use {@link #preencher} para colocar as linhas. */
    public static JTable tabelaVazia(String[] colunas) {
        DefaultTableModel modelo = new DefaultTableModel(new Object[0][], colunas) {
            @Override
            public boolean isCellEditable(int l, int c) {
                return false;
            }
        };
        JTable t = new JTable(modelo);
        t.setRowHeight(44);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(LINHA);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);
        t.setBackground(Color.WHITE);
        t.setSelectionBackground(SELECAO);
        t.setSelectionForeground(TEXTO);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultRenderer(Object.class, new RenderizadorCelula());
        JTableHeader h = t.getTableHeader();
        h.setDefaultRenderer(new RenderizadorCabecalho());
        h.setPreferredSize(new Dimension(0, 38));
        h.setReorderingAllowed(false);
        ajustarLarguras(t, colunas, new Object[0][]);
        return t;
    }

    /** Troca todas as linhas da tabela e reajusta as larguras. */
    public static void preencher(JTable t, Object[][] dados) {
        DefaultTableModel modelo = (DefaultTableModel) t.getModel();
        modelo.setRowCount(0);
        for (Object[] linha : dados) {
            modelo.addRow(linha);
        }
        String[] colunas = new String[modelo.getColumnCount()];
        for (int c = 0; c < colunas.length; c++) {
            colunas[c] = modelo.getColumnName(c);
        }
        ajustarLarguras(t, colunas, dados);
        t.revalidate();
        t.repaint();
    }

    /** Tabela estilizada. selecionada = índice da linha destacada, ou -1. */
    public static JTable tabela(String[] colunas, Object[][] dados, int selecionada) {
        JTable t = tabelaVazia(colunas);
        preencher(t, dados);
        if (selecionada >= 0 && dados.length > selecionada) {
            t.setRowSelectionInterval(selecionada, selecionada);
        }
        return t;
    }

    /** Define a largura de cada coluna pelo maior texto (a coluna ID fica estreita). */
    private static void ajustarLarguras(JTable t, String[] colunas, Object[][] dados) {
        TableColumnModel modelo = t.getColumnModel();
        for (int c = 0; c < colunas.length; c++) {
            int maior = colunas[c].length();
            for (Object[] linha : dados) {
                if (c < linha.length && linha[c] != null) {
                    maior = Math.max(maior, linha[c].toString().length());
                }
            }
            TableColumn coluna = modelo.getColumn(c);
            coluna.setPreferredWidth(Math.max(70, maior * 7 + 28));
            if (colunas[c].equals("ID")) {
                coluna.setPreferredWidth(50);
                coluna.setMaxWidth(60);
            }
        }
    }

    /** Tabela dentro de um scroll, ocupando toda a área disponível. */
    public static JScrollPane tabelaComScroll(String[] colunas, Object[][] dados, int selecionada) {
        JScrollPane sp = new JScrollPane(tabela(colunas, dados, selecionada));
        sp.setBorder(new LineBorder(LINHA, 1));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static JScrollPane comScroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(LINHA, 1));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    /** Envolve uma tabela já criada numa versão de altura natural (sem scroll). */
    public static JPanel tabelaCompacta(JTable t) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(t.getTableHeader(), BorderLayout.NORTH);
        p.add(t, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /** Tabela com altura natural (sem scroll), para usar dentro de cartões. */
    public static JPanel tabelaCompacta(String[] colunas, Object[][] dados) {
        JTable t = tabela(colunas, dados, -1);
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(t.getTableHeader(), BorderLayout.NORTH);
        p.add(t, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    // ---------- Blocos de resumo ----------
    public static Cartao estatistica(String titulo, String valor, String detalhe, Color corValor) {
        Cartao c = cartaoColuna(null, 18);
        c.add(rotulo(titulo, 12, true, APAGADO));
        c.add(Box.createVerticalStrut(6));
        c.add(rotulo(valor, 26, true, corValor));
        c.add(Box.createVerticalStrut(6));
        c.add(rotulo(detalhe, 12, false, APAGADO));
        return c;
    }

    public static JPanel linhaChaveValor(String chave, String valor, boolean destaque) {
        JPanel p = transparente(new BorderLayout());
        p.add(rotulo(chave, 13, false, APAGADO), BorderLayout.WEST);
        p.add(rotulo(valor, destaque ? 16 : 13, destaque, TEXTO), BorderLayout.EAST);
        p.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        return larguraTotal(p);
    }

    /** Igual à outra, mas com o valor num rótulo que a tela pode atualizar depois. */
    public static JPanel linhaChaveValor(String chave, JLabel valor) {
        JPanel p = transparente(new BorderLayout());
        p.add(rotulo(chave, 13, false, APAGADO), BorderLayout.WEST);
        p.add(valor, BorderLayout.EAST);
        p.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        return larguraTotal(p);
    }

    public static JComponent separador() {
        JPanel s = new JPanel();
        s.setBackground(LINHA);
        s.setPreferredSize(new Dimension(10, 1));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        return s;
    }
}
