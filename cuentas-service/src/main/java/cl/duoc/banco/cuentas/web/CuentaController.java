package cl.duoc.banco.cuentas.web;

import cl.duoc.banco.cuentas.domain.Cuenta;
import cl.duoc.banco.cuentas.domain.CuentaRepository;
import cl.duoc.banco.cuentas.migracion.MigracionReport;
import cl.duoc.banco.cuentas.migracion.MigracionService;
import cl.duoc.banco.cuentas.service.InteresCuenta;
import cl.duoc.banco.cuentas.service.InteresService;
import cl.duoc.banco.cuentas.service.ResumenCuentas;
import cl.duoc.banco.cuentas.service.ResumenService;
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
@RequestMapping("/api/cuentas")
public class CuentaController {

    private static final Sort ORDEN = Sort.by("cuentaId");

    private final CuentaRepository repositorio;
    private final InteresService interesService;
    private final ResumenService resumenService;
    private final MigracionService migracionService;

    public CuentaController(CuentaRepository repositorio,
                            InteresService interesService,
                            ResumenService resumenService,
                            MigracionService migracionService) {
        this.repositorio = repositorio;
        this.interesService = interesService;
        this.resumenService = resumenService;
        this.migracionService = migracionService;
    }

    /** GET /api/cuentas?tipo=ahorro|prestamo|hipoteca */
    @GetMapping
    public List<Cuenta> listar(@RequestParam(required = false) String tipo) {
        return (tipo == null || tipo.isBlank())
                ? repositorio.findAll(ORDEN)
                : repositorio.findByTipo(tipo.toLowerCase(), ORDEN);
    }

    /** Tambien es consumido por historial-service (via Feign + Eureka). */
    @GetMapping("/{cuentaId}")
    public Cuenta obtener(@PathVariable Long cuentaId) {
        return buscar(cuentaId);
    }

    @GetMapping("/{cuentaId}/intereses")
    public InteresCuenta intereses(@PathVariable Long cuentaId) {
        return interesService.calcular(buscar(cuentaId));
    }

    /** Consolidado por tipo de cuenta (protegido con Retry + CircuitBreaker + RateLimiter + Fallback). */
    @GetMapping("/resumen")
    public ResumenCuentas resumen() {
        return resumenService.obtenerResumen();
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

    private Cuenta buscar(Long cuentaId) {
        return repositorio.findById(cuentaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta " + cuentaId + " no encontrada"));
    }
}
