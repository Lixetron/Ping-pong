package top.lixetron.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.lixetron.server.model.Player;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findFirstByName(String name);
}
