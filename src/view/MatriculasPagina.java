package view;

import controller.MatriculaController;
import dao.AlunoDao;
import dao.MatriculaDao;
import dao.PlanoDao;
import model.Aluno;
import model.Matricula;
import model.Plano;
import util.Datas;
import util.Formato;
import util.RegraNegocioException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MatriculasPagina extends Pagina {

    private static final String[] STATUS = {"Ativa", "Trancada", "Cancelada"};

    private final MatriculaController controller = new MatriculaController();
    private final AlunoDao alunoDao = new AlunoDao();
    private final PlanoDao planoDao = new PlanoDao();
    private final MatriculaDao matriculaDao = new MatriculaDao();

    private List<Aluno> alunos = List.of();
    private List<Plano> planos = List.of();
    private List<Matricula> recentes = List.of();
    private boolean gravando;

    private final JComboBox<String> alunoCombo = Tema.combo();
    private final JComboBox<String> planoCombo = Tema.combo();
    private final JTextField inicio = Tema.campo("");
    private final JTextField termino = Tema.campoLeitura("");
    private final JTextField valor = Tema.campoLeitura("");
    private final JComboBox<String> status = Tema.combo(STATUS);

    private final JLabel resPlano = Tema.rotulo("—", 13, false, Tema.TEXTO);
    private final JLabel resDuracao = Tema.rotulo("—", 13, false, Tema.TEXTO);
    private final JLabel resMensalidades = Tema.rotulo("—", 13, false, Tema.TEXTO);
    private final JLabel resVencimento = Tema.rotulo("—", 13, false, Tema.TEXTO);
    private final JLabel resTotal = Tema.rotulo("—", 16, true, Tema.TEXTO);

    private final JTable tabela = Tema.tabelaVazia(new String[]{"ID", "Aluno", "Plano", "Início", "Término", "Status"});
    private final JComboBox<String> novoStatus = Tema.combo(STATUS);
    private final JButton alterarStatus = Tema.botao("Alterar status", Tema.TipoBotao.SECUNDARIO);

    public MatriculasPagina() {
        super("Matrículas", "Vincule um aluno a um plano");
        Mascara.data(inicio);

        Tema.Cartao nova = Tema.formulario("Nova matrícula",
                Tema.rotulado("Aluno", alunoCombo),
                Tema.rotulado("Plano", planoCombo),
                Tema.par(Tema.rotulado("Data de início", inicio),
                        Tema.rotulado("Data de término (calculada)", termino)),
                Tema.par(Tema.rotulado("Valor mensal (R$)", valor), Tema.rotulado("Status", status)));

        Tema.Cartao resumo = Tema.cartaoColuna("Resumo", 20);
        resumo.add(Tema.linhaChaveValor("Plano", resPlano));
        resumo.add(Tema.linhaChaveValor("Duração", resDuracao));
        resumo.add(Tema.linhaChaveValor("Mensalidades", resMensalidades));
        resumo.add(Tema.linhaChaveValor("1º vencimento", resVencimento));
        resumo.add(Box.createVerticalStrut(6));
        resumo.add(Tema.separador());
        resumo.add(Box.createVerticalStrut(6));
        resumo.add(Tema.linhaChaveValor("Total", resTotal));
        resumo.add(Box.createVerticalGlue());
        resumo.add(Box.createVerticalStrut(14));
        JButton confirmar = Tema.botao("Confirmar matrícula", Tema.TipoBotao.PRIMARIO);
        confirmar.addActionListener(e -> confirmar());
        resumo.add(Tema.larguraTotal(confirmar));
        resumo.add(Box.createVerticalStrut(10));
        JButton cancelar = Tema.botao("Cancelar", Tema.TipoBotao.SECUNDARIO);
        cancelar.addActionListener(e -> reiniciarFormulario());
        resumo.add(Tema.larguraTotal(cancelar));
        resumo.setPreferredSize(new Dimension(340, resumo.getPreferredSize().height));

        JPanel topo = Tema.transparente(new BorderLayout(20, 0));
        topo.add(nova, BorderLayout.CENTER);
        topo.add(resumo, BorderLayout.EAST);

        novoStatus.setPreferredSize(new Dimension(150, 38));
        alterarStatus.setEnabled(false);
        alterarStatus.addActionListener(e -> mudarStatus());
        JPanel acoesLista = Tema.transparente(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoesLista.add(Tema.rotulo("Matrícula selecionada:", 13, false, Tema.APAGADO));
        acoesLista.add(novoStatus);
        acoesLista.add(alterarStatus);
        JPanel cabecalhoLista = Tema.transparente(new BorderLayout());
        cabecalhoLista.add(Tema.rotulo("Matrículas recentes", 16, true, Tema.TEXTO), BorderLayout.WEST);
        cabecalhoLista.add(acoesLista, BorderLayout.EAST);

        JPanel lista = Tema.transparente(new BorderLayout(0, 10));
        lista.add(cabecalhoLista, BorderLayout.NORTH);
        lista.add(Tema.comScroll(tabela), BorderLayout.CENTER);

        corpo.add(topo, BorderLayout.NORTH);
        corpo.add(lista, BorderLayout.CENTER);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int i = tabela.getSelectedRow();
            alterarStatus.setEnabled(i >= 0 && i < recentes.size());
            if (i >= 0 && i < recentes.size()) {
                String atual = recentes.get(i).status();
                novoStatus.setSelectedItem(atual);
            }
        });
        planoCombo.addActionListener(e -> atualizarCalculos());
        inicio.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                atualizarCalculos();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                atualizarCalculos();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                atualizarCalculos();
            }
        });
        reiniciarFormulario();
    }

    @Override
    public void aoExibir() {
        record Dados(List<Aluno> alunos, List<Plano> planos, List<Matricula> recentes) { }
        carregar(() -> new Dados(alunoDao.ativos(), planoDao.buscar(""), matriculaDao.recentes(100)), d -> {
            alunos = d.alunos();
            planos = d.planos();
            List<String> nomesAlunos = new ArrayList<>();
            alunos.forEach(a -> nomesAlunos.add(a.nome() + " — " + a.cpf()));
            trocarOpcoes(alunoCombo, nomesAlunos);
            List<String> nomesPlanos = new ArrayList<>();
            planos.forEach(p -> nomesPlanos.add(p.nome() + " — " + Formato.moeda(p.valorMensal()) + " / mês"));
            trocarOpcoes(planoCombo, nomesPlanos);
            mostrarRecentes(d.recentes());
            atualizarCalculos();
        });
    }

    private void mostrarRecentes(List<Matricula> lista) {
        recentes = lista;
        LocalDate hoje = Datas.hoje();
        Object[][] dados = new Object[lista.size()][];
        for (int i = 0; i < dados.length; i++) {
            Matricula m = lista.get(i);
            dados[i] = new Object[]{m.id(), m.alunoNome(), m.planoNome(), Formato.data(m.inicio()),
                    Formato.data(m.termino()), m.statusExibido(hoje)};
        }
        Tema.preencher(tabela, dados);
        alterarStatus.setEnabled(false);
    }

    private Plano planoEscolhido() {
        int i = planoCombo.getSelectedIndex();
        return i >= 0 && i < planos.size() ? planos.get(i) : null;
    }

    private Aluno alunoEscolhido() {
        int i = alunoCombo.getSelectedIndex();
        return i >= 0 && i < alunos.size() ? alunos.get(i) : null;
    }

    private void atualizarCalculos() {
        Plano plano = planoEscolhido();
        LocalDate inicioData;
        try {
            inicioData = Formato.lerData(inicio.getText(), "Data de início", true);
        } catch (RegraNegocioException e) {
            inicioData = null;
        }
        valor.setText(plano == null ? "" : Formato.decimal(plano.valorMensal()));
        termino.setText(plano == null || inicioData == null ? ""
                : Formato.data(MatriculaController.calcularTermino(inicioData, plano)));
        resPlano.setText(plano == null ? "—" : plano.nome());
        resDuracao.setText(plano == null ? "—" : plano.duracaoMeses() + (plano.duracaoMeses() == 1 ? " mês" : " meses"));
        resMensalidades.setText(plano == null ? "—" : MatriculaController.descreverTotal(plano));
        resVencimento.setText(inicioData == null ? "—" : Formato.data(inicioData));
        resTotal.setText(plano == null ? "—"
                : Formato.moeda(plano.valorMensal().multiply(java.math.BigDecimal.valueOf(plano.duracaoMeses()))));
    }

    private void reiniciarFormulario() {
        inicio.setText(Formato.data(Datas.hoje()));
        status.setSelectedItem("Ativa");
        if (alunoCombo.getItemCount() > 0) {
            alunoCombo.setSelectedIndex(0);
        }
        if (planoCombo.getItemCount() > 0) {
            planoCombo.setSelectedIndex(0);
        }
        atualizarCalculos();
    }

    private void confirmar() {
        if (gravando) {
            return;
        }
        Aluno aluno = alunoEscolhido();
        Plano plano = planoEscolhido();
        LocalDate dataInicio;
        try {
            if (aluno == null) {
                throw new RegraNegocioException("Não há alunos ativos cadastrados.");
            }
            if (plano == null) {
                throw new RegraNegocioException("Não há planos cadastrados.");
            }
            dataInicio = Formato.lerData(inicio.getText(), "Data de início", true);
        } catch (RegraNegocioException e) {
            aviso(e.getMessage());
            return;
        }
        String statusEscolhido = (String) status.getSelectedItem();
        gravando = true;
        executar(() -> controller.criar(aluno, plano, dataInicio, statusEscolhido), id -> {
            gravando = false;
            info("Matrícula criada para " + aluno.nome() + ".\n"
                    + (statusEscolhido.equals("Ativa") ? plano.duracaoMeses() + " mensalidade(s) gerada(s) em Pagamentos." : ""));
            reiniciarFormulario();
            aoExibir();
        }, () -> gravando = false);
    }

    private void mudarStatus() {
        int i = tabela.getSelectedRow();
        if (gravando || i < 0 || i >= recentes.size()) {
            return;
        }
        Matricula m = recentes.get(i);
        String novo = (String) novoStatus.getSelectedItem();
        if (novo.equals(m.status())) {
            return;
        }
        if (novo.equals("Cancelada") && !confirmar("Cancelar a matrícula de " + m.alunoNome()
                + "? As mensalidades futuras ainda não pagas serão removidas.")) {
            return;
        }
        gravando = true;
        LocalDate hoje = Datas.hoje();
        executar(() -> {
            controller.alterarStatus(m, novo, hoje);
            return null;
        }, ok -> {
            gravando = false;
            aoExibir();
        }, () -> gravando = false);
    }
}
