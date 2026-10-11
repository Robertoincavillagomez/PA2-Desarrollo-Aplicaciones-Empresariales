
package com.isil.edu.pe.spedidos.client.dto;

/**
 * Respuesta del servicio de inventario después
 * de realizar una devolución de stock.
 */
public class DevolucionStockResponse {

    private String codigo;
    private int cantidadDevuelta;
    private int stockRestante;

    public DevolucionStockResponse() {
    }

    public DevolucionStockResponse(
            String codigo,
            int cantidadDevuelta,
            int stockRestante
    ) {
        this.codigo = codigo;
        this.cantidadDevuelta = cantidadDevuelta;
        this.stockRestante = stockRestante;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public int getCantidadDevuelta() {
        return cantidadDevuelta;
    }

    public void setCantidadDevuelta(int cantidadDevuelta) {
        this.cantidadDevuelta = cantidadDevuelta;
    }

    public int getStockRestante() {
        return stockRestante;
    }

    public void setStockRestante(int stockRestante) {
        this.stockRestante = stockRestante;
    }
}
