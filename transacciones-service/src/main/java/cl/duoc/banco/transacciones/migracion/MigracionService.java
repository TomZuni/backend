package cl.duoc.banco.transacciones.migracion;

import cl.duoc.banco.transacciones.domain.Transaccion;
import cl.duoc.banco.transacciones.domain.TransaccionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Migra transacciones.csv (sistema legacy) a la base de datos del microservicio.
 *
 * Reglas de calidad (en este orden; el primer incumplimiento define el motivo de rechazo):
 *  1. fila con menos de 4 columnas                     -> FILA_INCOMPLETA
 *  2. id no numerico                                   -> ID_INVALIDO
 *  3. fecha que no calza con yyyy-MM-dd, yyyy/MM/dd,
 *     dd-MM-yyyy ni dd/MM/yyyy (o fecha inexistente)   -> FECHA_INVALIDA
 *  4. monto vacio / no numerico / negativo o cero      -> MONTO_VACIO / MONTO_NO_NUMERICO / MONTO_NEGATIVO_O_CERO
 *  5. tipo distinto de credito|debito                  -> TIPO_INVALIDO
 *  6. id repetido (se conserva el primero)             -> ID_DUPLICADO
 * Correcciones automaticas: fechas en formato no ISO se normalizan (FECHA_NORMALIZADA_A_ISO).
 */
@Service
public class MigracionService {

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            formato("uuuu-MM-dd"), formato("uuuu/MM/dd"), formato("dd-MM-uuuu"), formato("dd/MM/uuuu"));

    private record FechaLeida(LocalDate fecha, boolean formatoNoIso) {
    }

    private final TransaccionRepository repositorio;
    private final ResourceLoader cargador;
    private final String semana;
    private final AtomicReference<MigracionReport> ultimoReporte = new AtomicReference<>();

    public MigracionService(TransaccionRepository repositorio,
                            ResourceLoader cargador,
                            @Value("${banco.migracion.semana:semana_3}") String semana) {
        this.repositorio = repositorio;
        this.cargador = cargador;
        this.semana = semana;
    }

    private static DateTimeFormatter formato(String patron) {
        return DateTimeFormatter.ofPattern(patron).withResolverStyle(ResolverStyle.STRICT);
    }

    @Transactional
    public MigracionReport migrar() {
        String ubicacion = "classpath:data/" + semana + "/transacciones.csv";
        Resource recurso = cargador.getResource(ubicacion);

        Map<String, Integer> rechazos = new TreeMap<>();
        Map<String, Integer> correcciones = new TreeMap<>();
        Map<Long, Transaccion> validas = new LinkedHashMap<>();
        int leidos = 0;

        try (BufferedReader lector = new BufferedReader(new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {
            lector.readLine(); // encabezado
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }
                leidos++;
                String[] c = linea.split(",", -1);
                if (c.length < 4) {
                    contar(rechazos, "FILA_INCOMPLETA");
                    continue;
                }
                Long id = aLong(c[0].strip());
                if (id == null) {
                    contar(rechazos, "ID_INVALIDO");
                    continue;
                }
                FechaLeida fecha = leerFecha(c[1].strip());
                if (fecha == null) {
                    contar(rechazos, "FECHA_INVALIDA");
                    continue;
                }
                String montoTxt = c[2].strip();
                if (montoTxt.isEmpty()) {
                    contar(rechazos, "MONTO_VACIO");
                    continue;
                }
                BigDecimal monto = aDecimal(montoTxt);
                if (monto == null) {
                    contar(rechazos, "MONTO_NO_NUMERICO");
                    continue;
                }
                if (monto.signum() <= 0) {
                    contar(rechazos, "MONTO_NEGATIVO_O_CERO");
                    continue;
                }
                String tipo = c[3].strip().toLowerCase(Locale.ROOT);
                if (!tipo.equals("credito") && !tipo.equals("debito")) {
                    contar(rechazos, "TIPO_INVALIDO");
                    continue;
                }
                if (validas.containsKey(id)) {
                    contar(rechazos, "ID_DUPLICADO");
                    continue;
                }
                validas.put(id, new Transaccion(id, fecha.fecha(), monto, tipo));
                if (fecha.formatoNoIso()) {
                    contar(correcciones, "FECHA_NORMALIZADA_A_ISO");
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo " + ubicacion, e);
        }

        repositorio.deleteAllInBatch();
        repositorio.saveAll(validas.values());

        MigracionReport reporte = new MigracionReport(
                "transacciones.csv (" + semana + ")", leidos, validas.size(), leidos - validas.size(),
                rechazos, correcciones, LocalDateTime.now());
        ultimoReporte.set(reporte);
        return reporte;
    }

    public MigracionReport ultimoReporte() {
        return ultimoReporte.get();
    }

    private static void contar(Map<String, Integer> mapa, String clave) {
        mapa.merge(clave, 1, Integer::sum);
    }

    private static Long aLong(String texto) {
        try {
            return Long.valueOf(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal aDecimal(String texto) {
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static FechaLeida leerFecha(String texto) {
        for (int i = 0; i < FORMATOS_FECHA.size(); i++) {
            try {
                return new FechaLeida(LocalDate.parse(texto, FORMATOS_FECHA.get(i)), i > 0);
            } catch (DateTimeParseException e) {
                // probar el siguiente formato
            }
        }
        return null;
    }
}
