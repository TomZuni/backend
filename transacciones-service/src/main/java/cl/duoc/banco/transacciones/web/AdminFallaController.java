package cl.duoc.banco.transacciones.web;

import cl.duoc.banco.transacciones.resilience.FaultSimulator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/falla")
public class AdminFallaController {

    private final FaultSimulator simulador;

    public AdminFallaController(FaultSimulator simulador) {
        this.simulador = simulador;
    }

    @GetMapping
    public Map<String, Object> estado() {
        return Map.of("servicio", "transacciones-service", "fallaSimuladaActiva", simulador.isActiva());
    }

    /** POST /api/admin/falla?activa=true  (requiere rol ADMIN) */
    @PostMapping
    public Map<String, Object> configurar(@RequestParam boolean activa) {
        simulador.setActiva(activa);
        return Map.of("servicio", "transacciones-service", "fallaSimuladaActiva", simulador.isActiva());
    }
}
