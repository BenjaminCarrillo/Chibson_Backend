package chibson.usuarios.repository;

import chibson.usuarios.model.RegistroAccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistroAccionRepository extends JpaRepository<RegistroAccion, Long> {

    List<RegistroAccion> findAllByOrderByFechaDesc();

    List<RegistroAccion> findByRutAdministradorOrderByFechaDesc(String rutAdministrador);
}
