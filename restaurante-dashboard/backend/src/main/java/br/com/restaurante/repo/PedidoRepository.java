package br.com.restaurante.repo;

import br.com.restaurante.domain.Canal;
import br.com.restaurante.domain.Pedido;
import br.com.restaurante.domain.StatusPedido;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByCriadoEmGreaterThanEqualAndCriadoEmLessThanOrderByCriadoEmDesc(LocalDateTime de, LocalDateTime ate);

    List<Pedido> findByStatusInOrderByCriadoEmDesc(Collection<StatusPedido> status);

    List<Pedido> findByCanalAndNumeroMesaAndStatusIn(Canal canal, Integer mesa, Collection<StatusPedido> status);

    long countByCanalAndStatusIn(Canal canal, Collection<StatusPedido> status);
}
