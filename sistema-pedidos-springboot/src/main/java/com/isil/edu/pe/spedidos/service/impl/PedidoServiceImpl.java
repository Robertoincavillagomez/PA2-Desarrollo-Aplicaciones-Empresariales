
package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioClient;
import com.isil.edu.pe.spedidos.client.InventarioClientException;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockResponse;
import com.isil.edu.pe.spedidos.client.dto.ProductoInventarioResponse;
import com.isil.edu.pe.spedidos.client.dto.ReservaStockResponse;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.PedidoException;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import com.isil.edu.pe.spedidos.service.PedidoService;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoServiceImpl implements PedidoService {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoServiceImpl.class);

    private final PedidoRepository pedidoRepository;
    private final InventarioClient inventarioClient;
    private final DevolucionStockClient devolucionStockClient;

    public PedidoServiceImpl(
            PedidoRepository pedidoRepository,
            InventarioClient inventarioClient,
            DevolucionStockClient devolucionStockClient
    ) {
        this.pedidoRepository = pedidoRepository;
        this.inventarioClient = inventarioClient;
        this.devolucionStockClient = devolucionStockClient;
    }

    @Override
    @Transactional
    public Pedido registrarPedido(
            String cliente,
            String productoCodigo,
            int cantidad
    ) {
        validarDatos(cliente, productoCodigo, cantidad);

        String codigo = productoCodigo.trim();

        final ReservaStockResponse reserva;

        try {
            reserva = inventarioClient.reservarStock(codigo, cantidad);
        } catch (InventarioClientException e) {

            if (e.getStatus() >= 500) {
                throw new InventarioNoDisponibleException(
                        "El servicio de inventario presentó un error temporal (HTTP "
                                + e.getStatus() + ").",
                        e
                );
            }

            throw new PedidoException(e.getMessage(), e);
        }

        /*
         * A partir de aquí, el inventario ya confirmó la reserva.
         * Si ocurre un error local, se intentará devolver el stock.
         */
        try {
            validarReserva(reserva);

            BigDecimal total = reserva.getPrecio()
                    .multiply(BigDecimal.valueOf(cantidad));

            Pedido pedido = new Pedido(
                    cliente.trim(),
                    reserva.getCodigo(),
                    reserva.getNombre(),
                    reserva.getPrecio(),
                    cantidad,
                    total
            );

            /*
             * saveAndFlush fuerza el envío de las operaciones pendientes
             * a H2 y permite detectar errores de escritura en este punto.
             */
            return pedidoRepository.saveAndFlush(pedido);

        } catch (RuntimeException errorLocal) {

            log.error(
                    "Falló el registro local del pedido. "
                            + "Se intentará compensar la reserva. "
                            + "Producto: {}, cantidad: {}",
                    codigo,
                    cantidad,
                    errorLocal
            );

            compensarReserva(codigo, cantidad, errorLocal);

            /*
             * Se conserva el error original para que Spring
             * revierta la transacción local.
             */
            throw errorLocal;
        }
    }

    private void compensarReserva(
            String codigo,
            int cantidad,
            RuntimeException errorOriginal
    ) {
        try {
            log.warn(
                    "COMPENSACION INICIADA: devolviendo {} unidades "
                            + "del producto {}.",
                    cantidad,
                    codigo
            );

            DevolucionStockResponse respuesta =
                    devolucionStockClient.devolverStock(codigo, cantidad);

            if (respuesta == null
                    || respuesta.getCodigo() == null
                    || !codigo.equals(respuesta.getCodigo())
                    || respuesta.getCantidadDevuelta() != cantidad) {

                throw new IllegalStateException(
                        "El inventario no confirmó correctamente "
                                + "la compensación."
                );
            }

            log.info(
                    "COMPENSACION EXITOSA: se devolvieron {} unidades "
                            + "del producto {}. Stock restante: {}",
                    cantidad,
                    codigo,
                    respuesta.getStockRestante()
            );

        } catch (RuntimeException errorCompensacion) {

            log.error(
                    "COMPENSACION FALLIDA: no se pudo confirmar "
                            + "la devolución de {} unidades del producto {}. "
                            + "Se requiere revisión manual.",
                    cantidad,
                    codigo,
                    errorCompensacion
            );

            errorOriginal.addSuppressed(errorCompensacion);
        }
    }

    @Override
    public List<ProductoInventarioResponse> listarProductos() {
        return inventarioClient.listarProductos();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAllByOrderByIdDesc();
    }

    private void validarDatos(
            String cliente,
            String productoCodigo,
            int cantidad
    ) {
        if (cliente == null || cliente.isBlank()) {
            throw new PedidoException(
                    "El cliente es obligatorio."
            );
        }

        if (productoCodigo == null || productoCodigo.isBlank()) {
            throw new PedidoException(
                    "Debe seleccionar un producto."
            );
        }

        if (cantidad <= 0) {
            throw new PedidoException(
                    "La cantidad debe ser mayor que cero."
            );
        }
    }

    private void validarReserva(ReservaStockResponse reserva) {
        if (reserva == null
                || reserva.getCodigo() == null
                || reserva.getCodigo().isBlank()
                || reserva.getNombre() == null
                || reserva.getNombre().isBlank()
                || reserva.getPrecio() == null) {

            throw new PedidoException(
                    "inventario-service devolvió "
                            + "una respuesta incompleta."
            );
        }
    }
}
