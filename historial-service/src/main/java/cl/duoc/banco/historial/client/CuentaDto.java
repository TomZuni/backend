package cl.duoc.banco.historial.client;

import java.math.BigDecimal;

/** Vista de una cuenta tal como la expone cuentas-service. */
public record CuentaDto(Long cuentaId, String nombre, BigDecimal saldo, Integer edad, String tipo) {
}
