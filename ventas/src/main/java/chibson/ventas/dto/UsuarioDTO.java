package chibson.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos del cliente que entrega ms-usuarios: nombre, estado y dirección del perfil. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private String rut;
    private String nombres;
    private String primerApellido;
    private String region;
    private String ciudadComuna;
    private String condominio;
    private String numero;
    private String estado;

    public String nombreCompleto() {
        return nombres + " " + primerApellido;
    }
}
