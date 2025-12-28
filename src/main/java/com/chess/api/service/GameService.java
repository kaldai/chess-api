package com.chess.api.service;

import com.chess.api.dto.*;
import com.chess.api.model.*;
import com.chess.api.model.enums.GameResult;
import com.chess.api.model.enums.GameStatus;
import com.chess.api.model.enums.GameType;
import com.chess.api.model.enums.InviteStatus;
import com.chess.api.repository.GameRepository;
import com.chess.api.repository.MoveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

  private final GameRepository gameRepository;
  private final MoveRepository moveRepository;
  private final PlayerService playerService;

  @Transactional
  public Game createGame(GameCreateRequest request, Player creator) {
    Game game = new Game();
    game.setWhitePlayer(creator);
    game.setGameType(request.getGameType());
    game.setStatus(GameStatus.WAITING);

    if (request.getTimeControl() != null && request.getTimeIncrement() != null) {
      game.setTimeControl(request.getTimeControl());
      game.setTimeIncrement(request.getTimeIncrement());
    } else {
      game.setTimeControl(request.getGameType().getBaseTime());
      game.setTimeIncrement(request.getGameType().getIncrement());
    }

    game.setWhiteTimeLeft(game.getTimeControl() * 1000);
    game.setBlackTimeLeft(game.getTimeControl() * 1000);
    game.setCreatedAt(LocalDateTime.now());

    return gameRepository.save(game);
  }

  public Game getGameById(Long id) {
    return gameRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Игра не найдена"));
  }

  @Transactional
  public Game joinGame(Long gameId, Player player) {
    Game game = getGameById(gameId);

    if (game.getBlackPlayer() == null && !player.equals(game.getWhitePlayer())) {
      game.setBlackPlayer(player);
      game.setStatus(GameStatus.ACTIVE);
      game.setStartedAt(LocalDateTime.now());
      return gameRepository.save(game);
    }

    throw new RuntimeException("Нельзя присоединиться к этой игре");
  }

  @Transactional
  public void resignGame(Long gameId, Player player) {
    Game game = getGameById(gameId);

    if (!game.getStatus().equals(GameStatus.ACTIVE)) {
      throw new RuntimeException("Игра не активна");
    }

    if (player.equals(game.getWhitePlayer())) {
      game.setResult(GameResult.BLACK_WIN);
      game.setStatus(GameStatus.BLACK_WON);
    } else if (player.equals(game.getBlackPlayer())) {
      game.setResult(GameResult.WHITE_WIN);
      game.setStatus(GameStatus.WHITE_WON);
    } else {
      throw new RuntimeException("Вы не участник этой игры");
    }

    game.setFinishedAt(LocalDateTime.now());
    gameRepository.save(game);

    // Обновляем рейтинги
    updatePlayerRatings(game);
  }

  @Transactional
  public void offerDraw(Long gameId, Player player) {
    Game game = getGameById(gameId);

    if (!game.getStatus().equals(GameStatus.ACTIVE)) {
      throw new RuntimeException("Игра не активна");
    }

    if (!player.equals(game.getWhitePlayer()) && !player.equals(game.getBlackPlayer())) {
      throw new RuntimeException("Вы не участник этой игры");
    }

    game.setStatus(GameStatus.DRAW_PROPOSED);
    gameRepository.save(game);
  }

  @Transactional
  public void acceptDraw(Long gameId, Player player) {
    Game game = getGameById(gameId);

    if (!game.getStatus().equals(GameStatus.DRAW_PROPOSED)) {
      throw new RuntimeException("Ничья не была предложена");
    }

    // Проверяем, что игрок принимает ничью от оппонента
    Player opponent = player.equals(game.getWhitePlayer()) ?
        game.getBlackPlayer() : game.getWhitePlayer();

    game.setStatus(GameStatus.DRAW);
    game.setResult(GameResult.DRAW);
    game.setFinishedAt(LocalDateTime.now());
    gameRepository.save(game);

    updatePlayerRatings(game);
  }

  @Transactional
  public void pauseGame(Long gameId, Player player) {
    Game game = getGameById(gameId);

    if (!game.getStatus().equals(GameStatus.ACTIVE)) {
      throw new RuntimeException("Игра не активна");
    }

    if (!player.equals(game.getWhitePlayer()) && !player.equals(game.getBlackPlayer())) {
      throw new RuntimeException("Вы не участник этой игры");
    }

    game.setStatus(GameStatus.PAUSED);
    gameRepository.save(game);
    log.info("Игра {} приостановлена игроком {}", gameId, player.getUsername());
  }

  @Transactional
  public void resumeGame(Long gameId, Player player) {
    Game game = getGameById(gameId);

    if (!game.getStatus().equals(GameStatus.PAUSED)) {
      throw new RuntimeException("Игра не приостановлена");
    }

    if (!player.equals(game.getWhitePlayer()) && !player.equals(game.getBlackPlayer())) {
      throw new RuntimeException("Вы не участник этой игры");
    }

    game.setStatus(GameStatus.ACTIVE);
    gameRepository.save(game);
    log.info("Игра {} возобновлена игроком {}", gameId, player.getUsername());
  }

  @Transactional
  public Game invitePlayer(GameInviteRequest inviteRequest, Player sender) {
    Player receiver = playerService.getPlayerById(inviteRequest.getPlayerId());

    if (sender.equals(receiver)) {
      throw new RuntimeException("Нельзя пригласить самого себя");
    }

    // Создаем игру
    Game game = new Game();
    game.setWhitePlayer(sender);
    game.setBlackPlayer(receiver);
    game.setGameType(inviteRequest.getGameType());
    game.setStatus(GameStatus.WAITING);

    if (inviteRequest.getTimeControl() != null && inviteRequest.getTimeIncrement() != null) {
      game.setTimeControl(inviteRequest.getTimeControl());
      game.setTimeIncrement(inviteRequest.getTimeIncrement());
    } else {
      game.setTimeControl(inviteRequest.getGameType().getBaseTime());
      game.setTimeIncrement(inviteRequest.getGameType().getIncrement());
    }

    game.setWhiteTimeLeft(game.getTimeControl() * 1000);
    game.setBlackTimeLeft(game.getTimeControl() * 1000);
    game.setCreatedAt(LocalDateTime.now());

    // Можно добавить GameInvite сущность, но для простоты используем статус WAITING
    log.info("Приглашение отправлено от {} к {} на игру {}",
        sender.getUsername(), receiver.getUsername(), inviteRequest.getGameType());

    return gameRepository.save(game);
  }

  public List<GameInviteDTO> getPlayerInvites(Player player) {
    // Поиск игр, где игрок является черными и игра в статусе WAITING
    List<Game> waitingGames = gameRepository.findByStatus(GameStatus.WAITING);
    List<GameInviteDTO> invites = new ArrayList<>();

    for (Game game : waitingGames) {
      if (game.getBlackPlayer() != null && game.getBlackPlayer().equals(player)) {
        GameInviteDTO invite = new GameInviteDTO();
        invite.setId(game.getId());
        invite.setSender(new PlayerDTO(
            game.getWhitePlayer().getId(),
            game.getWhitePlayer().getUsername(),
            getRatingForGameType(game.getWhitePlayer(), game.getGameType())
        ));
        invite.setReceiver(new PlayerDTO(
            game.getBlackPlayer().getId(),
            game.getBlackPlayer().getUsername(),
            getRatingForGameType(game.getBlackPlayer(), game.getGameType())
        ));
        invite.setGameType(game.getGameType());
        invite.setStatus(InviteStatus.PENDING);
        invite.setTimeControl(game.getTimeControl());
        invite.setTimeIncrement(game.getTimeIncrement());
        invite.setSentAt(game.getCreatedAt());
        invite.setGameId(game.getId());

        invites.add(invite);
      }
    }

    return invites;
  }

  @Transactional
  public Game acceptInvite(Long inviteId, Player player) {
    Game game = getGameById(inviteId);

    if (!game.getStatus().equals(GameStatus.WAITING)) {
      throw new RuntimeException("Приглашение уже обработано");
    }

    if (!game.getBlackPlayer().equals(player)) {
      throw new RuntimeException("Это приглашение не для вас");
    }

    game.setStatus(GameStatus.ACTIVE);
    game.setStartedAt(LocalDateTime.now());

    log.info("Приглашение {} принято игроком {}", inviteId, player.getUsername());
    return gameRepository.save(game);
  }

  @Transactional
  public void rejectInvite(Long inviteId, Player player) {
    Game game = getGameById(inviteId);

    if (!game.getStatus().equals(GameStatus.WAITING)) {
      throw new RuntimeException("Приглашение уже обработано");
    }

    if (!game.getBlackPlayer().equals(player)) {
      throw new RuntimeException("Это приглашение не для вас");
    }

    // Удаляем игру или помечаем как отклоненную
    gameRepository.delete(game);
    log.info("Приглашение {} отклонено игроком {}", inviteId, player.getUsername());
  }

  public List<Game> getActiveGames(Player player) {
    return gameRepository.findActiveGamesByPlayer(player, GameStatus.ACTIVE);
  }

  public List<Game> getPlayerGameHistory(Player player, int limit) {
    List<Game> finishedGames = gameRepository.findFinishedGamesByPlayer(player);

    if (limit > 0 && finishedGames.size() > limit) {
      return finishedGames.subList(0, limit);
    }

    return finishedGames;
  }

  public List<Game> getAllGames() {
    return gameRepository.findAll();
  }

  public List<Game> findWaitingGames() {
    return gameRepository.findWaitingGames();
  }

  @Transactional
  public Game saveGame(Game game) {
    return gameRepository.save(game);
  }

  @Transactional
  public Game saveMove(Game game, Move move) {
    move.setGame(game);
    moveRepository.save(move);
    game.getMoves().add(move);
    return gameRepository.save(game);
  }

  public List<MoveDTO> getGameMoves(Long gameId) {
    List<Move> moves = moveRepository.findByGameIdOrderByMoveNumberAsc(gameId);
    return moves.stream()
        .map(move -> new MoveDTO(
            move.getId(),
            move.getMoveNumber(),
            move.getFromSquare(),
            move.getToSquare(),
            move.getPromotion(),
            move.getSan(),
            move.getWhiteTimeLeft(),
            move.getBlackTimeLeft(),
            move.getTimestamp()
        ))
        .toList();
  }

  private void updatePlayerRatings(Game game) {
    if (game.getWhitePlayer() == null || game.getBlackPlayer() == null) {
      return;
    }

    int whiteRating = getRatingForGameType(game.getWhitePlayer(), game.getGameType());
    int blackRating = getRatingForGameType(game.getBlackPlayer(), game.getGameType());

    double expectedWhite = 1.0 / (1.0 + Math.pow(10, (blackRating - whiteRating) / 400.0));
    double expectedBlack = 1.0 - expectedWhite;

    int kFactor = 32;
    int whiteChange, blackChange;

    switch (game.getResult()) {
      case WHITE_WIN:
        whiteChange = (int) (kFactor * (1 - expectedWhite));
        blackChange = (int) (kFactor * (0 - expectedBlack));
        break;
      case BLACK_WIN:
        whiteChange = (int) (kFactor * (0 - expectedWhite));
        blackChange = (int) (kFactor * (1 - expectedBlack));
        break;
      default: // DRAW
        whiteChange = (int) (kFactor * (0.5 - expectedWhite));
        blackChange = (int) (kFactor * (0.5 - expectedBlack));
        break;
    }

    // Обновляем рейтинги через PlayerService
    playerService.updatePlayerRating(game.getWhitePlayer().getId(),
        game.getGameType().name(), whiteChange);
    playerService.updatePlayerRating(game.getBlackPlayer().getId(),
        game.getGameType().name(), blackChange);

    // Обновляем статистику игроков
    Player white = game.getWhitePlayer();
    Player black = game.getBlackPlayer();

    white.setGamesPlayed(white.getGamesPlayed() + 1);
    black.setGamesPlayed(black.getGamesPlayed() + 1);

    if (game.getResult() == GameResult.WHITE_WIN) {
      white.setGamesWon(white.getGamesWon() + 1);
      black.setGamesLost(black.getGamesLost() + 1);
    } else if (game.getResult() == GameResult.BLACK_WIN) {
      black.setGamesWon(black.getGamesWon() + 1);
      white.setGamesLost(white.getGamesLost() + 1);
    } else {
      white.setGamesDrawn(white.getGamesDrawn() + 1);
      black.setGamesDrawn(black.getGamesDrawn() + 1);
    }

    playerService.updatePlayerStatistics(white);
    playerService.updatePlayerStatistics(black);
  }

  private int getRatingForGameType(Player player, GameType gameType) {
    return switch (gameType) {
      case CLASSICAL -> player.getClassicalRating();
      case RAPID -> player.getRapidRating();
      case BLITZ -> player.getBlitzRating();
    };
  }
}