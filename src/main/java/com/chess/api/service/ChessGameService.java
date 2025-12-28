package com.chess.api.service;

import com.chess.api.dto.MoveRequest;
import com.chess.api.model.Game;
import com.chess.api.model.Move;
import com.chess.api.model.Player;
import com.chess.api.model.enums.GameResult;
import com.chess.api.model.enums.GameStatus;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChessGameService {

  private final ConcurrentHashMap<Long, GameTimer> gameTimers = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
  private final GameService gameService;

  public ChessGameService(GameService gameService) {
    this.gameService = gameService;
  }

  @Transactional
  public synchronized Move makeMove(Game game, Player player, MoveRequest moveRequest)
      throws IllegalMoveException {

    Board board = game.getChessBoard();
    Side sideToMove = board.getSideToMove();

    // Проверяем, чей ход
    if ((sideToMove == Side.WHITE && !player.equals(game.getWhitePlayer())) ||
        (sideToMove == Side.BLACK && !player.equals(game.getBlackPlayer()))) {
      throw new IllegalMoveException("Не ваш ход");
    }

    // Проверяем, что игра активна
    if (game.getStatus() != GameStatus.ACTIVE) {
      throw new IllegalMoveException("Игра не активна");
    }

    // Создаем ход
    com.github.bhlangonijr.chesslib.move.Move chessMove;
    try {
      Square from = Square.fromValue(moveRequest.getFrom().toUpperCase());
      Square to = Square.fromValue(moveRequest.getTo().toUpperCase());

      if (moveRequest.getPromotion() != null && !moveRequest.getPromotion().isEmpty()) {
        Piece promotionPiece = getPromotionPiece(moveRequest.getPromotion(), sideToMove);
        chessMove = new com.github.bhlangonijr.chesslib.move.Move(from, to, promotionPiece);
      } else {
        chessMove = new com.github.bhlangonijr.chesslib.move.Move(from, to);
      }
    } catch (Exception e) {
      throw new IllegalMoveException("Неверные параметры хода: " + e.getMessage());
    }

    // Проверяем легальность хода
    if (!board.isMoveLegal(chessMove, true)) {
      throw new IllegalMoveException("Недопустимый ход");
    }

    // Выполняем ход
    board.doMove(chessMove);

    // Обновляем состояние игры
    game.setCurrentFen(board.getFen());

    // Сохраняем ход в БД
    Move move = new Move();
    move.setGame(game);
    move.setFromSquare(moveRequest.getFrom());
    move.setToSquare(moveRequest.getTo());
    move.setPromotion(moveRequest.getPromotion());
    move.setSan(chessMove.toString());
    move.setMoveNumber(game.getMoves().size() + 1);

    // Обновляем таймер
    GameTimer timer = gameTimers.get(game.getId());
    if (timer != null) {
      timer.switchTurn();
      move.setWhiteTimeLeft(timer.getWhiteTimeLeft());
      move.setBlackTimeLeft(timer.getBlackTimeLeft());
    }

    // Сохраняем ход через GameService
    gameService.saveMove(game, move);

    // Проверяем окончание игры
    checkGameEnd(game, board);

    return move;
  }

  private Piece getPromotionPiece(String promotion, Side side) {
    PieceType type = switch (promotion.toUpperCase()) {
      case "Q" -> PieceType.QUEEN;
      case "R" -> PieceType.ROOK;
      case "B" -> PieceType.BISHOP;
      case "N" -> PieceType.KNIGHT;
      default -> throw new IllegalArgumentException("Неверный тип фигуры для превращения");
    };
    return Piece.make(side, type);
  }

  private void checkGameEnd(Game game, Board board) {
    if (board.isMated()) {
      if (board.getSideToMove() == Side.WHITE) {
        game.setStatus(GameStatus.BLACK_WON);
        game.setResult(GameResult.BLACK_WIN);
      } else {
        game.setStatus(GameStatus.WHITE_WON);
        game.setResult(GameResult.WHITE_WIN);
      }
      stopGameTimer(game.getId());
    } else if (board.isStaleMate() || board.isDraw()) {
      game.setStatus(GameStatus.DRAW);
      game.setResult(GameResult.DRAW);
      stopGameTimer(game.getId());
    }

    if (game.getStatus().ordinal() >= GameStatus.WHITE_WON.ordinal()) {
      game.setFinishedAt(LocalDateTime.now());
    }
  }

  public void startGameTimer(Game game) {
    GameTimer timer = new GameTimer(
        game.getId(),
        game.getWhiteTimeLeft(),
        game.getBlackTimeLeft(),
        game.getTimeIncrement() * 1000,
        this::onTimeExpired
    );

    gameTimers.put(game.getId(), timer);
    scheduler.scheduleAtFixedRate(timer, 1, 1, TimeUnit.SECONDS);
  }

  private void onTimeExpired(Long gameId, Side side) {
    log.info("Время вышло для игры {}, сторона: {}", gameId, side);

    try {
      Game game = gameService.getGameById(gameId);

      if (side == Side.WHITE) {
        game.setResult(GameResult.BLACK_WIN);
        game.setStatus(GameStatus.BLACK_WON);
      } else {
        game.setResult(GameResult.WHITE_WIN);
        game.setStatus(GameStatus.WHITE_WON);
      }

      game.setFinishedAt(LocalDateTime.now());
      gameService.saveGame(game);
    } catch (Exception e) {
      log.error("Ошибка при обработке окончания времени для игры {}", gameId, e);
    }
  }

  public void stopGameTimer(Long gameId) {
    GameTimer timer = gameTimers.remove(gameId);
    if (timer != null) {
      timer.stop();
    }
  }

  public Integer[] getTimeLeft(Long gameId) {
    GameTimer timer = gameTimers.get(gameId);
    if (timer != null) {
      return new Integer[]{timer.getWhiteTimeLeft(), timer.getBlackTimeLeft()};
    }
    return new Integer[]{0, 0};
  }

  public List<String> getLegalMoves(Game game, String square) {
    Board board = game.getChessBoard();
    try {
      Square sq = Square.fromValue(square.toUpperCase());
      return board.legalMoves().stream()
          .filter(move -> move.getFrom() == sq)
          .map(com.github.bhlangonijr.chesslib.move.Move::toString)
          .toList();
    } catch (Exception e) {
      return List.of();
    }
  }

  public static class IllegalMoveException extends Exception {
    public IllegalMoveException(String message) {
      super(message);
    }
  }
}