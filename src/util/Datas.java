package util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Datas sempre calculadas no fuso do computador do usuário e enviadas ao banco como parâmetro.
 * (O Supabase roda em UTC: usar current_date/now() no SQL erraria o "hoje" à noite no Brasil.)
 */
public final class Datas {

    private Datas() { }

    public static LocalDate hoje() {
        return LocalDate.now();
    }

    public static OffsetDateTime inicioDoDia(LocalDate d) {
        return d.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    /** Início do dia seguinte (limite superior exclusivo). */
    public static OffsetDateTime fimDoDia(LocalDate d) {
        return inicioDoDia(d.plusDays(1));
    }
}
