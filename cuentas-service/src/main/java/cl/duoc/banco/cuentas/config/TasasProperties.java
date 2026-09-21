package cl.duoc.banco.cuentas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Tasas de interes ANUALES por tipo de cuenta. Se leen de "banco.tasas.*",
 * definido en cuentas-service.yml dentro del Config Server.
 */
@ConfigurationProperties(prefix = "banco")
public record TasasProperties(Map<String, BigDecimal> tasas) {

    public TasasProperties {
        tasas = (tasas == null) ? Map.of() : tasas;
    }

    public BigDecimal tasaAnual(String tipo) {
        return tasas.getOrDefault(tipo, BigDecimal.ZERO);
    }
}
