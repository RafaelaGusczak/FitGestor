package db;

import javax.swing.SwingUtilities;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Acesso ao Postgres do Supabase via JDBC.
 *
 * Todas as operações de banco rodam em UMA thread própria, com UMA conexão reaproveitada.
 * Assim a tela nunca congela esperando a rede e as transações não se misturam.
 * Os DAOs só podem ser chamados dentro de {@link #async}, {@link #sync} ou {@link #transacao}.
 */
public final class Banco {

    private Banco() { }

    @FunctionalInterface
    public interface Operacao<T> {
        T executar() throws SQLException;
    }

    private static final long OCIOSO_MS = 20_000;
    private static final Thread[] THREAD_BANCO = new Thread[1];
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "banco");
        t.setDaemon(true);
        THREAD_BANCO[0] = t;
        return t;
    });

    private static Connection conexao;
    private static long ultimoUso;

    /** Conexão atual. Só pode ser usada na thread do banco. */
    public static Connection conexao() throws SQLException {
        if (Thread.currentThread() != THREAD_BANCO[0]) {
            throw new IllegalStateException("Acesso ao banco fora da thread do banco: use Banco.async/sync.");
        }
        long agora = System.currentTimeMillis();
        boolean vencida = conexao == null || conexao.isClosed()
                || (agora - ultimoUso > OCIOSO_MS && !conexao.isValid(3));
        if (vencida) {
            fechar();
            conexao = abrir();
        }
        ultimoUso = agora;
        return conexao;
    }

    private static void fechar() {
        if (conexao != null) {
            try {
                conexao.close();
            } catch (SQLException ignorada) {
                // já estava quebrada
            }
            conexao = null;
        }
    }

    private static Connection abrir() throws SQLException {
        Properties cfg = lerConfiguracao();
        String url = cfg.getProperty("url", "").trim();
        String usuario = cfg.getProperty("user", "").trim();
        String senha = cfg.getProperty("password", "");
        if (url.isEmpty() || usuario.isEmpty() || senha.isEmpty() || senha.contains("SUA-SENHA")) {
            throw new SQLException("Configuração do banco incompleta: preencha url, user e password no "
                    + "db.properties (ou use as variáveis DB_URL, DB_USER, DB_PASSWORD).");
        }
        Properties p = new Properties();
        p.setProperty("user", usuario);
        p.setProperty("password", senha);
        p.setProperty("ApplicationName", "FitGestor");
        p.setProperty("connectTimeout", "10");
        p.setProperty("socketTimeout", "30");
        Connection c = DriverManager.getConnection(url, p);
        c.setAutoCommit(true);
        return c;
    }

    /** Variáveis de ambiente têm prioridade sobre o arquivo db.properties. */
    private static Properties lerConfiguracao() throws SQLException {
        Properties cfg = new Properties();
        java.util.List<java.io.File> candidatos = candidatosConfig();
        java.io.File achado = null;
        for (java.io.File f : candidatos) {
            if (f.isFile()) {
                achado = f;
                break;
            }
        }
        if (achado != null) {
            try (FileInputStream in = new FileInputStream(achado)) {
                cfg.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new SQLException("Não foi possível ler " + achado.getAbsolutePath() + ": " + e.getMessage());
            }
        }
        sobrepor(cfg, "url", "DB_URL");
        sobrepor(cfg, "user", "DB_USER");
        sobrepor(cfg, "password", "DB_PASSWORD");
        if (achado == null && (cfg.getProperty("url") == null || cfg.getProperty("password") == null)) {
            StringBuilder sb = new StringBuilder("Arquivo db.properties não encontrado. Procurei em:");
            for (java.io.File f : candidatos) {
                sb.append("\n  ").append(f.getAbsolutePath());
            }
            throw new SQLException(sb.toString());
        }
        return cfg;
    }

    /** Pasta de trabalho, pasta do projeto (ao lado de src/lib/out) e pastas acima do código compilado. */
    private static java.util.List<java.io.File> candidatosConfig() {
        java.util.List<java.io.File> lista = new java.util.ArrayList<>();
        String nome = System.getProperty("fitgestor.config", "db.properties");
        java.io.File dado = new java.io.File(nome);
        lista.add(dado.getAbsoluteFile());
        if (!dado.isAbsolute()) {
            try {
                java.io.File codigo = new java.io.File(
                        Banco.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                java.io.File pasta = codigo.isFile() ? codigo.getParentFile() : codigo;
                for (int i = 0; i < 4 && pasta != null; i++) {
                    lista.add(new java.io.File(pasta, nome));
                    pasta = pasta.getParentFile();
                }
            } catch (Exception ignorada) {
                // sem localização do código: só vale a pasta de trabalho
            }
        }
        return lista;
    }

    private static void sobrepor(Properties cfg, String chave, String variavel) {
        String v = System.getenv(variavel);
        if (v != null && !v.isBlank()) {
            cfg.setProperty(chave, v);
        }
    }

    /** Executa em segundo plano; os dois callbacks voltam na thread da interface. */
    public static <T> void async(Operacao<T> op, Consumer<T> aoConcluir, Consumer<Exception> aoFalhar) {
        EXECUTOR.submit(() -> {
            try {
                T resultado = op.executar();
                SwingUtilities.invokeLater(() -> aoConcluir.accept(resultado));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> aoFalhar.accept(e));
            }
        });
    }

    /** Executa e espera o resultado. Não chamar na thread da interface. */
    public static <T> T sync(Operacao<T> op) throws Exception {
        try {
            return EXECUTOR.submit(() -> op.executar()).get();
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof Exception ex) {
                throw ex;
            }
            throw e;
        }
    }

    /** Agrupa várias operações em uma transação: confirma se tudo der certo, desfaz se algo falhar. */
    public static <T> T transacao(Operacao<T> op) throws SQLException {
        Connection c = conexao();
        c.setAutoCommit(false);
        try {
            T resultado = op.executar();
            c.commit();
            return resultado;
        } catch (SQLException | RuntimeException e) {
            try {
                c.rollback();
            } catch (SQLException ignorada) {
                fechar();
            }
            throw e;
        } finally {
            if (conexao != null) {
                try {
                    conexao.setAutoCommit(true);
                } catch (SQLException ignorada) {
                    fechar();
                }
            }
        }
    }

    /** Escapa %, _ e \ para uso em LIKE/ILIKE. */
    public static String parteDe(String texto) {
        String t = texto == null ? "" : texto.trim();
        return "%" + t.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}