package chibson.ventas.repository;

import chibson.ventas.model.HistorialEstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstadoPedido, Long> {
}
