package chibson.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Texto de la ficha técnica, por ejemplo "cuerpo:caoba maciza;trastes:22;color:natural" (HU-05, HU-06). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FichaTecnicaDTO {
    private String texto;
}
