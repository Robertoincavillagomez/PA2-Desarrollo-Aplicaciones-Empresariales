
package com.isil.edu.pe.spedidos;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioClient;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockResponse;
import com.isil.edu.pe.spedidos.client.dto.ReservaStockResponse;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import com.isil.edu.pe.spedidos.service.PedidoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class CompensacionPedidoIntegrationTest {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @MockitoBean
    private InventarioClient inventarioClient;

    @MockitoBean
    private DevolucionStockClient devolucionStockClient;

    @Test
    void debeCompensarCuandoH2RechazaElPedido() {

        String clienteLargo = "A".repeat(130);

        ReservaStockResponse reserva =
                mock(ReservaStockResponse.class);

        when(reserva.getCodigo()).thenReturn("LAP-001");
        when(reserva.getNombre()).thenReturn("Laptop");
        when(reserva.getPrecio())
                .thenReturn(new BigDecimal("2500.00"));

        when(inventarioClient.reservarStock("LAP-001", 2))
                .thenReturn(reserva);

        DevolucionStockResponse devolucion =
                mock(DevolucionStockResponse.class);

        when(devolucion.getCodigo()).thenReturn("LAP-001");
        when(devolucion.getCantidadDevuelta()).thenReturn(2);

        when(devolucionStockClient.devolverStock("LAP-001", 2))
                .thenReturn(devolucion);

        long pedidosAntes = pedidoRepository.count();

        assertThrows(
                RuntimeException.class,
                () -> pedidoService.registrarPedido(
                        clienteLargo,
                        "LAP-001",
                        2
                )
        );

        verify(inventarioClient, times(1))
                .reservarStock("LAP-001", 2);

        verify(devolucionStockClient, times(1))
                .devolverStock("LAP-001", 2);

        assertEquals(
                pedidosAntes,
                pedidoRepository.count()
        );
    }
}
