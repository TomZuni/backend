package cl.duoc.banco.historial.migracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Ejecuta la migracion automaticamente al iniciar el microservicio. */
@Component
public class MigracionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionRunner.class);

    private final MigracionService servicio;

    public MigracionRunner(MigracionService servicio) {
        this.servicio = servicio;
    }

    @Override
    public void run(ApplicationArguments args) {
        MigracionReport r = servicio.migrar();
        log.info("Migracion {}: leidos={}, migrados={}, rechazados={}, motivos={}, correcciones={}",
                r.fuente(), r.registrosLeidos(), r.registrosMigrados(), r.registrosRechazados(),
                r.motivosRechazo(), r.correcciones());
    }
}
