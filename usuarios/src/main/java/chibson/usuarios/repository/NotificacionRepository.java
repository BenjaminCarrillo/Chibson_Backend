package chibson.usuarios.repository;

import chibson.usuarios.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioRutOrderByFechaDesc(String rut);

    void deleteByUsuarioRut(String rut);
}
