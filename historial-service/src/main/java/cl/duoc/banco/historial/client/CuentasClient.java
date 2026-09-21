package cl.duoc.banco.historial.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente declarativo hacia cuentas-service. El nombre corresponde al registrado en Eureka,
 * por lo que no hay URLs fijas: el LoadBalancer resuelve la instancia via Service Discovery.
 */
@FeignClient(name = "cuentas-service")
public interface CuentasClient {

    @GetMapping("/api/cuentas/{cuentaId}")
    CuentaDto obtenerCuenta(@PathVariable("cuentaId") Long cuentaId);
}
