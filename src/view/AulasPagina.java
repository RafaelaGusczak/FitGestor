package view;

import dao.AulaDao;
import dao.InstrutorDao;
import db.Banco;
import model.Aula;
import model.Instrutor;
import util.Datas;
import util.Formato;
import util.RegraNegocioException;

import javax.swing.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AulasPagina extends CadastroPagina<Aula> {

    private final AulaDao dao = new AulaDao();
    private final InstrutorDao instrutorDao = new InstrutorDao();
    private List<Instrutor> instrutores = List.of();

    private final JComboBox<String> filtroDia;
    private final JTextField modalidade = Tema.campo("");
    private final JComboBox<String> instrutor = Tema.combo();
    private final JComboBox<String> dia = Tema.combo(Formato.DIAS_SEMANA);
    private final JTextField horario = Tema.campo("");
    private final JTextField vagas = Tema.campo("");

    public AulasPagina() {
        super("Aulas", "Grade semanal de aulas coletivas");
        Mascara.hora(horario);
        List<String> opcoes = new ArrayList<>(List.of("Todos"));
        opcoes.addAll(List.of(Formato.DIAS_SEMANA));
        filtroDia = Tema.combo(opcoes.toArray(new String[0]));
        filtroDia.addActionListener(e -> pesquisar());
        montar("Modalidade", Tema.rotulado("Dia da semana", filtroDia),
                new String[]{"ID", "Modalidade", "Dia", "Horário", "Instrutor", "Vagas"},
                "Dados da aula",
                Tema.rotulado("Modalidade", modalidade),
                Tema.rotulado("Instrutor", instrutor),
                Tema.par(Tema.rotulado("Dia da semana", dia), Tema.rotulado("Horário (hh:mm)", horario)),
                Tema.rotulado("Vagas", vagas));
    }

    @Override
    protected void prepararDados(Runnable concluir) {
        carregar(() -> instrutorDao.buscar(""), lista -> {
            instrutores = lista;
            List<String> nomes = new ArrayList<>();
            lista.forEach(i -> nomes.add(i.nome()));
            trocarOpcoes(instrutor, nomes);
            concluir.run();
        });
    }

    @Override
    protected Banco.Operacao<List<Aula>> consulta(String texto) {
        String diaFiltro = (String) filtroDia.getSelectedItem();
        LocalDate hoje = Datas.hoje();
        return () -> dao.buscar(texto, diaFiltro, Datas.inicioDoDia(hoje), Datas.fimDoDia(hoje));
    }

    @Override
    protected Integer idDe(Aula a) {
        return a.id();
    }

    @Override
    protected Object[] linha(Aula a) {
        return new Object[]{a.id(), a.modalidade(), a.diaSemana(), Formato.hora(a.horario()),
                a.instrutorNome() == null ? "—" : a.instrutorNome(), a.vagas()};
    }

    @Override
    protected void preencher(Aula a) {
        modalidade.setText(a.modalidade());
        if (a.instrutorNome() != null) {
            instrutor.setSelectedItem(a.instrutorNome());
        }
        dia.setSelectedItem(a.diaSemana());
        horario.setText(Formato.hora(a.horario()));
        vagas.setText(String.valueOf(a.vagas()));
    }

    @Override
    protected void limpar() {
        modalidade.setText("");
        if (instrutor.getItemCount() > 0) {
            instrutor.setSelectedIndex(0);
        }
        dia.setSelectedIndex(0);
        horario.setText("");
        vagas.setText("");
    }

    @Override
    protected Aula ler(Integer id) {
        int indice = instrutor.getSelectedIndex();
        if (indice < 0 || indice >= instrutores.size()) {
            throw new RegraNegocioException("Cadastre um instrutor antes de criar aulas.");
        }
        return new Aula(id, Formato.obrigatorio(modalidade.getText(), "a modalidade"),
                (String) dia.getSelectedItem(), Formato.lerHora(horario.getText(), "Horário"),
                instrutores.get(indice).id(), instrutores.get(indice).nome(),
                Formato.lerInteiro(vagas.getText(), "Vagas"), 0);
    }

    @Override
    protected int gravar(Aula a) throws SQLException {
        return dao.salvar(a);
    }

    @Override
    protected void remover(Aula a) throws SQLException {
        dao.excluir(a.id());
    }

    @Override
    protected String nomeDe(Aula a) {
        return "a aula de " + a.modalidade() + " (" + a.diaSemana() + " " + Formato.hora(a.horario())
                + "), junto com os check-ins dela";
    }
}
