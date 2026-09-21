package cl.duoc.banco.cuentas.service;

import cl.duoc.banco.cuentas.config.TasasProperties;
import cl.duoc.banco.cuentas.domain.Cuenta;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class InteresService {

    private static final BigDecimal MESES = BigDecimal.valueOf(12);

    private final TasasProperties tasas;

    public InteresService(TasasProperties tasas) {
        this.tasas = tasas;
    }

    /** interes mensual = saldo * tasaAnual / 12 (tasas provenientes del Config Server). */
    public InteresCuenta calcular(Cuenta cuenta) {
        BigDecimal tasa = tasas.tasaAnual(cuenta.getTipo());
        BigDecimal interes = cuenta.getSaldo().multiply(tasa).divide(MESES, 2, RoundingMode.HALF_UP);
        String sentido = "ahorro".equals(cuenta.getTipo()) ? "A_FAVOR_DEL_CLIENTE" : "A_PAGAR_POR_EL_CLIENTE";
        return new InteresCuenta(cuenta.getCuentaId(), cuenta.getNombre(), cuenta.getTipo(),
                cuenta.getSaldo(), tasa, interes, sentido);
    }
}
