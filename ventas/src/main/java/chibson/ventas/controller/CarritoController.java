package chibson.ventas.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import chibson.ventas.dto.CarritoDTO;
import chibson.ventas.dto.ItemDTO;
import chibson.ventas.service.CarritoService;

/** Carro de compras del cliente con sesión (HU-20 a HU-22). */
@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private static final String RUT = "X-Rut-Usuario";

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    /** Carro con subtotales, total y contador de unidades (HU-20 a HU-22). */
    @GetMapping
    public CarritoDTO verCarrito(@RequestHeader(value = RUT, required = false) String rut) {
        return CarritoDTO.desde(carritoService.verCarrito(rut));
    }

    /** HU-20 */
    @PostMapping("/items")
    public CarritoDTO agregarProducto(@RequestHeader(value = RUT, required = false) String rut,
                                      @RequestBody ItemDTO datos) {
        return CarritoDTO.desde(carritoService.agregarProducto(rut, datos));
    }

    /** HU-21: en el cuerpo solo se usa "cantidad". */
    @PutMapping("/items/{idItem}")
    public CarritoDTO modificarCantidad(@RequestHeader(value = RUT, required = false) String rut,
                                        @PathVariable Long idItem, @RequestBody ItemDTO datos) {
        return CarritoDTO.desde(carritoService.modificarCantidad(rut, idItem, datos.getCantidad()));
    }

    /** HU-22 */
    @DeleteMapping("/items/{idItem}")
    public CarritoDTO eliminarProducto(@RequestHeader(value = RUT, required = false) String rut,
                                       @PathVariable Long idItem) {
        return CarritoDTO.desde(carritoService.eliminarProducto(rut, idItem));
    }
}
