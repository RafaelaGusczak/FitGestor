package view;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.util.function.IntFunction;

/**
 * Máscara automática para campos de texto: o usuário digita só números e os separadores
 * (pontos, traços, barras, parênteses) entram sozinhos. Também formata o texto colocado por setText.
 */
public final class Mascara {

    private Mascara() { }

    public static void cpf(JTextField campo) {
        aplicar(campo, 11, n -> "###.###.###-##");
    }

    public static void data(JTextField campo) {
        aplicar(campo, 8, n -> "##/##/####");
    }

    public static void hora(JTextField campo) {
        aplicar(campo, 4, n -> "##:##");
    }

    /** (47) 9999-9999 com 10 dígitos e (47) 99999-9999 com 11. */
    public static void telefone(JTextField campo) {
        aplicar(campo, 11, n -> n > 10 ? "(##) #####-####" : "(##) ####-####");
    }

    private static void aplicar(JTextField campo, int maxDigitos, IntFunction<String> padrao) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new Filtro(campo, maxDigitos, padrao));
    }

    private static String digitos(CharSequence s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    /** Coloca os separadores só quando há um dígito depois deles. */
    private static String formatar(String digitos, String padrao) {
        StringBuilder sb = new StringBuilder();
        int d = 0;
        for (int i = 0; i < padrao.length() && d < digitos.length(); i++) {
            char p = padrao.charAt(i);
            sb.append(p == '#' ? digitos.charAt(d++) : p);
        }
        return sb.toString();
    }

    private static final class Filtro extends DocumentFilter {
        private final JTextField campo;
        private final int maxDigitos;
        private final IntFunction<String> padrao;

        Filtro(JTextField campo, int maxDigitos, IntFunction<String> padrao) {
            this.campo = campo;
            this.maxDigitos = maxDigitos;
            this.padrao = padrao;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String texto, javax.swing.text.AttributeSet a)
                throws BadLocationException {
            replace(fb, offset, 0, texto, a);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int tamanho) throws BadLocationException {
            replace(fb, offset, tamanho, "", null);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int tamanho, String texto, javax.swing.text.AttributeSet a)
                throws BadLocationException {
            String atual = fb.getDocument().getText(0, fb.getDocument().getLength());
            String novoTexto = texto == null ? "" : texto;
            String antes = digitos(atual.substring(0, offset));
            String depois = digitos(atual.substring(offset + tamanho));
            String inseridos = digitos(novoTexto);

            // Apagar só um separador (ex.: backspace sobre o ponto) apaga o dígito anterior.
            if (tamanho > 0 && inseridos.isEmpty() && digitos(atual.substring(offset, offset + tamanho)).isEmpty()
                    && !antes.isEmpty()) {
                antes = antes.substring(0, antes.length() - 1);
            }

            String todos = antes + inseridos + depois;
            if (todos.length() > maxDigitos) {
                todos = todos.substring(0, maxDigitos);
            }
            int digitosAteCursor = Math.min(antes.length() + inseridos.length(), todos.length());
            String formatado = formatar(todos, padrao.apply(todos.length()));
            if (formatado.equals(atual)) {
                return;
            }
            fb.replace(0, atual.length(), formatado, a);
            campo.setCaretPosition(posicaoApos(formatado, digitosAteCursor));
        }

        private static int posicaoApos(String formatado, int qtdDigitos) {
            if (qtdDigitos <= 0) {
                return 0;
            }
            int contados = 0;
            for (int i = 0; i < formatado.length(); i++) {
                if (Character.isDigit(formatado.charAt(i)) && ++contados == qtdDigitos) {
                    return i + 1;
                }
            }
            return formatado.length();
        }
    }
}
