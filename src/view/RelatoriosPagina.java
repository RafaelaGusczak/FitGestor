package view;

import dao.PlanoDao;
import dao.RelatorioDao;
import model.Plano;
import util.Datas;
import util.Formato;
import util.RegraNegocioException;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class RelatoriosPagina extends Pagina {

    private final CardLayout cartas = new CardLayout();
    private final JPanel conteudo = Tema.transparente(cartas);
    private final JLabel[] abas = new JLabel[3];
    private final String[] nomes = {"Inadimplentes", "Frequência por aluno", "Matrículas a vencer"};

    private final RelatorioDao dao = new RelatorioDao();
    private final PlanoDao planoDao = new PlanoDao();
    private List<Plano> planos = List.of();
    private int aba;

    private final JTextField campoDe = Tema.campo("");
    private final JTextField campoAte = Tema.campo("");
    private final JComboBox<String> comboPlano = Tema.combo("Todos");

    private final JTable tabelaInadimplentes = Tema.tabelaVazia(
            new String[]{"Aluno", "Telefone", "Plano", "Vencimento", "Dias de atraso", "Valor"});
    private final JTable tabelaFrequencia = Tema.tabelaVazia(
            new String[]{"Aluno", "Plano", "Check-ins no período", "Última visita"});
    private final JTable tabelaAVencer = Tema.tabelaVazia(
            new String[]{"Aluno", "Plano", "Término", "Dias restantes"});
    private final JLabel contagem = Tema.rotulo(" ", 13, false, Tema.APAGADO);
    private final JLabel totalAtraso = Tema.rotulo("R$ 0,00", 20, true, Tema.VERMELHO);

    public RelatoriosPagina() {
        super("Relatórios", "Consultas gerenciais");
        Mascara.data(campoDe);
        Mascara.data(campoAte);

        JPanel barraAbas = Tema.transparente(new FlowLayout(FlowLayout.LEFT, 28, 0));
        barraAbas.setBorder(BorderFactory.createEmptyBorder(0, -28, 0, 0));
        for (int i = 0; i < nomes.length; i++) {
            final int indice = i;
            abas[i] = new JLabel(nomes[i]);
            abas[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            abas[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    trocarAba(indice);
                }
            });
            barraAbas.add(abas[i]);
        }

        JPanel filtros = Tema.transparente(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(0, 0, 0, 12);
        JComponent de = Tema.rotulado("De", campoDe);
        JComponent ate = Tema.rotulado("Até", campoAte);
        JComponent plano = Tema.rotulado("Plano", comboPlano);
        de.setPreferredSize(new Dimension(180, de.getPreferredSize().height));
        ate.setPreferredSize(new Dimension(180, ate.getPreferredSize().height));
        plano.setPreferredSize(new Dimension(200, plano.getPreferredSize().height));
        filtros.add(de, c);
        filtros.add(ate, c);
        filtros.add(plano, c);
        c.weightx = 1;
        filtros.add(Box.createGlue(), c);
        c.weightx = 0;
        JButton exportar = Tema.botao("Exportar PDF", Tema.TipoBotao.SECUNDARIO);
        exportar.addActionListener(e -> info("A exportação para PDF ainda não foi implementada."));
        filtros.add(exportar, c);
        c.insets = new Insets(0, 0, 0, 0);
        JButton gerar = Tema.botao("Gerar relatório", Tema.TipoBotao.PRIMARIO);
        gerar.addActionListener(e -> gerarRelatorio());
        filtros.add(gerar, c);

        conteudo.add(inadimplentes(), nomes[0]);
        conteudo.add(Tema.comScroll(tabelaFrequencia), nomes[1]);
        conteudo.add(Tema.comScroll(tabelaAVencer), nomes[2]);

        JPanel topo = Tema.transparente(new BorderLayout(0, 16));
        topo.add(barraAbas, BorderLayout.NORTH);
        topo.add(filtros, BorderLayout.CENTER);

        corpo.add(topo, BorderLayout.NORTH);
        corpo.add(conteudo, BorderLayout.CENTER);
        selecionar(0);
        periodoPadrao(0);
    }

    private JPanel inadimplentes() {
        JPanel p = Tema.transparente(new BorderLayout(0, 12));
        p.add(Tema.comScroll(tabelaInadimplentes), BorderLayout.CENTER);

        JPanel total = Tema.transparente(new BorderLayout());
        total.add(contagem, BorderLayout.WEST);
        JPanel valor = Tema.transparente(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        valor.add(Tema.rotulo("Total em atraso", 13, false, Tema.APAGADO));
        valor.add(totalAtraso);
        total.add(valor, BorderLayout.EAST);
        p.add(total, BorderLayout.SOUTH);
        return p;
    }

    @Override
    public void aoExibir() {
        carregar(() -> planoDao.buscar(""), lista -> {
            planos = lista;
            List<String> opcoes = new ArrayList<>(List.of("Todos"));
            lista.forEach(pl -> opcoes.add(pl.nome()));
            trocarOpcoes(comboPlano, opcoes);
            gerarRelatorio();
        });
    }

    /** Preenche as datas padrão de cada relatório. */
    private void periodoPadrao(int indice) {
        LocalDate hoje = Datas.hoje();
        LocalDate de = switch (indice) {
            case 0 -> hoje.minusDays(90);
            case 1 -> hoje.withDayOfMonth(1);
            default -> hoje;
        };
        LocalDate ate = indice == 2 ? hoje.plusDays(30) : hoje;
        campoDe.setText(Formato.data(de));
        campoAte.setText(Formato.data(ate));
    }

    private void gerarRelatorio() {
        LocalDate de;
        LocalDate ate;
        try {
            de = Formato.lerData(campoDe.getText(), "De", true);
            ate = Formato.lerData(campoAte.getText(), "Até", true);
            if (ate.isBefore(de)) {
                throw new RegraNegocioException("A data final não pode ser anterior à inicial.");
            }
        } catch (RegraNegocioException e) {
            aviso(e.getMessage());
            return;
        }
        int indicePlano = comboPlano.getSelectedIndex() - 1;
        Integer planoId = indicePlano >= 0 && indicePlano < planos.size() ? planos.get(indicePlano).id() : null;
        LocalDate hoje = Datas.hoje();

        switch (aba) {
            case 0 -> carregar(() -> dao.inadimplentes(de, ate, planoId, hoje), lista -> {
                Object[][] linhas = new Object[lista.size()][];
                BigDecimal soma = BigDecimal.ZERO;
                for (int i = 0; i < linhas.length; i++) {
                    RelatorioDao.Inadimplente r = lista.get(i);
                    long dias = ChronoUnit.DAYS.between(r.vencimento(), hoje);
                    linhas[i] = new Object[]{r.aluno(), r.telefone() == null ? "—" : r.telefone(), r.plano(),
                            Formato.data(r.vencimento()), dias + (dias == 1 ? " dia" : " dias"), Formato.moeda(r.valor())};
                    soma = soma.add(r.valor());
                }
                Tema.preencher(tabelaInadimplentes, linhas);
                contagem.setText(lista.size() + (lista.size() == 1 ? " registro" : " registros"));
                totalAtraso.setText(Formato.moeda(soma));
            });
            case 1 -> carregar(() -> dao.frequencia(Datas.inicioDoDia(de), Datas.fimDoDia(ate), planoId), lista -> {
                Object[][] linhas = new Object[lista.size()][];
                for (int i = 0; i < linhas.length; i++) {
                    RelatorioDao.Frequencia r = lista.get(i);
                    linhas[i] = new Object[]{r.aluno(), r.plano() == null ? "—" : r.plano(), r.checkins(),
                            r.ultimaVisita() == null ? "—" : Formato.data(r.ultimaVisita())};
                }
                Tema.preencher(tabelaFrequencia, linhas);
            });
            default -> carregar(() -> dao.aVencer(de, ate, planoId), lista -> {
                Object[][] linhas = new Object[lista.size()][];
                for (int i = 0; i < linhas.length; i++) {
                    RelatorioDao.AVencer r = lista.get(i);
                    linhas[i] = new Object[]{r.aluno(), r.plano(), Formato.data(r.termino()),
                            ChronoUnit.DAYS.between(hoje, r.termino())};
                }
                Tema.preencher(tabelaAVencer, linhas);
            });
        }
    }

    private void selecionar(int indice) {
        for (int i = 0; i < abas.length; i++) {
            boolean ativa = i == indice;
            abas[i].setFont(Tema.fonte(14, ativa));
            abas[i].setForeground(ativa ? Tema.PRIMARIA : Tema.APAGADO);
            abas[i].setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 0, 2, 0, ativa ? Tema.PRIMARIA : Tema.FUNDO),
                    BorderFactory.createEmptyBorder(0, 0, 8, 0)));
        }
        cartas.show(conteudo, nomes[indice]);
    }

    private void trocarAba(int indice) {
        aba = indice;
        selecionar(indice);
        periodoPadrao(indice);
        gerarRelatorio();
    }
}
