package com.chess.api.service;

import com.chess.api.dto.*;
import com.chess.api.model.*;
import com.chess.api.model.enums.GameResult;
import com.chess.api.model.enums.GameStatus;
import com.chess.api.model.enums.GameType;
import com.chess.api.model.enums.InviteStatus;
import com.chess.api.repository.GameInviteRepository;
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
  private final GameInviteRepository gameInviteRepository;
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
  public GameInvite invitePlayer(GameInviteRequest inviteRequest, Player sender) {
    Player receiver = playerService.getPlayerById(inviteRequest.getPlayerId());

    if (sender.equals(receiver)) {
      throw new RuntimeException("Нельзя пригласить самого себя");
    }

    // Проверяем, есть ли уже активное приглашение
    boolean existingInvite = gameInviteRepository.existsBySenderAndReceiverAndStatus(
        sender, receiver, InviteStatus.PENDING);

    if (existingInvite) {
      throw new RuntimeException("Приглашение уже отправлено этому игроку");
    }

    // Создаем приглашение
    GameInvite invite = new GameInvite();
    invite.setSender(sender);
    invite.setReceiver(receiver);
    invite.setGameType(inviteRequest.getGameType());
    invite.setStatus(InviteStatus.PENDING);

    if (inviteRequest.getTimeControl() != null && inviteRequest.getTimeIncrement() != null) {
      invite.setTimeControl(inviteRequest.getTimeControl());
      invite.setTimeIncrement(inviteRequest.getTimeIncrement());
    } else {
      invite.setTimeControl(inviteRequest.getGameType().getBaseTime());
      invite.setTimeIncrement(inviteRequest.getGameType().getIncrement());
    }

    invite.setSentAt(LocalDateTime.now());
    invite.setExpiresAt(LocalDateTime.now().plusDays(7)); // Приглашение истекает через 7 дней

    log.info("Приглашение отправлено от {} к {} на игру {}",
        sender.getUsername(), receiver.getUsername(), inviteRequest.getGameType());

    return gameInviteRepository.save(invite);
  }

  public List<GameInviteDTO> getPlayerInvites(Player player) {
    // Получаем все приглашения, где игрок является получателем и статус PENDING
    List<GameInvite> invites = gameInviteRepository.findByReceiverAndStatus(
        player, InviteStatus.PENDING);

    List<GameInviteDTO> inviteDTOs = new ArrayList<>();

    for (GameInvite invite : invites) {
      GameInviteDTO dto = new GameInviteDTO();
      dto.setId(invite.getId());
      dto.setSender(new PlayerDTO(
          invite.getSender().getId(),
          invite.getSender().getUsername(),
          getRatingForGameType(invite.getSender(), invite.getGameType())
      ));
      dto.setReceiver(new PlayerDTO(
          invite.getReceiver().getId(),
          invite.getReceiver().getUsername(),
          getRatingForGameType(invite.getReceiver(), invite.getGameType())
      ));
      dto.setGameType(invite.getGameType());
      dto.setStatus(invite.getStatus());
      dto.setTimeControl(invite.getTimeControl());
      dto.setTimeIncrement(invite.getTimeIncrement());
      dto.setSentAt(invite.getSentAt());
      dto.setRespondedAt(invite.getRespondedAt());
      dto.setGameId(invite.getGame() != null ? invite.getGame().getId() : null);

      inviteDTOs.add(dto);
    }

    return inviteDTOs;
  }

  public List<GameInviteDTO> getSentInvites(Player player) {
    // Получаем все приглашения, которые игрок отправил
    List<GameInvite> sentInvites = gameInviteRepository.findBySenderAndStatus(
        player, InviteStatus.PENDING);

    return sentInvites.stream()
        .map(invite -> new GameInviteDTO(
            invite.getId(),
            new PlayerDTO(
                invite.getSender().getId(),
                invite.getSender().getUsername(),
                getRatingForGameType(invite.getSender(), invite.getGameType())
            ),
            new PlayerDTO(
                invite.getReceiver().getId(),
                invite.getReceiver().getUsername(),
                getRatingForGameType(invite.getReceiver(), invite.getGameType())
            ),
            invite.getGameType(),
            invite.getStatus(),
            invite.getTimeControl(),
            invite.getTimeIncrement(),
            invite.getSentAt(),
            invite.getRespondedAt(),
            invite.getGame() != null ? invite.getGame().getId() : null
        ))
        .toList();
  }

  @Transactional
  public Game acceptInvite(Long inviteId, Player player) {
    GameInvite invite = gameInviteRepository.findByIdAndReceiver(inviteId, player)
        .orElseThrow(() -> new RuntimeException("Приглашение не найдено или не для вас"));

    if (!invite.getStatus().equals(InviteStatus.PENDING)) {
      throw new RuntimeException("Приглашение уже обработано");
    }

    // Проверяем, не истекло ли приглашение
    if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
      invite.setStatus(InviteStatus.EXPIRED);
      gameInviteRepository.save(invite);
      throw new RuntimeException("Приглашение истекло");
    }

    // Создаем игру на основе приглашения
    Game game = new Game();
    game.setWhitePlayer(invite.getSender());
    game.setBlackPlayer(invite.getReceiver());
    game.setGameType(invite.getGameType());
    game.setStatus(GameStatus.ACTIVE);
    game.setTimeControl(invite.getTimeControl());
    game.setTimeIncrement(invite.getTimeIncrement());
    game.setWhiteTimeLeft(game.getTimeControl() * 1000);
    game.setBlackTimeLeft(game.getTimeControl() * 1000);
    game.setCreatedAt(LocalDateTime.now());
    game.setStartedAt(LocalDateTime.now());

    Game savedGame = gameRepository.save(game);

    // Обновляем приглашение
    invite.setStatus(InviteStatus.ACCEPTED);
    invite.setRespondedAt(LocalDateTime.now());
    invite.setGame(savedGame);
    gameInviteRepository.save(invite);

    log.info("Приглашение {} принято игроком {}. Игра {} создана",
        inviteId, player.getUsername(), savedGame.getId());

    return savedGame;
  }

  @Transactional
  public void rejectInvite(Long inviteId, Player player) {
    GameInvite invite = gameInviteRepository.findByIdAndReceiver(inviteId, player)
        .orElseThrow(() -> new RuntimeException("Приглашение не найдено или не для вас"));

    if (!invite.getStatus().equals(InviteStatus.PENDING)) {
      throw new RuntimeException("Приглашение уже обработано");
    }

    invite.setStatus(InviteStatus.REJECTED);
    invite.setRespondedAt(LocalDateTime.now());
    gameInviteRepository.save(invite);

    log.info("Приглашение {} отклонено игроком {}", inviteId, player.getUsername());
  }

  @Transactional
  public void cancelInvite(Long inviteId, Player player) {
    GameInvite invite = gameInviteRepository.findByIdAndSender(inviteId, player)
        .orElseThrow(() -> new RuntimeException("Приглашение не найдено или вы не отправитель"));

    if (!invite.getStatus().equals(InviteStatus.PENDING)) {
      throw new RuntimeException("Приглашение уже обработано, нельзя отменить");
    }

    invite.setStatus(InviteStatus.CANCELLED);
    invite.setRespondedAt(LocalDateTime.now());
    gameInviteRepository.save(invite);

    log.info("Приглашение {} отменено отправителем {}", inviteId, player.getUsername());
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