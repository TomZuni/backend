package cl.duoc.banco.cuentas.service;

import cl.duoc.banco.cuentas.domain.Cuenta;
import cl.duoc.banco.cuentas.domain.CuentaRepository;
import cl.duoc.banco.cuentas.resilience.FaultSimulator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Consolidado de cuentas protegido con Retry -> CircuitBreaker -> RateLimiter.
 * El fallback (declarado en @Retry) responde con el ultimo consolidado conocido, marcado como DEGRADADO.
 */
@Service
public class ResumenService {

    private static final Logger log = LoggerFactory.getLogger(ResumenService.class);

    private final CuentaRepository repositorio;
    private final InteresService interesService;
    private final FaultSimulator simulador;
    private final AtomicReference<ResumenCuentas> ultimoResumen = new AtomicReference<>();

    public ResumenService(CuentaRepository repositorio, InteresService interesService, FaultSimulator simulador) {
        this.repositorio = repositorio;
        this.interesService = interesService;
        this.simulador = simulador;
    }

    @Retry(name = "resumen", fallbackMethod = "resumenFallback")
    @CircuitBreaker(name = "resumen")
    @RateLimiter(name = "resumen")
    public ResumenCuentas obtenerResumen() {
        simulador.verificar();

        List<Cuenta> cuentas = repositorio.findAll();
        Map<String, long[]> conteo = new TreeMap<>();
        Map<String, BigDecimal[]> montos = new TreeMap<>();
        for (Cuenta c : cuentas) {
            conteo.computeIfAbsent(c.getTipo(), k -> new long[1])[0]++;
            BigDecimal[] acumulado = montos.computeIfAbsent(c.getTipo(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            acumulado[0] = acumulado[0].add(c.getSaldo());
            acumulado[1] = acumulado[1].add(interesService.calcular(c).interesMensual());
        }
        Map<String, ResumenCuentas.DetalleTipo> porTipo = new TreeMap<>();
        conteo.forEach((tipo, n) ->
                porTipo.put(tipo, new ResumenCuentas.DetalleTipo(n[0], montos.get(tipo)[0], montos.get(tipo)[1])));

        ResumenCuentas resumen = new ResumenCuentas(cuentas.size(), porTipo, "OK", LocalDateTime.now());
        ultimoResumen.set(resumen);
        return resumen;
    }

    public ResumenCuentas resumenFallback(Throwable causa) {
        log.warn("Fallback de resumen de cuentas activado: {}", causa.toString());
        ResumenCuentas anterior = ultimoResumen.get();
        if (anterior != null) {
            return anterior.conEstado("DEGRADADO_CACHE");
        }
        return new ResumenCuentas(0, Map.of(), "DEGRADADO_SIN_DATOS", LocalDateTime.now());
    }
}
