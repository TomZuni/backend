package cl.duoc.banco.cuentas.service;

import java.math.BigDecimal;

/** sentido: A_FAVOR_DEL_CLIENTE (ahorro) | A_PAGAR_POR_EL_CLIENTE (prestamo, hipoteca) */
public record InteresCuenta(
        Long cuentaId,
        String nombre,
        String tipo,
        BigDecimal saldo,
        BigDecimal tasaAnual,
        BigDecimal interesMensual,
        String sentido) {
}
