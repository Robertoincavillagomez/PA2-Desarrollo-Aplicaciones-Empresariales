
package com.isil.edu.pe.spedidos.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockResponse;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.entity.PedidoEstado;
import com.isil.edu.pe.spedidos.repository.AnulacionPedidoRepository;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnulacionPedidoServiceImplTest {

    private PedidoRepository pedidoRepository;
    private AnulacionPedidoRepository anulacionRepository;
    private DevolucionStockClient devolucionStockClient;
    private AnulacionPedidoServiceImpl anulacionService;

    @BeforeEach
    void setUp() {
        pedidoRepository = mock(PedidoRepository.class);
        anulacionRepository = mock(AnulacionPedidoRepository.class);
        devolucionStockClient = mock(DevolucionStockClient.class);

        anulacionService = new AnulacionPedidoServiceImpl(
                pedidoRepository,
                anulacionRepository,
                devolucionStockClient
        );
    }

    @Test
    void debeAnularPedidoYDevolverStockUnaSolaVez() {
        Pedido pedido = nuevoPedido();

        when(pedidoRepository.buscarPorIdParaAnular(1L))
                .thenReturn(Optional.of(pedido));

        when(devolucionStockClient.devolverStock("LAP-001", 2))
                .thenReturn(new DevolucionStockResponse(
                        "LAP-001", 2, 10
                ));

        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(anulacionRepository.save(any(AnulacionPedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnulacionPedido resultado = anulacionService.anularPedido(
                1L, "Solicitud del cliente"
        );

        assertEquals(PedidoEstado.ANULADO, pedido.getEstado());
        assertEquals(1L, resultado.getPedidoId());
        assertEquals("LAP-001", resultado.getProductoCodigo());
        assertEquals(2, resultado.getCantidadDevuelta());

        verify(devolucionStockClient).devolverStock("LAP-001", 2);
        verify(pedidoRepository).save(pedido);
        verify(anulacionRepository).save(any(AnulacionPedido.class));
    }

    @Test
    void debeRechazarPedidoYaAnuladoSinDevolverStock() {
        Pedido pedido = nuevoPedido();
        pedido.setEstado(PedidoEstado.ANULADO);

        when(pedidoRepository.buscarPorIdParaAnular(1L))
                .thenReturn(Optional.of(pedido));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> anulacionService.anularPedido(1L, "Duplicado")
        );

        assertEquals("El pedido ya fue anulado.", error.getMessage());

        verifyNoInteractions(devolucionStockClient, anulacionRepository);
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void debeRechazarPedidoInexistenteSinDevolverStock() {
        when(pedidoRepository.buscarPorIdParaAnular(99L))
                .thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> anulacionService.anularPedido(99L, "No existe")
        );

        assertEquals(
                "No existe el pedido con ID 99.",
                error.getMessage()
        );

        verifyNoInteractions(devolucionStockClient, anulacionRepository);
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void debeEvitarGuardadoSiInventarioNoEstaDisponible() {
        Pedido pedido = nuevoPedido();

        when(pedidoRepository.buscarPorIdParaAnular(1L))
                .thenReturn(Optional.of(pedido));

        InventarioNoDisponibleException causa =
                new InventarioNoDisponibleException(
                        "Inventario no disponible.",
                        new RuntimeException("Conexión rechazada")
                );

        when(devolucionStockClient.devolverStock("LAP-001", 2))
                .thenThrow(causa);

        InventarioNoDisponibleException error = assertThrows(
                InventarioNoDisponibleException.class,
                () -> anulacionService.anularPedido(
                        1L, "Solicitud del cliente"
                )
        );

        assertSame(causa, error);
        assertEquals(PedidoEstado.REGISTRADO, pedido.getEstado());

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(anulacionRepository, never()).save(any(AnulacionPedido.class));
    }

    private Pedido nuevoPedido() {
        Pedido pedido = mock(Pedido.class);

        // El mock representa un pedido registrado en H2.
        // Usamos un estado mutable para comprobar la anulación.
        final PedidoEstado[] estado = {PedidoEstado.REGISTRADO};

        when(pedido.getId()).thenReturn(1L);
        when(pedido.getProductoCodigo()).thenReturn("LAP-001");
        when(pedido.getCantidad()).thenReturn(2);
        when(pedido.getEstado()).thenAnswer(invocation -> estado[0]);

        org.mockito.Mockito.doAnswer(invocation -> {
            estado[0] = PedidoEstado.ANULADO;
            return null;
        }).when(pedido).anular();

        org.mockito.Mockito.doAnswer(invocation -> {
            estado[0] = invocation.getArgument(0);
            return null;
        }).when(pedido).setEstado(any(PedidoEstado.class));

        return pedido;
    }
}
