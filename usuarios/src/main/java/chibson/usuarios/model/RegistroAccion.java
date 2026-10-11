package chibson.usuarios.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acción de un administrador en cualquier microservicio (HU-73).
 * Los registros son de solo lectura: no hay endpoints para editarlos ni borrarlos.
 */
@Entity
@Table(name = "registro_accion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRegistro;

    @Column(nullable = false, length = 12)
    private String rutAdministrador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAccion tipo;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_registro")
    private List<DetalleCambio> cambios = new ArrayList<>();
}
