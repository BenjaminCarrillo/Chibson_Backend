package chibson.contenido.repository;

import chibson.contenido.model.InformacionInstitucional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InformacionInstitucionalRepository extends JpaRepository<InformacionInstitucional, Long> {
}
