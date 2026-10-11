
package com.isil.edu.pe.spedidos.repository;

import com.isil.edu.pe.spedidos.entity.Pedido;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de acceso a datos para la entidad Pedido.
 *
 * Permite registrar, consultar y listar pedidos mediante
 * Spring Data JPA y Hibernate.
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Obtiene todos los pedidos ordenados por identificador descendente.
     */
    List<Pedido> findAllByOrderByIdDesc();

    /**
     * Busca un pedido y solicita un bloqueo de escritura
     * mientras se procesa su anulación.
     *
     * El bloqueo se mantiene durante la transacción activa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :pedidoId")
    Optional<Pedido> buscarPorIdParaAnular(
            @Param("pedidoId") Long pedidoId
    );
}

