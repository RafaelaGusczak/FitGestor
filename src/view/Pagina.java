package view;

import db.Banco;
import util.Erros;
import util.RegraNegocioException;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Base de todas as telas internas: cabeçalho (título, subtítulo, botões)
 * e uma área "corpo" que cada tela preenche.
 */
public abstract class Pagina extends JPanel {

    protected final JPanel corpo = Tema.transparente(new BorderLayout(0, 18));
    private final JLabel rotuloSubtitulo;

    protected Pagina(String titulo, String subtitulo, JComponent... acoes) {
        setLayout(new BorderLayout(0, 20));
        setBackground(Tema.FUNDO);
        setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        rotuloSubtitulo = Tema.rotulo(subtitulo, 13, false, Tema.APAGADO);
        JPanel titulos = Tema.colunaTransparente();
        titulos.add(Tema.rotulo(titulo, 26, true, Tema.TEXTO));
        titulos.add(Box.createVerticalStrut(2));
        titulos.add(rotuloSubtitulo);

        JPanel cabecalho = Tema.transparente(new BorderLayout());
        cabecalho.add(titulos, BorderLayout.WEST);
        if (acoes.length > 0) {
            JPanel botoes = Tema.transparente(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            for (JComponent a : acoes) {
                botoes.add(a);
            }
            JPanel centralizado = Tema.transparente(new GridBagLayout());
            centralizado.add(botoes);
            cabecalho.add(centralizado, BorderLayout.EAST);
        }
        add(cabecalho, BorderLayout.NORTH);
        add(corpo, BorderLayout.CENTER);
    }

    /** Chamado toda vez que a tela é aberta pelo menu: é aqui que ela busca os dados no banco. */
    public void aoExibir() { }

    protected void setSubtitulo(String texto) {
        rotuloSubtitulo.setText(texto);
    }

    // ---------- Banco e mensagens ----------

    /** Roda a consulta em segundo plano e entrega o resultado na tela; erros viram uma mensagem. */
    protected <T> void carregar(Banco.Operacao<T> operacao, Consumer<T> aoConcluir) {
        executar(operacao, aoConcluir, null);
    }

    protected <T> void executar(Banco.Operacao<T> operacao, Consumer<T> aoConcluir, Runnable aoFalhar) {
        Banco.async(operacao, aoConcluir, e -> {
            falha(e);
            if (aoFalhar != null) {
                aoFalhar.run();
            }
        });
    }

    protected void falha(Throwable e) {
        boolean regra = e instanceof RegraNegocioException;
        JOptionPane.showMessageDialog(this, Erros.traduzir(e), regra ? "Atenção" : "Erro",
                regra ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    protected void aviso(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    protected void info(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "FitGestor", JOptionPane.INFORMATION_MESSAGE);
    }

    protected boolean confirmar(String pergunta) {
        return JOptionPane.showConfirmDialog(this, pergunta, "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Troca as opções de um combo mantendo, se possível, o item que estava escolhido. */
    protected static void trocarOpcoes(JComboBox<String> combo, java.util.List<String> opcoes) {
        Object atual = combo.getSelectedItem();
        combo.setModel(new DefaultComboBoxModel<>(opcoes.toArray(new String[0])));
        if (atual != null && opcoes.contains(atual.toString())) {
            combo.setSelectedItem(atual);
        }
    }

    /** Barra de busca: campo + filtros opcionais + botão Pesquisar, alinhados pela base. Enter também pesquisa. */
    protected static JPanel barraBusca(JTextField campo, Runnable pesquisar, JComponent... filtros) {
        JPanel barra = Tema.transparente(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(0, 0, 0, 12);

        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        barra.add(Tema.rotulado("Buscar", campo), c);

        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        for (JComponent f : filtros) {
            f.setPreferredSize(new Dimension(200, f.getPreferredSize().height));
            barra.add(f, c);
        }
        c.insets = new Insets(0, 0, 0, 0);
        JButton botao = Tema.botao("Pesquisar", Tema.TipoBotao.SECUNDARIO);
        botao.addActionListener(e -> pesquisar.run());
        campo.addActionListener(e -> pesquisar.run());
        barra.add(botao, c);
        return barra;
    }
}
