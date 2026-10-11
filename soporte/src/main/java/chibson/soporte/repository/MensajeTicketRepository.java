package chibson.soporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import chibson.soporte.model.MensajeTicket;

@Repository
public interface MensajeTicketRepository extends JpaRepository<MensajeTicket, Long> {
}
