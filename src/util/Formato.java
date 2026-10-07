package util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Conversões entre texto da tela e valores (datas, dinheiro, CPF, horário). */
public final class Formato {

    private Formato() { }

    public static final String[] DIAS_SEMANA = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static DecimalFormat dinheiro() {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
    }

    public static String moeda(BigDecimal v) {
        return v == null ? "" : "R$ " + dinheiro().format(v);
    }

    public static String decimal(BigDecimal v) {
        return v == null ? "" : dinheiro().format(v).replace(".", "");
    }

    public static String data(LocalDate d) {
        return d == null ? "" : DATA.format(d);
    }

    public static String hora(LocalTime h) {
        return h == null ? "" : HORA.format(h);
    }

    /** Campo opcional: vazio vira null. */
    public static LocalDate lerData(String texto, String campo, boolean obrigatorio) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) {
            if (obrigatorio) {
                throw new RegraNegocioException("Informe " + campo + " (dd/mm/aaaa).");
            }
            return null;
        }
        try {
            return LocalDate.parse(t, DATA);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Data inválida em \"" + campo + "\". Use dd/mm/aaaa.");
        }
    }

    public static LocalTime lerHora(String texto, String campo) {
        try {
            return LocalTime.parse(texto.trim(), HORA);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Horário inválido em \"" + campo + "\". Use hh:mm.");
        }
    }

    /** Aceita "99,90", "99.90" ou "1.099,90". */
    public static BigDecimal lerValor(String texto, String campo) {
        String t = texto == null ? "" : texto.trim().replace("R$", "").trim();
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        }
        try {
            BigDecimal v = new BigDecimal(t);
            if (v.signum() < 0) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Valor inválido em \"" + campo + "\".");
        }
    }

    public static int lerInteiro(String texto, String campo) {
        try {
            int v = Integer.parseInt(texto.trim());
            if (v <= 0) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("\"" + campo + "\" deve ser um número maior que zero.");
        }
    }

    public static String obrigatorio(String texto, String campo) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) {
            throw new RegraNegocioException("Informe " + campo + ".");
        }
        return t;
    }

    /** Opcional: vazio vira null. */
    public static String opcional(String texto) {
        String t = texto == null ? "" : texto.trim();
        return t.isEmpty() ? null : t;
    }

    public static String soDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /** Exige 11 dígitos e devolve no formato 000.000.000-00. */
    public static String lerCpf(String texto) {
        String d = soDigitos(texto);
        if (d.length() != 11) {
            throw new RegraNegocioException("CPF deve ter 11 dígitos.");
        }
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    /** Nome do dia da semana no mesmo padrão gravado no banco ("Segunda", "Terça"...). */
    public static String diaSemana(LocalDate d) {
        return DIAS_SEMANA[d.getDayOfWeek().getValue() - 1];
    }

    public static String diaAbreviado(String dia) {
        return dia.substring(0, 3);
    }
}
