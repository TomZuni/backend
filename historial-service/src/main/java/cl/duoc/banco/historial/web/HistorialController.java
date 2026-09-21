package cl.duoc.banco.historial.web;

import cl.duoc.banco.historial.domain.Movimiento;
import cl.duoc.banco.historial.domain.MovimientoRepository;
import cl.duoc.banco.historial.migracion.MigracionReport;
import cl.duoc.banco.historial.migracion.MigracionService;
import cl.duoc.banco.historial.service.HistorialCuenta;
import cl.duoc.banco.historial.service.TitularGateway;
import cl.duoc.banco.historial.service.TitularInfo;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/historial")
public class HistorialController {

    private static final Sort ORDEN = Sort.by("fecha", "id");

    private final MovimientoRepository repositorio;
    private final TitularGateway titularGateway;
    private final MigracionService migracionService;

    public HistorialController(MovimientoRepository repositorio,
                               TitularGateway titularGateway,
                               MigracionService migracionService) {
        this.repositorio = repositorio;
        this.titularGateway = titularGateway;
        this.migracionService = migracionService;
    }

    /** GET /api/historial?transaccion=deposito|retiro|compra|pago */
    @GetMapping
    public List<Movimiento> listar(@RequestParam(required = false) String transaccion) {
        return (transaccion == null || transaccion.isBlank())
                ? repositorio.findAll(ORDEN)
                : repositorio.findByTransaccion(transaccion.toLowerCase(), ORDEN);
    }

    @GetMapping("/{id}")
    public Movimiento obtener(@PathVariable Long id) {
        return repositorio.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Movimiento " + id + " no encontrado"));
    }

    /**
     * Historial de una cuenta enriquecido con el titular, obtenido desde cuentas-service
     * (Feign + Eureka + Retry + CircuitBreaker + Fallback).
     */
    @GetMapping("/cuenta/{cuentaId}")
    public HistorialCuenta historialDeCuenta(@PathVariable Long cuentaId) {
        List<Movimiento> movimientos = repositorio.findByCuentaId(cuentaId, ORDEN);
        BigDecimal balance = movimientos.stream()
                .map(Movimiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TitularInfo titular = titularGateway.consultarTitular(cuentaId);
        return new HistorialCuenta(cuentaId, titular.titular(), titular.tipoCuenta(), titular.origen(),
                movimientos.size(), balance, movimientos);
    }

    @GetMapping("/migracion")
    public MigracionReport migracion() {
        return migracionService.ultimoReporte();
    }

    /** Vuelve a ejecutar la migracion (solo ADMIN). */
    @PostMapping("/migracion/reprocesar")
    public MigracionReport reprocesar() {
        return migracionService.migrar();
    }
}
