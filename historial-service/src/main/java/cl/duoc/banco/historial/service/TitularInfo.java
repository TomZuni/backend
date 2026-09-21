package cl.duoc.banco.historial.service;

/**
 * origen: "cuentas-service" (dato fresco) | "cuenta-no-registrada" | "FALLBACK: cuentas-service no disponible"
 */
public record TitularInfo(String titular, String tipoCuenta, String origen) {
}
