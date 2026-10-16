package chibson.usuarios.repository;

import chibson.usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    long countByFechaRegistro(LocalDate fecha);

    /** Clientes registrados entre dos fechas, ambas incluidas (resumen general, HU-50). */
    long countByFechaRegistroBetweenAndRolNombre(LocalDate desde, LocalDate hasta, String rol);

    List<Usuario> findAllByOrderByFechaRegistroDesc();
}
