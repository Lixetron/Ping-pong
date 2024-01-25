package top.lixetron.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import top.lixetron.server.exception.GameException;
import top.lixetron.server.listeners.GameEventListener;
import top.lixetron.server.model.Game;
import top.lixetron.server.model.GameStatusEnum;
import top.lixetron.server.model.Player;
import top.lixetron.server.repository.GameRepository;
import top.lixetron.server.repository.PlayerRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;

    public Game createGame(Player player) {
        Optional<Player> optionalPlayer = playerRepository.findFirstByName(player.getName());
        if (optionalPlayer.isEmpty()) {
            player = playerRepository.save(player);
        } else {
            player = optionalPlayer.get();
        }
        Game game = new Game();
        game.setUid(UUID.randomUUID().toString());
        game.setFirstPlayer(player);
        game.setStatus(GameStatusEnum.NEW);
        return gameRepository.save(game);
    }

    public Game connectToGame(Player player, String gameUid) {
        Optional<Player> optionalPlayer = playerRepository.findFirstByName(player.getName());
        if (optionalPlayer.isEmpty()) {
            player = playerRepository.save(player);
        } else {
            player = optionalPlayer.get();
        }

        Optional<Game> foundGame = gameRepository.findByUidAndFirstPlayerNot(gameUid, player);

        foundGame.orElseThrow(() -> new GameException("Game with provided id doesn't exist"));
        Game game = foundGame.get();

        if (game.getSecondPlayer() != null) {
            throw new GameException("Game is not valid anymore");
        }

        game.setSecondPlayer(player);
        game.setStatus(GameStatusEnum.IN_PROGRESS);
        return gameRepository.save(game);
    }

    public Game connectToRandomGame(Player player) {
        Optional<Player> optionalPlayer = playerRepository.findFirstByName(player.getName());
        if (optionalPlayer.isEmpty()) {
            player = playerRepository.save(player);
        } else {
            player = optionalPlayer.get();
        }

        Optional<Game> anyGame = gameRepository.findFirstByStatusAndSecondPlayerIsNullAndFirstPlayerNot(GameStatusEnum.NEW, player);

        anyGame.orElseThrow(() -> new GameException("There is no available Game!"));
        Game game = anyGame.get();

        game.setSecondPlayer(player);
        game.setStatus(GameStatusEnum.IN_PROGRESS);
        return gameRepository.save(game);
    }

    public Game update(Game game) {
        return gameRepository.save(game);
    }

    public Optional<Game> findByUid(String uid) {
        return gameRepository.findByUid(uid);
    }
}
