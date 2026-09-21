package cl.duoc.banco.historial.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "movimientos")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long cuentaId;

    @Column(nullable = false)
    private LocalDate fecha;

    /** deposito | retiro | compra | pago */
    @Column(nullable = false, length = 20)
    private String transaccion;

    /** positivo para deposito, negativo para retiro/compra/pago */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private String descripcion;

    protected Movimiento() {
    }

    public Movimiento(Long cuentaId, LocalDate fecha, String transaccion, BigDecimal monto, String descripcion) {
        this.cuentaId = cuentaId;
        this.fecha = fecha;
        this.transaccion = transaccion;
        this.monto = monto;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public Long getCuentaId() { return cuentaId; }
    public LocalDate getFecha() { return fecha; }
    public String getTransaccion() { return transaccion; }
    public BigDecimal getMonto() { return monto; }
    public String getDescripcion() { return descripcion; }
}
