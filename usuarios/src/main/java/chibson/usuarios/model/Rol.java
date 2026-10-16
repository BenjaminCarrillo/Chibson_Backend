package chibson.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/** Rol del usuario: ADMINISTRADOR o CLIENTE (HU-70, HU-71). */
@Entity
@Table(name = "rol")
@Data
@AllArgsConstructor
public class Rol {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String CLIENTE = "CLIENTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRol;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "rol_permiso",
            joinColumns = @JoinColumn(name = "id_rol"),
            inverseJoinColumns = @JoinColumn(name = "id_permiso"))
    private Set<Permiso> permisos;

    /**
     * Constructor vacío (lo usan JPA y Jackson). La lista se inicializa aquí y no en la declaración
     * del campo: con Java 25, el compilador de VS Code marca error si un campo tiene inicializador y
     * además lo asigna el constructor con todos los argumentos que genera @AllArgsConstructor.
     */
    public Rol() {
        this.permisos = new HashSet<>();
    }
}
