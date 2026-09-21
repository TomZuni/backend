package cl.duoc.banco.historial.domain;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByCuentaId(Long cuentaId, Sort sort);

    List<Movimiento> findByTransaccion(String transaccion, Sort sort);
}
