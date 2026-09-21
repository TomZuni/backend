package cl.duoc.banco.cuentas.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/** estado: OK | DEGRADADO_CACHE | DEGRADADO_SIN_DATOS */
public record ResumenCuentas(
        long totalCuentas,
        Map<String, DetalleTipo> porTipo,
        String estado,
        LocalDateTime calculadoEn) {

    public record DetalleTipo(long cuentas, BigDecimal saldoTotal, BigDecimal interesMensualTotal) {
    }

    public ResumenCuentas conEstado(String nuevoEstado) {
        return new ResumenCuentas(totalCuentas, porTipo, nuevoEstado, calculadoEn);
    }
}
