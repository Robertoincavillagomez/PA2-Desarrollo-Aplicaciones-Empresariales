
package com.isil.edu.pe.spedidos.client;

import com.isil.edu.pe.spedidos.client.dto.ApiError;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockRequest;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente HTTP para devolver stock al servicio de inventario.
 */
@Component
public class DevolucionStockRestClient implements DevolucionStockClient {

    private final RestClient restClient;
    private final String baseUrl;

    public DevolucionStockRestClient(
            @Qualifier("inventarioHttpClient") RestClient restClient,
            @Value("${inventario.api.base-url}") String baseUrl
    ) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public DevolucionStockResponse devolverStock(String codigo, int cantidad) {

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "El código del producto es obligatorio."
            );
        }

        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad a devolver debe ser mayor que cero."
            );
        }

        DevolucionStockRequest request =
                new DevolucionStockRequest(cantidad);

        try {
            DevolucionStockResponse response = restClient
                    .post()
                    .uri(
                            baseUrl + "/productos/codigo/{codigo}/devoluciones",
                            codigo
                    )
                    .body(request)
                    .retrieve()
                    .body(DevolucionStockResponse.class);

            if (response == null
                    || response.getCodigo() == null
                    || !codigo.equals(response.getCodigo())
                    || response.getCantidadDevuelta() != cantidad) {
                throw new InventarioClientException(
                        502,
                        "El inventario devolvió una respuesta inválida."
                );
            }

            return response;

        } catch (RestClientResponseException e) {
            throw convertirError(e);

        } catch (ResourceAccessException e) {
            throw new InventarioNoDisponibleException(
                    "El servicio de inventario no está disponible "
                            + "o excedió el tiempo de espera.",
                    e
            );
        }
    }

    private InventarioClientException convertirError(
            RestClientResponseException e
    ) {
        String mensaje = "inventario-service respondió HTTP "
                + e.getStatusCode().value() + ".";

        try {
            ApiError error = e.getResponseBodyAs(ApiError.class);

            if (error != null
                    && error.getMessage() != null
                    && !error.getMessage().isBlank()) {
                mensaje = error.getMessage();
            }

        } catch (RuntimeException ignored) {
            // Conserva el mensaje genérico si el JSON no es válido.
        }

        return new InventarioClientException(
                e.getStatusCode().value(),
                mensaje
        );
    }
}
