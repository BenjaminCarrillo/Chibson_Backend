package chibson.ventas.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import chibson.ventas.dto.CheckoutDTO;
import chibson.ventas.dto.CompraRespuestaDTO;
import chibson.ventas.dto.EstadoDTO;
import chibson.ventas.dto.MotivoDTO;
import chibson.ventas.dto.PagoDTO;
import chibson.ventas.dto.PedidoDTO;
import chibson.ventas.dto.PedidoResumenDTO;
import chibson.ventas.model.EstadoPedido;
import chibson.ventas.service.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private static final String RUT = "X-Rut-Usuario";

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // ---------- Cliente ----------

    /** HU-23, HU-24, HU-25: compra. 201 con comprobante si se aprueba, 402 si el pago se rechaza. */
    @PostMapping("/checkout")
    public ResponseEntity<CompraRespuestaDTO> comprar(@RequestHeader(value = RUT, required = false) String rut,
                                                      @RequestBody CheckoutDTO datos) {
        return responder(pedidoService.comprar(rut, datos));
    }

    /** HU-23 criterio 4: reintentar el pago de un pedido pendiente. */
    @PostMapping("/{numero}/pagar")
    public ResponseEntity<CompraRespuestaDTO> reintentarPago(@RequestHeader(value = RUT, required = false) String rut,
                                                             @PathVariable Long numero, @RequestBody PagoDTO datos) {
        return responder(pedidoService.reintentarPago(numero, rut, datos.getMetodoPago(), datos.isSimularRechazo()));
    }

    /** HU-33 */
    @GetMapping("/mis-pedidos")
    public List<PedidoDTO> misPedidos(@RequestHeader(value = RUT, required = false) String rut) {
        return pedidoService.listarPorCliente(rut);
    }

    /** HU-33 y HU-36 */
    @GetMapping("/mis-pedidos/{numero}")
    public PedidoDTO miPedido(@RequestHeader(value = RUT, required = false) String rut, @PathVariable Long numero) {
        return pedidoService.detalleCliente(numero, rut);
    }

    // ---------- Administración: permiso GESTIONAR_PEDIDOS ----------

    /** HU-34 */
    @GetMapping
    public List<PedidoDTO> listar(@RequestHeader(value = RUT, required = false) String rut,
                                  @RequestParam(required = false) EstadoPedido estado) {
        return pedidoService.listar(rut, estado);
    }

    /** HU-34 y HU-36 */
    @GetMapping("/{numero}")
    public PedidoDTO detalle(@RequestHeader(value = RUT, required = false) String rut, @PathVariable Long numero) {
        return pedidoService.detalleAdmin(numero, rut);
    }

    /** HU-35 y HU-38 */
    @PatchMapping("/{numero}/estado")
    public PedidoDTO actualizarEstado(@RequestHeader(value = RUT, required = false) String rut,
                                      @PathVariable Long numero, @RequestBody EstadoDTO datos) {
        return pedidoService.actualizarEstado(numero, datos.getEstado(), rut);
    }

    /** HU-37 */
    @PatchMapping("/{numero}/cancelar")
    public PedidoDTO cancelar(@RequestHeader(value = RUT, required = false) String rut,
                              @PathVariable Long numero, @RequestBody MotivoDTO datos) {
        return pedidoService.cancelar(numero, datos.getMotivo(), rut);
    }

    /** HU-44 */
    @DeleteMapping("/{numero}")
    public ResponseEntity<Void> eliminar(@RequestHeader(value = RUT, required = false) String rut,
                                         @PathVariable Long numero) {
        pedidoService.eliminar(numero, rut);
        return ResponseEntity.noContent().build();
    }

    // ---------- Para otros microservicios ----------

    /** ms-usuarios (HU-17) */
    @GetMapping("/cliente/{rut}/existe")
    public boolean clienteTienePedidos(@PathVariable String rut) {
        return pedidoService.clienteTienePedidos(rut);
    }

    /** ms-productos (HU-04) */
    @GetMapping("/producto/{sku}/existe")
    public boolean productoTienePedidos(@PathVariable String sku) {
        return pedidoService.productoTienePedidos(sku);
    }

    /** ms-productos (HU-81) */
    @GetMapping("/cliente/{rut}/recibio/{sku}")
    public boolean clienteRecibioProducto(@PathVariable String rut, @PathVariable String sku) {
        return pedidoService.clienteRecibioProducto(rut, sku);
    }

    /** ms-informes (HU-45 a HU-55). Fechas AAAA-MM-DD, opcionales. */
    @GetMapping("/resumen")
    public List<PedidoResumenDTO> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return pedidoService.resumen(desde, hasta);
    }

    private ResponseEntity<CompraRespuestaDTO> responder(CompraRespuestaDTO respuesta) {
        HttpStatus estado = respuesta.isAprobado() ? HttpStatus.CREATED : HttpStatus.PAYMENT_REQUIRED;
        return ResponseEntity.status(estado).body(respuesta);
    }
}
