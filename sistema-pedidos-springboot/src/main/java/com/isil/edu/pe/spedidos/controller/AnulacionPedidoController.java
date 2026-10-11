
package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.client.InventarioClientException;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.service.AnulacionPedidoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/anulaciones")
public class AnulacionPedidoController {

    private final AnulacionPedidoService anulacionPedidoService;

    public AnulacionPedidoController(
            AnulacionPedidoService anulacionPedidoService
    ) {
        this.anulacionPedidoService = anulacionPedidoService;
    }

    @PostMapping
    public ResponseEntity<AnulacionPedido> anularPedido(
            @RequestBody AnulacionRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Debe enviar los datos de la anulación."
            );
        }

        AnulacionPedido anulacion = anulacionPedidoService.anularPedido(
                request.pedidoId(),
                request.motivo()
        );

        return ResponseEntity.ok(anulacion);
    }

    @GetMapping
    public List<AnulacionPedido> listarAnulaciones() {
        return anulacionPedidoService.listarAnulaciones();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentoInvalido(
            IllegalArgumentException e
    ) {
        if (e.getMessage() != null
                && e.getMessage().startsWith("No existe el pedido")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> manejarConflicto(
            IllegalStateException e
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(InventarioNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejarInventarioNoDisponible(
            InventarioNoDisponibleException e
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(InventarioClientException.class)
    public ResponseEntity<ErrorResponse> manejarErrorInventario(
            InventarioClientException e
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(e.getMessage()));
    }

    public record AnulacionRequest(Long pedidoId, String motivo) {
    }

    public record ErrorResponse(String message) {
    }
}
