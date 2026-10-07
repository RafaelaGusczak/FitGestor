package view;

import db.Banco;
import util.RegraNegocioException;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Layout padrão dos cadastros (Alunos, Planos, Instrutores, Aulas): busca em cima, tabela à esquerda
 * e formulário à direita. Cuida de listar, selecionar, salvar e excluir; cada tela diz só o que é específico dela.
 *
 * @param <T> a entidade do cadastro (record do pacote model)
 */
public abstract class CadastroPagina<T> extends Pagina {

    private JTextField busca;
    private JTable tabela;
    private List<T> itens = List.of();
    private T selecionado;
    private boolean atualizandoTabela;
    private boolean gravando;

    protected CadastroPagina(String titulo, String subtitulo) {
        super(titulo, subtitulo);
    }

    /**
     * Monta a tela. A subclasse chama no fim do seu construtor, já com os campos criados.
     *
     * @param filtro componente extra da busca (já com rótulo), ou null
     */
    protected final void montar(String dicaBusca, JComponent filtro, String[] colunas,
                                String tituloFormulario, JComponent... camposFormulario) {
        busca = Tema.campoComDica(dicaBusca);
        JPanel barra = (filtro == null)
                ? barraBusca(busca, this::pesquisar)
                : barraBusca(busca, this::pesquisar, filtro);

        Tema.Cartao form = Tema.formulario(tituloFormulario, camposFormulario);
        form.add(Tema.barraAcoes(this::salvar, this::novo, this::excluir));

        JPanel lateral = Tema.transparente(new BorderLayout());
        lateral.add(form, BorderLayout.NORTH);
        lateral.setPreferredSize(new Dimension(348, 0));

        tabela = Tema.tabelaVazia(colunas);
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || atualizandoTabela) {
                return;
            }
            int linha = tabela.getSelectedRow();
            if (linha >= 0 && linha < itens.size()) {
                selecionado = itens.get(linha);
                preencher(selecionado);
            }
        });

        JPanel centro = Tema.transparente(new BorderLayout(20, 0));
        centro.add(Tema.comScroll(tabela), BorderLayout.CENTER);
        centro.add(lateral, BorderLayout.EAST);

        corpo.add(barra, BorderLayout.NORTH);
        corpo.add(centro, BorderLayout.CENTER);
    }

    // ---------- O que cada tela define ----------

    /** Monta a consulta. Roda na thread da tela: leia aqui os filtros dos combos. */
    protected abstract Banco.Operacao<List<T>> consulta(String textoBusca);

    protected abstract Integer idDe(T item);

    protected abstract Object[] linha(T item);

    protected abstract void preencher(T item);

    protected abstract void limpar();

    /** Lê o formulário e valida. Lance {@link RegraNegocioException} se algo estiver errado. */
    protected abstract T ler(Integer idAtual);

    /** Grava (roda na thread do banco). Devolve o id. */
    protected abstract int gravar(T item) throws SQLException;

    /** Exclui (roda na thread do banco). */
    protected abstract void remover(T item) throws SQLException;

    /** Para telas que precisam carregar listas antes (ex.: instrutores). Chame {@code concluir} ao terminar. */
    protected void prepararDados(Runnable concluir) {
        concluir.run();
    }

    protected String nomeDe(T item) {
        return "este registro";
    }

    // ---------- Comportamento comum ----------

    @Override
    public void aoExibir() {
        prepararDados(this::pesquisar);
    }

    protected final void pesquisar() {
        pesquisar(null);
    }

    private void pesquisar(Integer selecionarId) {
        carregar(consulta(busca.getText()), lista -> {
            itens = lista;
            Object[][] dados = new Object[lista.size()][];
            for (int i = 0; i < dados.length; i++) {
                dados[i] = linha(lista.get(i));
            }
            atualizandoTabela = true;
            try {
                Tema.preencher(tabela, dados);
                Integer alvo = selecionarId != null ? selecionarId : (selecionado == null ? null : idDe(selecionado));
                selecionado = null;
                for (int i = 0; alvo != null && i < lista.size(); i++) {
                    if (alvo.equals(idDe(lista.get(i)))) {
                        tabela.setRowSelectionInterval(i, i);
                        selecionado = lista.get(i);
                        break;
                    }
                }
            } finally {
                atualizandoTabela = false;
            }
            if (selecionado != null) {
                preencher(selecionado);
            }
        });
    }

    private void novo() {
        selecionado = null;
        atualizandoTabela = true;
        tabela.clearSelection();
        atualizandoTabela = false;
        limpar();
    }

    private void salvar() {
        if (gravando) {
            return;
        }
        T item;
        try {
            item = ler(selecionado == null ? null : idDe(selecionado));
        } catch (RegraNegocioException e) {
            aviso(e.getMessage());
            return;
        }
        gravando = true;
        executar(() -> gravar(item), id -> {
            gravando = false;
            pesquisar(id);
        }, () -> gravando = false);
    }

    private void excluir() {
        if (gravando) {
            return;
        }
        if (selecionado == null) {
            aviso("Selecione um registro na tabela para excluir.");
            return;
        }
        T item = selecionado;
        if (!confirmar("Excluir " + nomeDe(item) + "?")) {
            return;
        }
        gravando = true;
        executar(() -> {
            remover(item);
            return null;
        }, ok -> {
            gravando = false;
            selecionado = null;
            limpar();
            pesquisar();
        }, () -> gravando = false);
    }
}
