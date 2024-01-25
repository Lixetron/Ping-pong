package top.lixetron.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.lixetron.server.model.Game;
import top.lixetron.server.model.GameStatusEnum;
import top.lixetron.server.model.Player;

import java.util.Optional;


public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findFirstByStatusAndSecondPlayerIsNullAndFirstPlayerNot(GameStatusEnum status, Player player);
    Optional<Game> findByUid(String uid);
    Optional<Game> findByUidAndFirstPlayerNot(String uid, Player player);
}
