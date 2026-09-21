package cl.duoc.banco.transacciones.web;

import cl.duoc.banco.transacciones.domain.Transaccion;
import cl.duoc.banco.transacciones.domain.TransaccionRepository;
import cl.duoc.banco.transacciones.migracion.MigracionReport;
import cl.duoc.banco.transacciones.migracion.MigracionService;
import cl.duoc.banco.transacciones.service.ResumenService;
import cl.duoc.banco.transacciones.service.ResumenTransacciones;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private static final Sort ORDEN = Sort.by("fecha", "id");

    private final TransaccionRepository repositorio;
    private final ResumenService resumenService;
    private final MigracionService migracionService;

    public TransaccionController(TransaccionRepository repositorio,
                                 ResumenService resumenService,
                                 MigracionService migracionService) {
        this.repositorio = repositorio;
        this.resumenService = resumenService;
        this.migracionService = migracionService;
    }

    /** GET /api/transacciones?tipo=credito|debito */
    @GetMapping
    public List<Transaccion> listar(@RequestParam(required = false) String tipo) {
        return (tipo == null || tipo.isBlank())
                ? repositorio.findAll(ORDEN)
                : repositorio.findByTipo(tipo.toLowerCase(), ORDEN);
    }

    @GetMapping("/{id}")
    public Transaccion obtener(@PathVariable Long id) {
        return repositorio.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaccion " + id + " no encontrada"));
    }

    /** Totales de creditos/debitos (protegido con Retry + CircuitBreaker + RateLimiter + Fallback). */
    @GetMapping("/resumen")
    public ResumenTransacciones resumen() {
        return resumenService.obtenerResumen();
    }

    /** Resultado de la ultima migracion. */
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
