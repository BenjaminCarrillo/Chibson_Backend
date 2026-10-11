package chibson.soporte.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import chibson.soporte.model.EstadoTicket;
import chibson.soporte.model.Ticket;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByRutClienteOrderByFechaCreacionDesc(String rutCliente);

    /** Del más antiguo al más reciente (HU-28). */
    List<Ticket> findAllByOrderByFechaCreacionAsc();

    List<Ticket> findByEstadoOrderByFechaCreacionAsc(EstadoTicket estado);

    long countByEstadoIn(Collection<EstadoTicket> estados);
}
