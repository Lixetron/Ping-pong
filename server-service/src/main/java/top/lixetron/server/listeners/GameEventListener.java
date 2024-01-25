package top.lixetron.server.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.geo.Point;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import top.lixetron.server.dto.GameObjectState;
import top.lixetron.server.enums.BallAbscissaDirection;
import top.lixetron.server.enums.BallOrdinateDirection;
import top.lixetron.server.enums.GameObjectType;
import top.lixetron.server.model.Game;
import top.lixetron.server.model.GameStatusEnum;
import top.lixetron.server.model.Player;
import top.lixetron.server.service.GameScheduler;
import top.lixetron.server.service.GameService;
import top.lixetron.server.service.PlayerService;

import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameEventListener {

    public static Map<String, Game> games = new HashMap<>();
    public static Map<String, String> playersSessionsDictionary = new HashMap<>();
    private final GameService gameService;
    private final PlayerService playerService;
    private final GameScheduler gameScheduler;

    private final SimpMessagingTemplate messagingTemplate;

    //TODO: Add SimpUserRegistry

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        String playerName = playersSessionsDictionary.remove(event.getSessionId());
        Optional<String> gameKey = games.entrySet().stream()
                .filter(stringGameEntry -> playerName.equals(stringGameEntry.getValue().getFirstPlayer().getName())
                        || playerName.equals(stringGameEntry.getValue().getSecondPlayer().getName()))
                .map(Map.Entry::getKey)
                .findFirst();
        gameKey.ifPresent(gameUid -> {
            games.remove(gameUid);
            log.warn("Disconnected player: {} ", playerName);
            gameScheduler.cancelScheduledTask(gameUid);
        });
    }

    @EventListener
    private void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());

        String playerName = Objects.requireNonNull(headers.getNativeHeader("player")).get(0);
        Optional<Player> optionalPlayer = playerService.findByName(playerName);

        String gameUid = Objects.requireNonNull(headers.getDestination()).split("/")[3];
        Optional<Game> optionalGame = gameService.findByUid(gameUid);

        if (optionalGame.isPresent() && optionalPlayer.isPresent()) {

            Game game = optionalGame.get();
            Player player = optionalPlayer.get();

            if (game.getFirstPlayer().equals(player)) {
                playersSessionsDictionary.put(getWebsocketSessionId(StompHeaderAccessor.wrap(event.getMessage())), playerName);
                game.setPlayersPosition(new HashMap<>(Map.of(playerName, new Point(0, 0))));
                games.put(gameUid, game);
            } else if (game.getSecondPlayer().equals(player)) {
                playersSessionsDictionary.put(getWebsocketSessionId(StompHeaderAccessor.wrap(event.getMessage())), playerName);
                Game currentGame = games.get(gameUid);
                currentGame.setSecondPlayer(game.getSecondPlayer());
                currentGame.getPlayersPosition().put(playerName, new Point(100, 0));

                GameObjectState secondPlayerState = new GameObjectState();
                secondPlayerState.setType(GameObjectType.PLAYER);
                secondPlayerState.setClientId(playerName);
                secondPlayerState.setPosition(new Point(100, 0));
                messagingTemplate.convertAndSend("/topic/game/" + gameUid, secondPlayerState); //init position of second player

                GameObjectState scoreState = new GameObjectState();
                scoreState.setType(GameObjectType.SCORE);
                scoreState.setPosition(new Point(0, 0));
                scoreState.setClientId(currentGame.getFirstPlayer().getName());
                currentGame.setScore(scoreState.getPosition());
                messagingTemplate.convertAndSend("/topic/game/" + gameUid, scoreState);

                Random random = new Random();
                GameObjectState ball = new GameObjectState();
                ball.setType(GameObjectType.BALL);
                ball.setPosition(new Point(50, (double) random.nextInt(99) + 1));

                currentGame.setBallPosition(ball.getPosition());
                currentGame.setBallAbscissaDirection(BallAbscissaDirection.values()[random.nextInt(2)]);

                messagingTemplate.convertAndSend("/topic/game/" + gameUid, ball); //init position of ball

                Runnable ballPosition = () -> {
                    Point currentBallPosition = currentGame.getBallPosition();
                    BallAbscissaDirection currentBallAbscissaDirection = currentGame.getBallAbscissaDirection();
                    Point currentScore = currentGame.getScore();
                    boolean isScoreChanged = false;

                    switch (currentBallAbscissaDirection) {
                        case LEFT -> {
                            //reach left side
                            if (currentBallPosition.getX() <= 1) {
                                double firstPlayerY = currentGame.getPlayersPosition().get(currentGame.getFirstPlayer().getName()).getY();

                                //collision with first player
                                if (currentBallPosition.getY() >= firstPlayerY && currentBallPosition.getY() <= firstPlayerY + 20) {
                                    currentBallAbscissaDirection = BallAbscissaDirection.RIGHT;

                                    double difference = currentBallPosition.getY() - (firstPlayerY + 10);

                                    if (difference >= 0) {
                                        currentGame.setBallOrdinateDirection(BallOrdinateDirection.DOWN);
                                        currentGame.setBallOrdinateDirectionPower(difference * 0.12);
                                    } else {
                                        currentGame.setBallOrdinateDirection(BallOrdinateDirection.UP);
                                        currentGame.setBallOrdinateDirectionPower(difference * 0.12);
                                    }

                                    double x = currentBallPosition.getX() + 1;

                                    double y = currentBallPosition.getY();

                                    if (Objects.nonNull(currentGame.getBallOrdinateDirection())) {
                                        y += currentGame.getBallOrdinateDirectionPower();
                                    }

                                    ball.setPosition(new Point(x, y));
                                    currentGame.setBallAbscissaDirection(currentBallAbscissaDirection);
                                } else { //reset if not collision with first player
                                    ball.setPosition(new Point(50, (double) random.nextInt(99) + 1));
                                    currentGame.setBallAbscissaDirection(BallAbscissaDirection.values()[random.nextInt(2)]);
                                    currentGame.setBallOrdinateDirection(null);

                                    double firstPlayerScore = currentScore.getX();
                                    double secondPlayerScore = currentScore.getY();

                                    scoreState.setPosition(new Point(firstPlayerScore, secondPlayerScore + 1));
                                    currentGame.setScore(scoreState.getPosition());
                                    isScoreChanged = true;
                                }
                            } else { //not reach left side
                                double y = currentBallPosition.getY();

                                if (Objects.nonNull(currentGame.getBallOrdinateDirection())) {
                                    if (y >= 99) {
                                        currentGame.setBallOrdinateDirection(BallOrdinateDirection.UP);
                                        currentGame.setBallOrdinateDirectionPower(currentGame.getBallOrdinateDirectionPower() * -1);
                                    } else if (y <= 1) {
                                        currentGame.setBallOrdinateDirection(BallOrdinateDirection.DOWN);
                                        currentGame.setBallOrdinateDirectionPower(currentGame.getBallOrdinateDirectionPower() * -1);
                                    }

                                    y += currentGame.getBallOrdinateDirectionPower();
                                }

                                double x = currentBallPosition.getX() - 1;

                                if (Objects.nonNull(currentGame.getBallOrdinateDirection())) {
                                    y += currentGame.getBallOrdinateDirectionPower();
                                }

                                ball.setPosition(new Point(x, y));
                            }
                        }
                        case RIGHT -> {
                            //reach right side
                            if (currentBallPosition.getX() >= 99) {
                                double secondPlayerY = currentGame.getPlayersPosition().get(currentGame.getSecondPlayer().getName()).getY();

                                //collision with second player
                                if (currentBallPosition.getY() >= secondPlayerY && currentBallPosition.getY() <= secondPlayerY + 20 ) {
                                    currentBallAbscissaDirection = BallAbscissaDirection.LEFT;

                                    double difference = currentBallPosition.getY() - (secondPlayerY + 10);

                                    if (difference >= 0) {
                                        currentGame.setBallOrdinateDirection(BallOrdinateDirection.DOWN);
                                        currentGame.setBallOrdinateDirectionPower(difference * 0.12);
                                    } else {
                                        currentGame.setBallOrdinateDirection(BallOrdinateDirection.UP);
                                        currentGame.setBallOrdinateDirectionPower(difference * 0.12);
                                    }

                                    double x = currentBallPosition.getX() - 1;

                                    double y = currentBallPosition.getY();

                                    if (Objects.nonNull(currentGame.getBallOrdinateDirection())) {
                                        y += currentGame.getBallOrdinateDirectionPower();
                                    }

                                    ball.setPosition(new Point(x, y));
                                    currentGame.setBallAbscissaDirection(currentBallAbscissaDirection);
                                } else { //reset if not collision with second player
                                    ball.setPosition(new Point(50, (double) random.nextInt(99) + 1));
                                    currentGame.setBallAbscissaDirection(BallAbscissaDirection.values()[random.nextInt(2)]);
                                    currentGame.setBallOrdinateDirection(null);

                                    double firstPlayerScore = currentScore.getX();
                                    double secondPlayerScore = currentScore.getY();

                                    scoreState.setPosition(new Point(firstPlayerScore + 1, secondPlayerScore));
                                    currentGame.setScore(scoreState.getPosition());
                                    isScoreChanged = true;
                                }
                            } else { //not reach right side
                                double y = currentBallPosition.getY();

                                if (Objects.nonNull(currentGame.getBallOrdinateDirection())) {
                                    if (y >= 99) {
                                        currentGame.setBallOrdinateDirection(BallOrdinateDirection.UP);
                                        currentGame.setBallOrdinateDirectionPower(currentGame.getBallOrdinateDirectionPower() * -1);
                                    } else if (y <= 1) {
                                        currentGame.setBallOrdinateDirection(BallOrdinateDirection.DOWN);
                                        currentGame.setBallOrdinateDirectionPower(currentGame.getBallOrdinateDirectionPower() * -1);
                                    }

                                    y += currentGame.getBallOrdinateDirectionPower();
                                }

                                double x = currentBallPosition.getX() + 1;

                                ball.setPosition(new Point(x, y));
                            }
                        }
                    }
                    currentGame.setBallPosition(ball.getPosition());
                    messagingTemplate.convertAndSend("/topic/game/" + gameUid, ball);
                    if (isScoreChanged) {
                        messagingTemplate.convertAndSend("/topic/game/" + gameUid, scoreState);
                        if (scoreState.getPosition().getX() >= 10) {
                            game.setWinner(game.getFirstPlayer());
                            game.setStatus(GameStatusEnum.FINISHED);
                            gameService.update(game);
                            gameScheduler.cancelScheduledTask(gameUid);
                        } else if (scoreState.getPosition().getY() >= 10) {
                            game.setWinner(game.getSecondPlayer());
                            game.setStatus(GameStatusEnum.FINISHED);
                            gameService.update(game);
                            gameScheduler.cancelScheduledTask(gameUid);
                        }
                    }
                };

                gameScheduler.scheduleWithFixedDelayAndUid(ballPosition, Duration.ofMillis(60), gameUid);
            }
        }
    }

    public static String getWebsocketSessionId(StompHeaderAccessor headerAccessor) {
        return Objects.requireNonNull(headerAccessor.getHeader(SimpMessageHeaderAccessor.SESSION_ID_HEADER)).toString();
    }
}
