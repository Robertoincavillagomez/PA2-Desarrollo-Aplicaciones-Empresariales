
package com.isil.edu.pe.spedidos.service;

import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import java.util.List;

/**
 * Define las operaciones para anular pedidos
 * y consultar el historial de anulaciones.
 */
public interface AnulacionPedidoService {

    AnulacionPedido anularPedido(Long pedidoId, String motivo);

    List<AnulacionPedido> listarAnulaciones();
}
