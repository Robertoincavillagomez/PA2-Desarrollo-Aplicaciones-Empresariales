
package com.isil.edu.pe.spedidos.client;

import com.isil.edu.pe.spedidos.client.dto.DevolucionStockResponse;

/**
 * Contrato para solicitar la devolución de stock al inventario.
 */
public interface DevolucionStockClient {

    DevolucionStockResponse devolverStock(String codigo, int cantidad);
}

