package view;

import controller.PagamentoController;
import dao.PagamentoDao;
import model.Pagamento;
import util.Datas;
import util.Formato;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PagamentosPagina extends Pagina {

    private static final String TODOS_OS_MESES = "Todos os meses";
    private static final int COLUNA_ACAO = 7;

    private final PagamentoController controller = new PagamentoController();
    private final PagamentoDao dao = new PagamentoDao();

    private final JPanel resumo = Tema.transparente(new GridLayout(1, 3, 16, 0));
    private final JTextField busca = Tema.campoComDica("Nome do aluno");
    private final JComboBox<String> situacao = Tema.combo("Todas", "Pago", "Pendente", "Atrasado");
    private final JComboBox<String> mes = Tema.combo();
    private final JTable tabela = Tema.tabelaVazia(
            new String[]{"ID", "Aluno", "Plano", "Vencimento", "Valor", "Forma", "Situação", "Ação"});

    private List<Pagamento> pagamentos = List.of();
    private List<YearMonth> meses = List.of();
    private boolean ocupado;
    private boolean montandoMeses;

    public PagamentosPagina() {
        this(new Runnable[1]);
    }

    /** O botão do cabeçalho nasce antes da tela (argumento do super), então o clique passa por este vetor. */
    private PagamentosPagina(Runnable[] aoGerar) {
        super("Pagamentos", "Mensalidades geradas a partir das matrículas", botaoGerar(aoGerar));
        aoGerar[0] = this::gerar;

        JPanel topo = Tema.transparente(new BorderLayout(0, 18));
        topo.add(resumo, BorderLayout.NORTH);
        situacao.addActionListener(e -> pesquisar());
        mes.addActionListener(e -> {
            if (!montandoMeses) {
                pesquisar();
            }
        });
        topo.add(barraBusca(busca, this::pesquisar,
                Tema.rotulado("Situação", situacao), Tema.rotulado("Mês de vencimento", mes)), BorderLayout.CENTER);

        corpo.add(topo, BorderLayout.NORTH);
        corpo.add(Tema.comScroll(tabela), BorderLayout.CENTER);

        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int linha = tabela.rowAtPoint(e.getPoint());
                int coluna = tabela.columnAtPoint(e.getPoint());
                if (linha >= 0 && coluna == COLUNA_ACAO && "Dar baixa".equals(tabela.getValueAt(linha, coluna))) {
                    darBaixa(pagamentos.get(linha));
                }
            }
        });
        tabela.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int linha = tabela.rowAtPoint(e.getPoint());
                boolean sobreAcao = linha >= 0 && tabela.columnAtPoint(e.getPoint()) == COLUNA_ACAO
                        && "Dar baixa".equals(tabela.getValueAt(linha, COLUNA_ACAO));
                tabela.setCursor(Cursor.getPredefinedCursor(sobreAcao ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
    }

    private static JButton botaoGerar(Runnable[] aoGerar) {
        JButton b = Tema.botao("Gerar mensalidades", Tema.TipoBotao.SECUNDARIO);
        b.addActionListener(e -> aoGerar[0].run());
        return b;
    }

    @Override
    public void aoExibir() {
        montarMeses();
        pesquisar();
    }

    /** Mês atual ± 6, mais a opção "Todos os meses". Mantém a escolha anterior; na primeira vez, o mês atual. */
    private void montarMeses() {
        YearMonth atual = YearMonth.from(Datas.hoje());
        Object escolhido = mes.getSelectedItem();
        montandoMeses = true;
        List<String> nomes = new ArrayList<>();
        List<YearMonth> lista = new ArrayList<>();
        nomes.add(TODOS_OS_MESES);
        for (int i = -6; i <= 6; i++) {
            YearMonth m = atual.plusMonths(i);
            lista.add(m);
            nomes.add(nomeDoMes(m));
        }
        meses = lista;
        mes.setModel(new DefaultComboBoxModel<>(nomes.toArray(new String[0])));
        mes.setSelectedItem(escolhido != null && nomes.contains(escolhido.toString()) ? escolhido : nomeDoMes(atual));
        montandoMeses = false;
    }

    private static String nomeDoMes(YearMonth m) {
        String nome = m.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1) + "/" + m.getYear();
    }

    private void pesquisar() {
        LocalDate hoje = Datas.hoje();
        String texto = busca.getText();
        String sit = (String) situacao.getSelectedItem();
        int indiceMes = mes.getSelectedIndex() - 1;
        LocalDate de = indiceMes >= 0 && indiceMes < meses.size() ? meses.get(indiceMes).atDay(1) : null;
        LocalDate ate = indiceMes >= 0 && indiceMes < meses.size() ? meses.get(indiceMes).atEndOfMonth() : null;

        record Dados(List<Pagamento> lista, PagamentoDao.Resumo resumo) { }
        carregar(() -> new Dados(dao.buscar(texto, sit, de, ate, hoje),
                        dao.resumo(hoje, hoje.withDayOfMonth(1), hoje.withDayOfMonth(hoje.lengthOfMonth()))),
                d -> {
                    pagamentos = d.lista();
                    Object[][] dados = new Object[pagamentos.size()][];
                    for (int i = 0; i < dados.length; i++) {
                        Pagamento p = pagamentos.get(i);
                        String s = p.situacao(hoje);
                        dados[i] = new Object[]{p.id(), p.alunoNome(), p.planoNome(), Formato.data(p.vencimento()),
                                Formato.moeda(p.valor()), p.forma() == null ? "—" : p.forma(), s,
                                s.equals("Pago") ? "—" : "Dar baixa"};
                    }
                    Tema.preencher(tabela, dados);
                    mostrarResumo(d.resumo(), hoje);
                });
    }

    private void mostrarResumo(PagamentoDao.Resumo r, LocalDate hoje) {
        resumo.removeAll();
        resumo.add(Tema.estatistica("Recebido no mês", Formato.moeda(r.recebidoNoMes()),
                nomeDoMes(YearMonth.from(hoje)).toLowerCase(Locale.forLanguageTag("pt-BR")), Tema.VERDE));
        resumo.add(Tema.estatistica("A receber no mês", Formato.moeda(r.aReceberNoMes()),
                r.aReceberQtd() + (r.aReceberQtd() == 1 ? " mensalidade pendente" : " mensalidades pendentes"), Tema.TEXTO));
        resumo.add(Tema.estatistica("Em atraso", Formato.moeda(r.emAtraso()),
                r.emAtrasoQtd() + (r.emAtrasoQtd() == 1 ? " mensalidade" : " mensalidades"), Tema.VERMELHO));
        resumo.revalidate();
        resumo.repaint();
    }

    private void darBaixa(Pagamento p) {
        if (ocupado) {
            return;
        }
        JComboBox<String> forma = new JComboBox<>(PagamentoController.FORMAS);
        int resposta = JOptionPane.showConfirmDialog(this, new Object[]{
                "Dar baixa na mensalidade de " + p.alunoNome() + "\n" + Formato.moeda(p.valor())
                        + " — vencimento " + Formato.data(p.vencimento()) + "\n\nForma de pagamento:", forma},
                "Dar baixa", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (resposta != JOptionPane.OK_OPTION) {
            return;
        }
        String escolhida = (String) forma.getSelectedItem();
        LocalDate hoje = Datas.hoje();
        ocupado = true;
        executar(() -> {
            controller.darBaixa(p.id(), escolhida, hoje);
            return null;
        }, ok -> {
            ocupado = false;
            pesquisar();
        }, () -> {
            ocupado = false;
            pesquisar();
        });
    }

    private void gerar() {
        if (ocupado) {
            return;
        }
        ocupado = true;
        executar(controller::gerarMensalidades, qtd -> {
            ocupado = false;
            info(qtd == 0 ? "Todas as mensalidades das matrículas ativas já estavam geradas."
                    : qtd + " mensalidade(s) gerada(s).");
            pesquisar();
        }, () -> ocupado = false);
    }
}
