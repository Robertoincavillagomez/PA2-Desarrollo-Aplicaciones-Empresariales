
package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioClient;
import com.isil.edu.pe.spedidos.client.InventarioClientException;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockResponse;
import com.isil.edu.pe.spedidos.client.dto.ReservaStockResponse;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.PedidoException;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PedidoServiceImplTest {

    private PedidoRepository pedidoRepository;
    private InventarioClient inventarioClient;
    private DevolucionStockClient devolucionStockClient;
    private PedidoServiceImpl pedidoService;

    @BeforeEach
    void setUp() {
        pedidoRepository = mock(PedidoRepository.class);
        inventarioClient = mock(InventarioClient.class);
        devolucionStockClient = mock(DevolucionStockClient.class);

        pedidoService = new PedidoServiceImpl(
                pedidoRepository,
                inventarioClient,
                devolucionStockClient
        );
    }

    @Test
    void debeRegistrarPedidoYCalcularTotal() {
        ReservaStockResponse reserva = nuevaReserva(
                "LAP-001",
                "Laptop",
                new BigDecimal("2500.00")
        );

        when(inventarioClient.reservarStock("LAP-001", 2))
                .thenReturn(reserva);

        when(pedidoRepository.saveAndFlush(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Pedido pedido = pedidoService.registrarPedido(
                "Ana Torres",
                "LAP-001",
                2
        );

        assertEquals("Ana Torres", pedido.getCliente());
        assertEquals("LAP-001", pedido.getProductoCodigo());
        assertEquals(new BigDecimal("2500.00"), pedido.getPrecioUnitario());
        assertEquals(new BigDecimal("5000.00"), pedido.getTotal());

        verify(inventarioClient).reservarStock("LAP-001", 2);
        verify(pedidoRepository).saveAndFlush(any(Pedido.class));
        verifyNoInteractions(devolucionStockClient);
    }

    @Test
    void debeRechazarCantidadMenorOIgualACero() {
        PedidoException error = assertThrows(
                PedidoException.class,
                () -> pedidoService.registrarPedido(
                        "Ana Torres",
                        "LAP-001",
                        0
                )
        );

        assertEquals(
                "La cantidad debe ser mayor que cero.",
                error.getMessage()
        );

        verifyNoInteractions(
                inventarioClient,
                pedidoRepository,
                devolucionStockClient
        );
    }

    @Test
    void debeConservarCausaDelErrorFuncionalDeInventario() {
        InventarioClientException causa =
                new InventarioClientException(
                        409,
                        "Stock insuficiente."
                );

        when(inventarioClient.reservarStock("LAP-001", 5))
                .thenThrow(causa);

        PedidoException error = assertThrows(
                PedidoException.class,
                () -> pedidoService.registrarPedido(
                        "Ana Torres",
                        "LAP-001",
                        5
                )
        );

        assertEquals("Stock insuficiente.", error.getMessage());
        assertSame(causa, error.getCause());

        verifyNoInteractions(
                pedidoRepository,
                devolucionStockClient
        );
    }

    @Test
    void debeTratarError5xxComoServicioNoDisponible() {
        InventarioClientException causa =
                new InventarioClientException(
                        500,
                        "Error interno de inventario."
                );

        when(inventarioClient.reservarStock("LAP-001", 1))
                .thenThrow(causa);

        InventarioNoDisponibleException error = assertThrows(
                InventarioNoDisponibleException.class,
                () -> pedidoService.registrarPedido(
                        "Ana Torres",
                        "LAP-001",
                        1
                )
        );

        assertSame(causa, error.getCause());

        verifyNoInteractions(
                pedidoRepository,
                devolucionStockClient
        );
    }

    @Test
    void debeCompensarCuandoFallaElGuardadoLocal() {
        ReservaStockResponse reserva = nuevaReserva(
                "LAP-001",
                "Laptop",
                new BigDecimal("2500.00")
        );

        when(inventarioClient.reservarStock("LAP-001", 2))
                .thenReturn(reserva);

        RuntimeException errorBaseDatos =
                new RuntimeException("Error simulado de H2");

        when(pedidoRepository.saveAndFlush(any(Pedido.class)))
                .thenThrow(errorBaseDatos);

        DevolucionStockResponse devolucion =
                mock(DevolucionStockResponse.class);

        when(devolucion.getCodigo()).thenReturn("LAP-001");
        when(devolucion.getCantidadDevuelta()).thenReturn(2);

        when(devolucionStockClient.devolverStock("LAP-001", 2))
                .thenReturn(devolucion);

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> pedidoService.registrarPedido(
                        "Ana Torres",
                        "LAP-001",
                        2
                )
        );

        assertSame(errorBaseDatos, error);

        verify(inventarioClient, times(1))
                .reservarStock("LAP-001", 2);

        verify(pedidoRepository, times(1))
                .saveAndFlush(any(Pedido.class));

        verify(devolucionStockClient, times(1))
                .devolverStock("LAP-001", 2);
    }

    @Test
    void debeRegistrarErrorSiLaCompensacionFalla() {
        ReservaStockResponse reserva = nuevaReserva(
                "LAP-001",
                "Laptop",
                new BigDecimal("2500.00")
        );

        when(inventarioClient.reservarStock("LAP-001", 2))
                .thenReturn(reserva);

        RuntimeException errorBaseDatos =
                new RuntimeException("Error simulado de H2");

        when(pedidoRepository.saveAndFlush(any(Pedido.class)))
                .thenThrow(errorBaseDatos);

        RuntimeException errorDevolucion =
                new RuntimeException("Inventario no disponible");

        when(devolucionStockClient.devolverStock("LAP-001", 2))
                .thenThrow(errorDevolucion);

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> pedidoService.registrarPedido(
                        "Ana Torres",
                        "LAP-001",
                        2
                )
        );

        assertSame(errorBaseDatos, error);
        assertEquals(1, error.getSuppressed().length);
        assertSame(errorDevolucion, error.getSuppressed()[0]);

        verify(devolucionStockClient, times(1))
                .devolverStock("LAP-001", 2);
    }

    private ReservaStockResponse nuevaReserva(
            String codigo,
            String nombre,
            BigDecimal precio
    ) {
        ReservaStockResponse reserva = new ReservaStockResponse();
        reserva.setCodigo(codigo);
        reserva.setNombre(nombre);
        reserva.setPrecio(precio);
        return reserva;
    }
}
