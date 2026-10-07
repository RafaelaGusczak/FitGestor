package view;

import controller.CheckinController;
import dao.AulaDao;
import dao.CheckinDao;
import db.Banco;
import model.Aula;
import model.Checkin;
import model.SituacaoAluno;
import util.Datas;
import util.Formato;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CheckinPagina extends Pagina {

    /** Caixa com a situação do aluno encontrado: verde se pode entrar, vermelha se algo impede. */
    private static class CaixaSituacao extends JPanel {
        private final JLabel nome = Tema.rotulo(" ", 14, true, Tema.TEXTO);
        private final JLabel detalhe = Tema.rotulo(" ", 12, false, Tema.APAGADO);
        private final JPanel selos = Tema.transparente(new FlowLayout(FlowLayout.LEFT, 8, 0));
        private boolean liberado = true;

        CaixaSituacao() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
            add(nome);
            add(Box.createVerticalStrut(4));
            add(detalhe);
            add(Box.createVerticalStrut(8));
            selos.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
            selos.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(selos);
            setVisible(false);
        }

        void mostrar(SituacaoAluno s) {
            boolean ativo = s.aluno().situacao().equals("Ativo");
            liberado = ativo && s.matriculaAtiva() && s.pagamentoEmDia();
            nome.setText(s.aluno().nome());
            detalhe.setText(s.matriculaAtiva()
                    ? "Plano " + s.plano() + " · válido até " + Formato.data(s.validoAte())
                    : "Sem matrícula vigente");
            selos.removeAll();
            if (!ativo) {
                selos.add(Tema.pilula("Aluno inativo", Tema.TipoPilula.ERRO));
            }
            selos.add(s.matriculaAtiva() ? Tema.pilula("Matrícula ativa", Tema.TipoPilula.OK)
                    : Tema.pilula("Sem matrícula ativa", Tema.TipoPilula.ERRO));
            selos.add(s.pagamentoEmDia() ? Tema.pilula("Pagamento em dia", Tema.TipoPilula.OK)
                    : Tema.pilula("Mensalidade em atraso", Tema.TipoPilula.ERRO));
            setVisible(true);
            revalidate();
            repaint();
        }

        void esconder() {
            setVisible(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(liberado ? new Color(0xF0FDF4) : new Color(0xFEF2F2));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(liberado ? new Color(0xBBF7D0) : new Color(0xFECACA));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private final CheckinController controller = new CheckinController();
    private final AulaDao aulaDao = new AulaDao();
    private final CheckinDao checkinDao = new CheckinDao();

    private final JTextField alunoCampo = Tema.campoComDica("Digite nome ou CPF e tecle Enter");
    private final CaixaSituacao caixa = new CaixaSituacao();
    private final JComboBox<String> aulaCombo = Tema.combo();
    private final JLabel vagas = Tema.rotulo(" ", 12, false, Tema.APAGADO);
    private final JTable tabelaHoje = Tema.tabelaVazia(new String[]{"Hora", "Aluno", "Aula", "Instrutor"});
    private final Tema.Cartao hoje = Tema.cartaoColuna("Check-ins de hoje", 20);

    private SituacaoAluno situacaoAtual;
    private List<Aula> aulasDeHoje = List.of();
    private boolean ocupado;
    private boolean montandoAulas;

    public CheckinPagina() {
        super("Check-in", "Registre a presença do aluno na aula");

        JButton buscar = Tema.botao("Buscar", Tema.TipoBotao.SECUNDARIO);
        JPanel linhaAluno = Tema.transparente(new BorderLayout(8, 0));
        linhaAluno.add(alunoCampo, BorderLayout.CENTER);
        linhaAluno.add(buscar, BorderLayout.EAST);
        buscar.addActionListener(e -> buscarAluno());
        alunoCampo.addActionListener(e -> buscarAluno());
        alunoCampo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void mudou() {
                situacaoAtual = null;
                caixa.esconder();
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                mudou();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                mudou();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                mudou();
            }
        });

        aulaCombo.addActionListener(e -> {
            if (!montandoAulas) {
                mostrarVagas();
            }
        });

        Tema.Cartao registro = Tema.formulario("Registrar check-in",
                Tema.rotulado("Aluno (nome ou CPF)", linhaAluno),
                caixa,
                Tema.rotulado("Aula de hoje", aulaCombo),
                vagas);
        JButton confirmar = Tema.botao("Confirmar check-in", Tema.TipoBotao.PRIMARIO);
        confirmar.addActionListener(e -> confirmar());
        registro.add(Tema.larguraTotal(confirmar));
        registro.setPreferredSize(new Dimension(400, registro.getPreferredSize().height));

        hoje.add(Tema.tabelaCompacta(tabelaHoje));

        JPanel esquerda = Tema.transparente(new BorderLayout());
        esquerda.add(registro, BorderLayout.NORTH);
        JPanel direita = Tema.transparente(new BorderLayout());
        direita.add(hoje, BorderLayout.NORTH);

        JPanel linha = Tema.transparente(new BorderLayout(20, 0));
        linha.add(esquerda, BorderLayout.WEST);
        linha.add(direita, BorderLayout.CENTER);
        corpo.add(linha, BorderLayout.CENTER);
    }

    @Override
    public void aoExibir() {
        recarregar();
    }

    private void recarregar() {
        LocalDate dia = Datas.hoje();
        record Dados(List<Aula> aulas, List<Checkin> checkins) { }
        carregar(() -> {
            var inicio = Datas.inicioDoDia(dia);
            var fim = Datas.fimDoDia(dia);
            String nomeDia = Formato.diaSemana(dia);
            List<Aula> deHoje = new ArrayList<>();
            for (Aula a : aulaDao.todas(inicio, fim)) {
                if (a.diaSemana().equals(nomeDia)) {
                    deHoje.add(a);
                }
            }
            return new Dados(deHoje, checkinDao.doPeriodo(inicio, fim));
        }, d -> {
            aulasDeHoje = d.aulas();
            montandoAulas = true;
            int escolhida = Math.max(0, aulaCombo.getSelectedIndex());
            List<String> nomes = new ArrayList<>();
            for (Aula a : aulasDeHoje) {
                nomes.add(a.modalidade() + " · " + Formato.diaAbreviado(a.diaSemana()) + " " + Formato.hora(a.horario()));
            }
            aulaCombo.setModel(new DefaultComboBoxModel<>(nomes.toArray(new String[0])));
            if (!nomes.isEmpty()) {
                aulaCombo.setSelectedIndex(Math.min(escolhida, nomes.size() - 1));
            }
            montandoAulas = false;
            mostrarVagas();

            Object[][] linhas = new Object[d.checkins().size()][];
            for (int i = 0; i < linhas.length; i++) {
                Checkin c = d.checkins().get(i);
                linhas[i] = new Object[]{Formato.hora(c.hora()), c.aluno(), c.aula(),
                        c.instrutor() == null ? "—" : c.instrutor()};
            }
            Tema.preencher(tabelaHoje, linhas);
            hoje.revalidate();
        });
    }

    private Aula aulaEscolhida() {
        int i = aulaCombo.getSelectedIndex();
        return i >= 0 && i < aulasDeHoje.size() ? aulasDeHoje.get(i) : null;
    }

    private void mostrarVagas() {
        Aula a = aulaEscolhida();
        vagas.setText(a == null ? "Nenhuma aula cadastrada para hoje"
                : "Vagas disponíveis: " + a.vagasLivres() + " de " + a.vagas());
    }

    private void buscarAluno() {
        String texto = alunoCampo.getText();
        caixa.esconder();
        situacaoAtual = null;
        carregar(() -> controller.consultar(texto), s -> {
            situacaoAtual = s;
            caixa.mostrar(s);
        });
    }

    private void confirmar() {
        if (ocupado) {
            return;
        }
        SituacaoAluno situacao = situacaoAtual;
        Aula aula = aulaEscolhida();
        if (situacao == null) {
            aviso("Busque o aluno (nome ou CPF, tecle Enter) antes de confirmar.");
            return;
        }
        ocupado = true;
        executar(() -> {
            controller.registrar(situacao, aula);
            return null;
        }, ok -> {
            ocupado = false;
            alunoCampo.setText("");
            caixa.esconder();
            situacaoAtual = null;
            recarregar();
        }, () -> {
            ocupado = false;
            recarregar();
        });
    }
}
