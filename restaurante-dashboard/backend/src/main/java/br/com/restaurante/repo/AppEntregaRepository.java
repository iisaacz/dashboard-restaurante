package br.com.restaurante.repo;

import br.com.restaurante.domain.AppEntrega;
import br.com.restaurante.domain.Canal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppEntregaRepository extends JpaRepository<AppEntrega, Long> {
    Optional<AppEntrega> findByCanal(Canal canal);
}
