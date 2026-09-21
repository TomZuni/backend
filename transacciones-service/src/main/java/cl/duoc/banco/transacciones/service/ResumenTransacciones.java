package cl.duoc.banco.transacciones.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** estado: OK | DEGRADADO_CACHE | DEGRADADO_SIN_DATOS */
public record ResumenTransacciones(
        long totalTransacciones,
        BigDecimal totalCreditos,
        BigDecimal totalDebitos,
        BigDecimal balance,
        String estado,
        LocalDateTime calculadoEn) {

    public ResumenTransacciones conEstado(String nuevoEstado) {
        return new ResumenTransacciones(totalTransacciones, totalCreditos, totalDebitos, balance, nuevoEstado, calculadoEn);
    }
}
