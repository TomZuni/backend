package cl.duoc.banco.historial.service;

import cl.duoc.banco.historial.domain.Movimiento;

import java.math.BigDecimal;
import java.util.List;

public record HistorialCuenta(
        Long cuentaId,
        String titular,
        String tipoCuenta,
        String origenTitular,
        int totalMovimientos,
        BigDecimal balance,
        List<Movimiento> movimientos) {
}
