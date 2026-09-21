package cl.duoc.banco.cuentas.migracion;

import cl.duoc.banco.cuentas.domain.Cuenta;
import cl.duoc.banco.cuentas.domain.CuentaRepository;
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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Migra intereses.csv (sistema legacy) a la tabla de cuentas.
 *
 * Reglas de calidad (el primer incumplimiento define el motivo de rechazo):
 *  1. menos de 5 columnas                                   -> FILA_INCOMPLETA
 *  2. cuenta_id no numerico                                 -> ID_INVALIDO
 *  3. nombre vacio o "Unknown"                              -> NOMBRE_INVALIDO
 *  4. saldo vacio / no numerico / negativo                  -> SALDO_VACIO / SALDO_NO_NUMERICO / SALDO_NEGATIVO
 *  5. edad vacia / fuera del rango 18..90                   -> EDAD_VACIA / EDAD_FUERA_DE_RANGO
 *  6. tipo distinto de ahorro|prestamo|hipoteca             -> TIPO_INVALIDO
 *  7. cuenta_id ya migrada (se conserva el primer registro valido):
 *       mismos datos                                        -> REGISTRO_DUPLICADO
 *       datos distintos                                     -> CUENTA_ID_REPETIDO
 */
@Service
public class MigracionService {

    private static final Set<String> TIPOS_VALIDOS = Set.of("ahorro", "prestamo", "hipoteca");

    private final CuentaRepository repositorio;
    private final ResourceLoader cargador;
    private final String semana;
    private final AtomicReference<MigracionReport> ultimoReporte = new AtomicReference<>();

    public MigracionService(CuentaRepository repositorio,
                            ResourceLoader cargador,
                            @Value("${banco.migracion.semana:semana_3}") String semana) {
        this.repositorio = repositorio;
        this.cargador = cargador;
        this.semana = semana;
    }

    @Transactional
    public MigracionReport migrar() {
        String ubicacion = "classpath:data/" + semana + "/intereses.csv";
        Resource recurso = cargador.getResource(ubicacion);

        Map<String, Integer> rechazos = new TreeMap<>();
        Map<Long, Cuenta> validas = new LinkedHashMap<>();
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
                String nombre = c[1].strip();
                if (nombre.isEmpty() || nombre.equalsIgnoreCase("unknown")) {
                    contar(rechazos, "NOMBRE_INVALIDO");
                    continue;
                }
                String saldoTxt = c[2].strip();
                if (saldoTxt.isEmpty()) {
                    contar(rechazos, "SALDO_VACIO");
                    continue;
                }
                BigDecimal saldo = aDecimal(saldoTxt);
                if (saldo == null) {
                    contar(rechazos, "SALDO_NO_NUMERICO");
                    continue;
                }
                if (saldo.signum() < 0) {
                    contar(rechazos, "SALDO_NEGATIVO");
                    continue;
                }
                String edadTxt = c[3].strip();
                if (edadTxt.isEmpty()) {
                    contar(rechazos, "EDAD_VACIA");
                    continue;
                }
                Integer edad = aInt(edadTxt);
                if (edad == null || edad < 18 || edad > 90) {
                    contar(rechazos, "EDAD_FUERA_DE_RANGO");
                    continue;
                }
                String tipo = c[4].strip().toLowerCase(Locale.ROOT);
                if (!TIPOS_VALIDOS.contains(tipo)) {
                    contar(rechazos, "TIPO_INVALIDO");
                    continue;
                }
                Cuenta existente = validas.get(cuentaId);
                if (existente != null) {
                    boolean igual = existente.getNombre().equals(nombre)
                            && existente.getSaldo().compareTo(saldo) == 0
                            && existente.getEdad().equals(edad)
                            && existente.getTipo().equals(tipo);
                    contar(rechazos, igual ? "REGISTRO_DUPLICADO" : "CUENTA_ID_REPETIDO");
                    continue;
                }
                validas.put(cuentaId, new Cuenta(cuentaId, nombre, saldo, edad, tipo));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo " + ubicacion, e);
        }

        repositorio.deleteAllInBatch();
        repositorio.saveAll(validas.values());

        MigracionReport reporte = new MigracionReport(
                "intereses.csv (" + semana + ")", leidos, validas.size(), leidos - validas.size(),
                rechazos, Map.of(), LocalDateTime.now());
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

    private static Integer aInt(String texto) {
        try {
            return Integer.valueOf(texto);
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
}
