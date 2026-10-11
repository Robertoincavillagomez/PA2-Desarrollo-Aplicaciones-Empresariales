
package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioClientException;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockResponse;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.entity.PedidoEstado;
import com.isil.edu.pe.spedidos.repository.AnulacionPedidoRepository;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import com.isil.edu.pe.spedidos.service.AnulacionPedidoService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnulacionPedidoServiceImpl implements AnulacionPedidoService {

    private final PedidoRepository pedidoRepository;
    private final AnulacionPedidoRepository anulacionRepository;
    private final DevolucionStockClient devolucionStockClient;

    public AnulacionPedidoServiceImpl(
            PedidoRepository pedidoRepository,
            AnulacionPedidoRepository anulacionRepository,
            DevolucionStockClient devolucionStockClient
    ) {
        this.pedidoRepository = pedidoRepository;
        this.anulacionRepository = anulacionRepository;
        this.devolucionStockClient = devolucionStockClient;
    }

    @Override
    @Transactional
    public AnulacionPedido anularPedido(Long pedidoId, String motivo) {

        if (pedidoId == null || pedidoId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del pedido debe ser válido."
            );
        }

        // Bloquea el pedido durante la transacción.
        Pedido pedido = pedidoRepository.buscarPorIdParaAnular(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el pedido con ID " + pedidoId + "."
                ));

        // Evita devolver el stock de un pedido ya anulado.
        if (pedido.getEstado() == PedidoEstado.ANULADO
                || anulacionRepository.existsByPedidoId(pedidoId)) {
            throw new IllegalStateException(
                    "El pedido ya fue anulado."
            );
        }

        DevolucionStockResponse respuesta;

        try {
            // Solicita la devolución al inventario remoto.
            respuesta = devolucionStockClient.devolverStock(
                    pedido.getProductoCodigo(),
                    pedido.getCantidad()
            );

        } catch (InventarioClientException e) {

            if (e.getStatus() >= 500) {
                throw new InventarioNoDisponibleException(
                        "El servicio de inventario presentó un error temporal.",
                        e
                );
            }

            throw new IllegalStateException(
                    "No se pudo devolver el stock: " + e.getMessage(),
                    e
            );
        }

        // Verifica que inventario confirmó la cantidad correcta.
        if (respuesta == null
                || respuesta.getCantidadDevuelta() != pedido.getCantidad()
                || !pedido.getProductoCodigo().equals(respuesta.getCodigo())) {

            throw new IllegalStateException(
                    "El inventario no confirmó correctamente la devolución."
            );
        }

        // Actualiza el estado sin eliminar el pedido.
        pedido.anular();
        pedidoRepository.save(pedido);

        // Registra la auditoría de la anulación.
        AnulacionPedido anulacion = new AnulacionPedido(
                pedido.getId(),
                pedido.getProductoCodigo(),
                pedido.getCantidad(),
                motivo
        );

        return anulacionRepository.save(anulacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnulacionPedido> listarAnulaciones() {
        return anulacionRepository.findAll();
    }
}
