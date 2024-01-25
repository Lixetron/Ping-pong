package top.lixetron.server.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.lixetron.server.dto.ConnectRequest;
import top.lixetron.server.dto.GameObjectState;
import top.lixetron.server.enums.GameObjectType;
import top.lixetron.server.exception.GameException;
import top.lixetron.server.listeners.GameEventListener;
import top.lixetron.server.model.Game;
import top.lixetron.server.model.Player;
import top.lixetron.server.service.GameService;

import java.util.Objects;

@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("/game")
public class GameController {
    private final GameService gameService;

    @PostMapping("/create")
    public ResponseEntity<Game> create(@RequestBody Player player) {
        log.info("create game request: {}", player);
        return ResponseEntity.ok(gameService.createGame(player));
    }

    @PostMapping("/connect")
    public ResponseEntity<Game> connect(@RequestBody ConnectRequest request) throws GameException {
        log.info("connect request: {}", request);
        return ResponseEntity.ok(gameService.connectToGame(request.getPlayer(), request.getGameUid()));
    }

    @PostMapping("/connect/random")
    public ResponseEntity<Game> connectRandom(@RequestBody Player player) throws GameException {
        log.info("connect random {}", player);
        return ResponseEntity.ok(gameService.connectToRandomGame(player));
    }


    @MessageMapping("/game-process/{uid}")
    @SendTo("/topic/game/{uid}")
    public GameObjectState play(@DestinationVariable String uid, @Payload Message<Point> message) {
        String sessionId = GameEventListener.getWebsocketSessionId(StompHeaderAccessor.wrap(message));

        Game currentGame = GameEventListener.games.get(uid);

        String playerNameBySessionId = GameEventListener.playersSessionsDictionary.get(sessionId);
        currentGame.getPlayersPosition().replace(playerNameBySessionId, message.getPayload());

        GameObjectState gameObjectState = new GameObjectState();
        gameObjectState.setType(GameObjectType.PLAYER);
        gameObjectState.setPosition(message.getPayload());
        gameObjectState.setClientId(playerNameBySessionId);

        return gameObjectState;
    }

}
