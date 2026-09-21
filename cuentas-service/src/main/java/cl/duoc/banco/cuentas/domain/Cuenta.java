package cl.duoc.banco.cuentas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "cuentas")
public class Cuenta {

    @Id
    private Long cuentaId;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo;

    @Column(nullable = false)
    private Integer edad;

    /** ahorro | prestamo | hipoteca */
    @Column(nullable = false, length = 20)
    private String tipo;

    protected Cuenta() {
    }

    public Cuenta(Long cuentaId, String nombre, BigDecimal saldo, Integer edad, String tipo) {
        this.cuentaId = cuentaId;
        this.nombre = nombre;
        this.saldo = saldo;
        this.edad = edad;
        this.tipo = tipo;
    }

    public Long getCuentaId() { return cuentaId; }
    public String getNombre() { return nombre; }
    public BigDecimal getSaldo() { return saldo; }
    public Integer getEdad() { return edad; }
    public String getTipo() { return tipo; }
}
