package cl.duoc.banco.cuentas.migracion;

import java.time.LocalDateTime;
import java.util.Map;

/** Resultado de la migracion: cuantos registros se leyeron, migraron y por que se rechazaron los demas. */
public record MigracionReport(
        String fuente,
        int registrosLeidos,
        int registrosMigrados,
        int registrosRechazados,
        Map<String, Integer> motivosRechazo,
        Map<String, Integer> correcciones,
        LocalDateTime ejecutadaEn) {
}
