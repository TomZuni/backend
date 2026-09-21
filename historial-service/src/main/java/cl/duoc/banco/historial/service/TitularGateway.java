package cl.duoc.banco.historial.service;

import cl.duoc.banco.historial.client.CuentaDto;
import cl.duoc.banco.historial.client.CuentasClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Consulta al microservicio de cuentas con tolerancia a fallos:
 *  - Retry: reintenta ante fallas transitorias de red / 5xx.
 *  - CircuitBreaker: si cuentas-service cae, abre el circuito y deja de llamarlo por un tiempo.
 *  - Fallback: el historial sigue respondiendo, sin el nombre del titular, en vez de fallar completo.
 * Un 404 (cuenta inexistente) es una respuesta de negocio valida y NO cuenta como falla.
 */
@Service
public class TitularGateway {

    private static final Logger log = LoggerFactory.getLogger(TitularGateway.class);

    private final CuentasClient cuentasClient;

    public TitularGateway(CuentasClient cuentasClient) {
        this.cuentasClient = cuentasClient;
    }

    @Retry(name = "cuentasClient", fallbackMethod = "titularFallback")
    @CircuitBreaker(name = "cuentasClient")
    public TitularInfo consultarTitular(Long cuentaId) {
        try {
            CuentaDto cuenta = cuentasClient.obtenerCuenta(cuentaId);
            return new TitularInfo(cuenta.nombre(), cuenta.tipo(), "cuentas-service");
        } catch (FeignException.NotFound e) {
            return new TitularInfo(null, null, "cuenta-no-registrada");
        }
    }

    public TitularInfo titularFallback(Long cuentaId, Throwable causa) {
        log.warn("Fallback de titular para cuenta {} (cuentas-service no responde): {}", cuentaId, causa.toString());
        return new TitularInfo(null, null, "FALLBACK: cuentas-service no disponible");
    }
}
