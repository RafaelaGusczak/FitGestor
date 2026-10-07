package view;

import dao.AlunoDao;
import dao.AulaDao;
import dao.CheckinDao;
import dao.MatriculaDao;
import dao.PagamentoDao;
import model.Aula;
import model.Checkin;
import util.Datas;
import util.Formato;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class InicioPagina extends Pagina {

    /** Tudo que a tela inicial mostra, lido numa única ida ao banco. */
    private record Painel(int alunosAtivos, int aVencer, PagamentoDao.Resumo pagamentos, int checkinsHoje,
                          List<Aula> aulas, List<Checkin> ultimos) { }

    private final AlunoDao alunos = new AlunoDao();
    private final MatriculaDao matriculas = new MatriculaDao();
    private final PagamentoDao pagamentos = new PagamentoDao();
    private final CheckinDao checkins = new CheckinDao();
    private final AulaDao aulas = new AulaDao();

    private final JPanel resumo = Tema.transparente(new GridLayout(1, 4, 16, 0));
    private final JTable tabelaAulas = Tema.tabelaVazia(new String[]{"Quando", "Modalidade", "Instrutor", "Vagas"});
    private final JTable tabelaCheckins = Tema.tabelaVazia(new String[]{"Hora", "Aluno", "Aula"});
    private final JPanel listas = Tema.transparente(new GridLayout(1, 2, 16, 0));

    public InicioPagina(Consumer<String> navegar) {
        super("Início", "Visão geral da academia",
                botao("+ Novo aluno", Tema.TipoBotao.SECUNDARIO, () -> navegar.accept("Alunos")),
                botao("+ Nova matrícula", Tema.TipoBotao.PRIMARIO, () -> navegar.accept("Matrículas")));

        Tema.Cartao cartaoAulas = Tema.cartaoColuna("Próximas aulas", 20);
        cartaoAulas.add(Tema.tabelaCompacta(tabelaAulas));
        Tema.Cartao cartaoCheckins = Tema.cartaoColuna("Últimos check-ins", 20);
        cartaoCheckins.add(Tema.tabelaCompacta(tabelaCheckins));
        listas.add(cartaoAulas);
        listas.add(cartaoCheckins);

        JPanel embaixo = Tema.transparente(new BorderLayout());
        embaixo.add(listas, BorderLayout.NORTH);
        corpo.add(resumo, BorderLayout.NORTH);
        corpo.add(embaixo, BorderLayout.CENTER);
    }

    private static JButton botao(String texto, Tema.TipoBotao tipo, Runnable acao) {
        JButton b = Tema.botao(texto, tipo);
        b.addActionListener(e -> acao.run());
        return b;
    }

    @Override
    public void aoExibir() {
        LocalDate hoje = Datas.hoje();
        setSubtitulo("Visão geral da academia — " + hoje.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"))
                + ", " + String.format("%02d", hoje.getDayOfMonth()) + " de "
                + hoje.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR")) + " de " + hoje.getYear());
        carregar(() -> {
            var inicioDia = Datas.inicioDoDia(hoje);
            var fimDia = Datas.fimDoDia(hoje);
            return new Painel(alunos.contarAtivos(),
                    matriculas.contarAVencer(hoje, hoje.plusDays(30)),
                    pagamentos.resumo(hoje, hoje.withDayOfMonth(1), hoje.withDayOfMonth(hoje.lengthOfMonth())),
                    checkins.contar(inicioDia, fimDia),
                    aulas.todas(inicioDia, fimDia),
                    checkins.ultimos(4));
        }, p -> mostrar(p, hoje));
    }

    private void mostrar(Painel p, LocalDate hoje) {
        resumo.removeAll();
        resumo.add(Tema.estatistica("Alunos ativos", String.valueOf(p.alunosAtivos()), "cadastrados como ativos", Tema.TEXTO));
        resumo.add(Tema.estatistica("Matrículas a vencer", String.valueOf(p.aVencer()), "nos próximos 30 dias", Tema.LARANJA));
        resumo.add(Tema.estatistica("Em atraso", Formato.moeda(p.pagamentos().emAtraso()),
                p.pagamentos().emAtrasoQtd() + (p.pagamentos().emAtrasoQtd() == 1 ? " mensalidade" : " mensalidades"), Tema.VERMELHO));
        resumo.add(Tema.estatistica("Check-ins hoje", String.valueOf(p.checkinsHoje()),
                "até as " + Formato.hora(LocalTime.now().withSecond(0).withNano(0)), Tema.VERDE));
        resumo.revalidate();
        resumo.repaint();

        Tema.preencher(tabelaAulas, proximasAulas(p.aulas(), hoje));
        Object[][] ultimos = new Object[p.ultimos().size()][];
        for (int i = 0; i < ultimos.length; i++) {
            Checkin c = p.ultimos().get(i);
            ultimos[i] = new Object[]{Formato.hora(c.hora()), c.aluno(), c.aula()};
        }
        Tema.preencher(tabelaCheckins, ultimos);
        listas.revalidate();
    }

    /** As 4 aulas que acontecem a seguir, a partir de agora. Vagas = livres / total (livres só contam hoje). */
    private Object[][] proximasAulas(List<Aula> todas, LocalDate hoje) {
        LocalDateTime agora = LocalDateTime.now();
        record Proxima(LocalDateTime quando, Aula aula) { }
        List<Proxima> lista = new ArrayList<>();
        for (Aula a : todas) {
            for (int d = 0; d <= 7; d++) {
                LocalDate dia = hoje.plusDays(d);
                LocalDateTime quando = dia.atTime(a.horario());
                if (Formato.diaSemana(dia).equals(a.diaSemana()) && !quando.isBefore(agora)) {
                    lista.add(new Proxima(quando, a));
                    break;
                }
            }
        }
        lista.sort(Comparator.comparing(Proxima::quando));
        int n = Math.min(4, lista.size());
        Object[][] linhas = new Object[n][];
        for (int i = 0; i < n; i++) {
            Proxima p = lista.get(i);
            boolean ehHoje = p.quando().toLocalDate().equals(hoje);
            String quando = (ehHoje ? "Hoje" : Formato.diaAbreviado(p.aula().diaSemana())) + " " + Formato.hora(p.aula().horario());
            int livres = ehHoje ? p.aula().vagasLivres() : p.aula().vagas();
            linhas[i] = new Object[]{quando, p.aula().modalidade(),
                    p.aula().instrutorNome() == null ? "—" : p.aula().instrutorNome(), livres + " / " + p.aula().vagas()};
        }
        return linhas;
    }
}
