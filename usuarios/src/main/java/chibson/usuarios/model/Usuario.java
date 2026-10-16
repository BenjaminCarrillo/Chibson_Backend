package chibson.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Usuario de la tienda. El RUT es la clave primaria (nota técnica de EP2).
 * La contraseña nunca se devuelve en las respuestas: para eso existe UsuarioDTO (HU-11).
 */
@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    /** Formato normalizado: sin puntos y con guion, por ejemplo 12345678-5. */
    @Id
    @Column(length = 12)
    private String rut;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 50)
    private String primerApellido;

    @Column(nullable = false, length = 50)
    private String segundoApellido;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false)
    private String password;

    // Dirección de envío predeterminada (HU-12)
    private String region;
    private String ciudadComuna;
    private String condominio;
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado;

    private LocalDate fechaRegistro;

    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    /**
     * Recuperación de contraseña (HU-10). Cambia la contraseña solo si el correo ingresado es el de
     * esta cuenta. Devuelve false si no coincide, y en ese caso no cambia nada.
     */
    public boolean recuperarPassword(String correoIngresado, String nuevaPassword) {
        if (correoIngresado == null || !correo.equalsIgnoreCase(correoIngresado.trim())) {
            return false;
        }
        this.password = nuevaPassword;
        return true;
    }
}
