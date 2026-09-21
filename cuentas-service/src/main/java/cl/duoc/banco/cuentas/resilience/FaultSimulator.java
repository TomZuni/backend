package cl.duoc.banco.cuentas.resilience;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Permite provocar fallas a demanda (solo ADMIN) para demostrar Retry, Circuit Breaker
 * y Fallback sin tener que apagar procesos.
 */
@Component
public class FaultSimulator {

    private final AtomicBoolean activa = new AtomicBoolean(false);

    public void verificar() {
        if (activa.get()) {
            throw new IllegalStateException("Falla simulada en cuentas-service");
        }
    }

    public void setActiva(boolean valor) {
        activa.set(valor);
    }

    public boolean isActiva() {
        return activa.get();
    }
}
