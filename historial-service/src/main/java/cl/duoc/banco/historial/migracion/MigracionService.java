package cl.duoc.banco.historial.migracion;

import cl.duoc.banco.historial.domain.Movimiento;
import cl.duoc.banco.historial.domain.MovimientoRepository;
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
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Migra cuentas_anuales.csv (historial de operaciones) a la tabla de movimientos.
 *
 * Reglas de calidad (el primer incumplimiento define el motivo de rechazo):
 *  1. menos de 5 columnas                                        -> FILA_INCOMPLETA
 *  2. cuenta_id no numerico                                      -> ID_INVALIDO
 *  3. fecha invalida (formatos aceptados: yyyy-MM-dd, yyyy/MM/dd,
 *     dd-MM-yyyy, dd/MM/yyyy)                                    -> FECHA_INVALIDA
 *  4. transaccion distinta de deposito|retiro|compra|pago
 *     (se normaliza: minusculas y sin tildes)                    -> TIPO_INVALIDO
 *  5. monto vacio / no numerico / cero                           -> MONTO_VACIO / MONTO_NO_NUMERICO / MONTO_CERO
 *  6. signo incoherente: deposito debe ser > 0; retiro, compra
 *     y pago deben ser < 0                                       -> SIGNO_INCONSISTENTE
 *  7. registro repetido (misma cuenta, fecha, tipo, monto y
 *     descripcion)                                               -> REGISTRO_DUPLICADO
 * Correcciones automaticas: FECHA_NORMALIZADA_A_ISO, TIPO_NORMALIZADO ("depósito" -> "deposito")
 * y DESCRIPCION_POR_DEFECTO (descripcion vacia -> "Sin descripción").
 */
@Service
public class MigracionService {

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            formato("uuuu-MM-dd"), formato("uuuu/MM/dd"), formato("dd-MM-uuuu"), formato("dd/MM/uuuu"));
    private static final Set<String> TIPOS_VALIDOS = Set.of("deposito", "retiro", "compra", "pago");
    private static final String DESCRIPCION_DEFECTO = "Sin descripción";

    private record FechaLeida(LocalDate fecha, boolean formatoNoIso) {
    }

    private final MovimientoRepository repositorio;
    private final ResourceLoader cargador;
    private final String semana;
    private final AtomicReference<MigracionReport> ultimoReporte = new AtomicReference<>();

    public MigracionService(MovimientoRepository repositorio,
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
        String ubicacion = "classpath:data/" + semana + "/cuentas_anuales.csv";
        Resource recurso = cargador.getResource(ubicacion);

        Map<String, Integer> rechazos = new TreeMap<>();
        Map<String, Integer> correcciones = new TreeMap<>();
        Set<String> claves = new HashSet<>();
        List<Movimiento> validos = new ArrayList<>();
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
                if (c.length < 5) {
                    contar(rechazos, "FILA_INCOMPLETA");
                    continue;
                }
                Long cuentaId = aLong(c[0].strip());
                if (cuentaId == null) {
                    contar(rechazos, "ID_INVALIDO");
                    continue;
                }
                FechaLeida fecha = leerFecha(c[1].strip());
                if (fecha == null) {
                    contar(rechazos, "FECHA_INVALIDA");
                    continue;
                }
                String tipoOriginal = c[2].strip();
                String tipo = sinTildes(tipoOriginal.toLowerCase(Locale.ROOT));
                if (!TIPOS_VALIDOS.contains(tipo)) {
                    contar(rechazos, "TIPO_INVALIDO");
                    continue;
                }
                String montoTxt = c[3].strip();
                if (montoTxt.isEmpty()) {
                    contar(rechazos, "MONTO_VACIO");
                    continue;
                }
                BigDecimal monto = aDecimal(montoTxt);
                if (monto == null) {
                    contar(rechazos, "MONTO_NO_NUMERICO");
                    continue;
                }
                if (monto.signum() == 0) {
                    contar(rechazos, "MONTO_CERO");
                    continue;
                }
                boolean esDeposito = tipo.equals("deposito");
                if ((esDeposito && monto.signum() < 0) || (!esDeposito && monto.signum() > 0)) {
                    contar(rechazos, "SIGNO_INCONSISTENTE");
                    continue;
                }
                String descripcionOriginal = c[4].strip();
                String descripcion = descripcionOriginal.isEmpty() ? DESCRIPCION_DEFECTO : descripcionOriginal;

                String clave = cuentaId + "|" + fecha.fecha() + "|" + tipo + "|" + monto.stripTrailingZeros().toPlainString() + "|" + descripcion;
                if (!claves.add(clave)) {
                    contar(rechazos, "REGISTRO_DUPLICADO");
                    continue;
                }
                validos.add(new Movimiento(cuentaId, fecha.fecha(), tipo, monto, descripcion));
                if (fecha.formatoNoIso()) {
                    contar(correcciones, "FECHA_NORMALIZADA_A_ISO");
                }
                if (!tipoOriginal.equals(tipo)) {
                    contar(correcciones, "TIPO_NORMALIZADO");
                }
                if (descripcionOriginal.isEmpty()) {
                    contar(correcciones, "DESCRIPCION_POR_DEFECTO");
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo " + ubicacion, e);
        }

        repositorio.deleteAllInBatch();
        repositorio.saveAll(validos);

        MigracionReport reporte = new MigracionReport(
                "cuentas_anuales.csv (" + semana + ")", leidos, validos.size(), leidos - validos.size(),
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

    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
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
