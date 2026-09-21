package cl.duoc.banco.transacciones.service;

import cl.duoc.banco.transacciones.domain.Transaccion;
import cl.duoc.banco.transacciones.domain.TransaccionRepository;
import cl.duoc.banco.transacciones.resilience.FaultSimulator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Consulta agregada protegida con tolerancia a fallos.
 * Orden de los patrones (de afuera hacia adentro): Retry -> CircuitBreaker -> RateLimiter.
 * Si todo falla, el fallback (declarado en @Retry, el aspecto mas externo) responde con el
 * ultimo resumen calculado, marcado como DEGRADADO.
 */
@Service
public class ResumenService {

    private static final Logger log = LoggerFactory.getLogger(ResumenService.class);

    private final TransaccionRepository repositorio;
    private final FaultSimulator simulador;
    private final AtomicReference<ResumenTransacciones> ultimoResumen = new AtomicReference<>();

    public ResumenService(TransaccionRepository repositorio, FaultSimulator simulador) {
        this.repositorio = repositorio;
        this.simulador = simulador;
    }

    @Retry(name = "resumen", fallbackMethod = "resumenFallback")
    @CircuitBreaker(name = "resumen")
    @RateLimiter(name = "resumen")
    public ResumenTransacciones obtenerResumen() {
        simulador.verificar();

        List<Transaccion> todas = repositorio.findAll();
        BigDecimal creditos = sumar(todas, "credito");
        BigDecimal debitos = sumar(todas, "debito");
        ResumenTransacciones resumen = new ResumenTransacciones(
                todas.size(), creditos, debitos, creditos.subtract(debitos), "OK", LocalDateTime.now());
        ultimoResumen.set(resumen);
        return resumen;
    }

    public ResumenTransacciones resumenFallback(Throwable causa) {
        log.warn("Fallback de resumen de transacciones activado: {}", causa.toString());
        ResumenTransacciones anterior = ultimoResumen.get();
        if (anterior != null) {
            return anterior.conEstado("DEGRADADO_CACHE");
        }
        return new ResumenTransacciones(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "DEGRADADO_SIN_DATOS", LocalDateTime.now());
    }

    private static BigDecimal sumar(List<Transaccion> transacciones, String tipo) {
        return transacciones.stream()
                .filter(t -> tipo.equals(t.getTipo()))
                .map(Transaccion::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
