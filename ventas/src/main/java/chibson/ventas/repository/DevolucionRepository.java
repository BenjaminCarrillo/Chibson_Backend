package chibson.ventas.repository;

import chibson.ventas.model.EstadoDevolucion;
import chibson.ventas.model.SolicitudDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevolucionRepository extends JpaRepository<SolicitudDevolucion, Long> {

    List<SolicitudDevolucion> findByRutClienteOrderByFechaSolicitudDesc(String rutCliente);

    List<SolicitudDevolucion> findAllByOrderByFechaSolicitudDesc();

    long countByEstado(EstadoDevolucion estado);

    /** Solicitudes anteriores del mismo pedido que no fueron rechazadas (HU-39 criterio 3). */
    List<SolicitudDevolucion> findByPedidoNumeroPedidoAndEstadoNot(Long numeroPedido, EstadoDevolucion estado);
}
