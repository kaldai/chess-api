package com.chess.api.websocket;

import com.chess.api.dto.ChatMessage;
import com.chess.api.dto.GameMessage;
import com.chess.api.dto.MoveRequest;
import com.chess.api.model.Game;
import com.chess.api.model.Player;
import com.chess.api.model.enums.GameResult;
import com.chess.api.model.enums.GameStatus;
import com.chess.api.service.ChessGameService;
import com.chess.api.service.GameService;
import com.chess.api.service.PlayerService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketController {

  private final ChessGameService chessGameService;
  private final GameService gameService;
  private final PlayerService playerService;
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * Обработка хода в игре через WebSocket
   */
  @MessageMapping("/game/{gameId}/move")
  public void handleMove(
      @DestinationVariable Long gameId,
      MoveRequest moveRequest,
      Principal principal) {

    log.info("Received move for game {} from {}: {} to {}",
        gameId, principal.getName(), moveRequest.getFrom(), moveRequest.getTo());

    try {
      Game game = gameService.getGameById(gameId);
      Player player = playerService.getPlayerByUsername(principal.getName());

      // Проверяем, что игрок участвует в игре
      if (!game.isPlayerInGame(player)) {
        sendError(gameId, "Вы не участник этой игры");
        return;
      }

      // Проверяем, что игра активна
      if (game.getStatus() != GameStatus.ACTIVE) {
        sendError(gameId, "Игра не активна");
        return;
      }

      // Проверяем, что ход игрока
      if (!game.isPlayerTurn(player)) {
        sendError(gameId, "Сейчас не ваш ход");
        return;
      }

      // Выполняем ход
      var move = chessGameService.makeMove(game, player, moveRequest);

      // Отправляем обновленное состояние игры всем подписчикам
      GameStateUpdate gameUpdate = new GameStateUpdate();
      gameUpdate.setType("MOVE_MADE");
      gameUpdate.setGame(gameService.getGameById(gameId)); // Обновленная игра
      gameUpdate.setMove(move);
      gameUpdate.setTimestamp(LocalDateTime.now());

      messagingTemplate.convertAndSend("/topic/game/" + gameId, gameUpdate);

      // Проверяем, закончилась ли игра
      if (game.getStatus() != GameStatus.ACTIVE) {
        GameStateUpdate endGameUpdate = new GameStateUpdate();
        endGameUpdate.setType("GAME_ENDED");
        endGameUpdate.setGame(game);
        endGameUpdate.setResult(game.getResult().toString());
        endGameUpdate.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/game/" + gameId, endGameUpdate);
        chessGameService.stopGameTimer(gameId);
      }

    } catch (ChessGameService.IllegalMoveException e) {
      log.warn("Illegal move attempted: {}", e.getMessage());
      sendError(gameId, "Недопустимый ход: " + e.getMessage());

      // Отправляем обновление только отправителю
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("type", "MOVE_REJECTED");
      errorResponse.put("message", e.getMessage());
      errorResponse.put("move", moveRequest);
      messagingTemplate.convertAndSendToUser(
          principal.getName(),
          "/queue/errors",
          errorResponse
      );
    } catch (Exception e) {
      log.error("Error processing move", e);
      sendError(gameId, "Ошибка при обработке хода");
    }
  }

  /**
   * Обработка сообщений чата в игре
   */
  @MessageMapping("/game/{gameId}/chat")
  @SendTo("/topic/chat/{gameId}")
  public ChatMessage handleChat(
      @DestinationVariable Long gameId,
      ChatMessage chatMessage,
      Principal principal) {

    chatMessage.setSender(principal.getName());
    chatMessage.setTimestamp(LocalDateTime.now());
    chatMessage.setType("CHAT_MESSAGE");

    log.info("Chat message from {} in game {}: {}",
        chatMessage.getSender(), gameId, chatMessage.getContent());

    // Проверяем, что отправитель участвует в игре
    try {
      Game game = gameService.getGameById(gameId);
      Player player = playerService.getPlayerByUsername(principal.getName());

      if (!game.isPlayerInGame(player)) {
        chatMessage.setType("SYSTEM_MESSAGE");
        chatMessage.setContent("Сообщения от неучастников игры не допускаются");
        chatMessage.setSender("SYSTEM");
      }
    } catch (Exception e) {
      log.warn("Player not in game or game not found", e);
      chatMessage.setType("SYSTEM_MESSAGE");
      chatMessage.setContent("Ошибка отправки сообщения");
      chatMessage.setSender("SYSTEM");
    }

    return chatMessage;
  }

  /**
   * Присоединение к игре через WebSocket
   */
  @MessageMapping("/game/{gameId}/join")
  public void handleJoin(
      @DestinationVariable Long gameId,
      Principal principal) {

    log.info("Player {} attempting to join game {}", principal.getName(), gameId);

    try {
      Game game = gameService.getGameById(gameId);
      Player player = playerService.getPlayerByUsername(principal.getName());

      // Если черный игрок еще не присоединился, присоединяем его
      if (game.getBlackPlayer() == null && !game.getWhitePlayer().equals(player)) {
        game.setBlackPlayer(player);

        // Если теперь есть оба игрока, начинаем игру
        if (game.getWhitePlayer() != null && game.getBlackPlayer() != null) {
          game.setStatus(GameStatus.ACTIVE);
          game.setStartedAt(LocalDateTime.now());
          gameService.saveGame(game);
          chessGameService.startGameTimer(game);

          // Отправляем сообщение о начале игры
          GameStateUpdate startUpdate = new GameStateUpdate();
          startUpdate.setType("GAME_STARTED");
          startUpdate.setGame(game);
          startUpdate.setTimestamp(LocalDateTime.now());

          messagingTemplate.convertAndSend("/topic/game/" + gameId, startUpdate);
        } else {
          gameService.saveGame(game);
        }

        // Отправляем уведомление о присоединении
        GameStateUpdate joinUpdate = new GameStateUpdate();
        joinUpdate.setType("PLAYER_JOINED");
        joinUpdate.setPlayer(player.getUsername());
        joinUpdate.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/game/" + gameId, joinUpdate);

      } else if (game.isPlayerInGame(player)) {
        // Игрок уже в игре, просто отправляем уведомление
        GameStateUpdate reconnectUpdate = new GameStateUpdate();
        reconnectUpdate.setType("PLAYER_RECONNECTED");
        reconnectUpdate.setPlayer(player.getUsername());
        reconnectUpdate.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/game/" + gameId, reconnectUpdate);
      } else {
        sendError(gameId, "Невозможно присоединиться к игре");
      }

    } catch (Exception e) {
      log.error("Error joining game", e);
      sendError(gameId, "Ошибка при присоединении к игре");
    }
  }

  /**
   * Выход из игры
   */
  @MessageMapping("/game/{gameId}/leave")
  @SendTo("/topic/game/{gameId}")
  public GameMessage handleLeave(
      @DestinationVariable Long gameId,
      Principal principal) {

    GameMessage message = new GameMessage();
    message.setType("PLAYER_LEFT");
    message.setContent("Игрок " + principal.getName() + " покинул игру");
    message.setSender(principal.getName());
    message.setTimestamp(LocalDateTime.now());

    log.info("Player {} left game {}", principal.getName(), gameId);

    try {
      // Останавливаем таймер, если игрок выходит из активной игры
      Game game = gameService.getGameById(gameId);
      if (game.getStatus() == GameStatus.ACTIVE) {
        chessGameService.stopGameTimer(gameId);

        // Автоматически присуждаем победу оставшемуся игроку
        if (game.getWhitePlayer().getUsername().equals(principal.getName())) {
          game.setStatus(GameStatus.BLACK_WON);
          game.setResult(GameResult.BLACK_WIN);
        } else {
          game.setStatus(GameStatus.WHITE_WON);
          game.setResult(GameResult.WHITE_WIN);
        }
        game.setFinishedAt(LocalDateTime.now());
        gameService.saveGame(game);
      }
    } catch (Exception e) {
      log.error("Error processing player leave", e);
    }

    return message;
  }

  /**
   * Предложение ничьей
   */
  @MessageMapping("/game/{gameId}/draw-offer")
  public void handleDrawOffer(
      @DestinationVariable Long gameId,
      Principal principal) {

    try {
      Game game = gameService.getGameById(gameId);
      Player player = playerService.getPlayerByUsername(principal.getName());

      if (!game.isPlayerInGame(player)) {
        sendError(gameId, "Вы не участник этой игры");
        return;
      }

      GameStateUpdate drawOffer = new GameStateUpdate();
      drawOffer.setType("DRAW_OFFERED");
      drawOffer.setPlayer(player.getUsername());
      drawOffer.setTimestamp(LocalDateTime.now());

      messagingTemplate.convertAndSend("/topic/game/" + gameId, drawOffer);

      // Также отправляем приватное уведомление оппоненту
      Player opponent = game.getWhitePlayer().equals(player) ?
          game.getBlackPlayer() : game.getWhitePlayer();

      if (opponent != null) {
        Map<String, Object> privateMessage = new HashMap<>();
        privateMessage.put("type", "DRAW_OFFER_RECEIVED");
        privateMessage.put("from", player.getUsername());
        privateMessage.put("gameId", gameId);

        messagingTemplate.convertAndSendToUser(
            opponent.getUsername(),
            "/queue/notifications",
            privateMessage
        );
      }

    } catch (Exception e) {
      log.error("Error processing draw offer", e);
      sendError(gameId, "Ошибка при предложении ничьей");
    }
  }

  /**
   * Принятие ничьей
   */
  @MessageMapping("/game/{gameId}/draw-accept")
  public void handleDrawAccept(
      @DestinationVariable Long gameId,
      Principal principal) {

    try {
      Game game = gameService.getGameById(gameId);
      Player player = playerService.getPlayerByUsername(principal.getName());

      if (!game.isPlayerInGame(player)) {
        sendError(gameId, "Вы не участник этой игры");
        return;
      }

      game.setStatus(GameStatus.DRAW);
      game.setResult(GameResult.DRAW);
      game.setFinishedAt(LocalDateTime.now());
      gameService.saveGame(game);
      chessGameService.stopGameTimer(gameId);

      GameStateUpdate drawAccepted = new GameStateUpdate();
      drawAccepted.setType("DRAW_ACCEPTED");
      drawAccepted.setGame(game);
      drawAccepted.setTimestamp(LocalDateTime.now());

      messagingTemplate.convertAndSend("/topic/game/" + gameId, drawAccepted);

    } catch (Exception e) {
      log.error("Error processing draw accept", e);
      sendError(gameId, "Ошибка при принятии ничьей");
    }
  }

  /**
   * Сдача в игре
   */
  @MessageMapping("/game/{gameId}/resign")
  public void handleResign(
      @DestinationVariable Long gameId,
      Principal principal) {

    try {
      Game game = gameService.getGameById(gameId);
      Player player = playerService.getPlayerByUsername(principal.getName());

      if (!game.isPlayerInGame(player)) {
        sendError(gameId, "Вы не участник этой игры");
        return;
      }

      if (game.getStatus() != GameStatus.ACTIVE) {
        sendError(gameId, "Игра не активна");
        return;
      }

      if (game.getWhitePlayer().equals(player)) {
        game.setStatus(GameStatus.BLACK_WON);
        game.setResult(GameResult.BLACK_WIN);
      } else {
        game.setStatus(GameStatus.WHITE_WON);
        game.setResult(GameResult.WHITE_WIN);
      }

      game.setFinishedAt(LocalDateTime.now());
      gameService.saveGame(game);
      chessGameService.stopGameTimer(gameId);

      GameStateUpdate resignUpdate = new GameStateUpdate();
      resignUpdate.setType("PLAYER_RESIGNED");
      resignUpdate.setGame(game);
      resignUpdate.setPlayer(player.getUsername());
      resignUpdate.setTimestamp(LocalDateTime.now());

      messagingTemplate.convertAndSend("/topic/game/" + gameId, resignUpdate);

    } catch (Exception e) {
      log.error("Error processing resignation", e);
      sendError(gameId, "Ошибка при сдаче");
    }
  }

  /**
   * Запрос оставшегося времени
   */
  @MessageMapping("/game/{gameId}/time")
  public void handleTimeRequest(
      @DestinationVariable Long gameId,
      Principal principal) {

    try {
      Integer[] timeLeft = chessGameService.getTimeLeft(gameId);

      Map<String, Object> timeUpdate = new HashMap<>();
      timeUpdate.put("type", "TIME_UPDATE");
      timeUpdate.put("whiteTimeLeft", timeLeft[0]);
      timeUpdate.put("blackTimeLeft", timeLeft[1]);
      timeUpdate.put("timestamp", LocalDateTime.now());

      messagingTemplate.convertAndSend("/topic/game/" + gameId, timeUpdate);

    } catch (Exception e) {
      log.error("Error processing time request", e);
    }
  }

  /**
   * Запрос текущего состояния игры
   */
  @MessageMapping("/game/{gameId}/state")
  public void handleStateRequest(
      @DestinationVariable Long gameId,
      Principal principal) {

    try {
      Game game = gameService.getGameById(gameId);

      GameStateUpdate stateUpdate = new GameStateUpdate();
      stateUpdate.setType("GAME_STATE");
      stateUpdate.setGame(game);
      stateUpdate.setTimestamp(LocalDateTime.now());

      // Отправляем только запросившему игроку
      messagingTemplate.convertAndSendToUser(
          principal.getName(),
          "/queue/game-state",
          stateUpdate
      );

    } catch (Exception e) {
      log.error("Error processing state request", e);
      sendError(gameId, "Ошибка при получении состояния игры");
    }
  }

  /**
   * Вспомогательный метод для отправки ошибок
   */
  private void sendError(Long gameId, String message) {
    Map<String, Object> error = new HashMap<>();
    error.put("type", "ERROR");
    error.put("message", message);
    error.put("timestamp", LocalDateTime.now());

    messagingTemplate.convertAndSend("/topic/game/" + gameId + "/errors", error);
  }
}

// DTO для обновления состояния игры через WebSocket
@Data
@NoArgsConstructor
@AllArgsConstructor
class GameStateUpdate {
  private String type;
  private Game game;
  private com.chess.api.model.Move move;
  private String player;
  private String result;
  private LocalDateTime timestamp;
}