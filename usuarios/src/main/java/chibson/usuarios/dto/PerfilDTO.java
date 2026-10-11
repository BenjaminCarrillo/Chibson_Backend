package chibson.usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos que el propio usuario puede editar en "Mi perfil" (HU-12). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilDTO {
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private String username;
    private String telefono;
    private String region;
    private String ciudadComuna;
    /** Opcional. */
    private String condominio;
    private String numero;
}
