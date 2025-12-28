package com.chess.api.controller;

import com.chess.api.dto.ApiResponse;
import com.chess.api.dto.GameCreateRequest;
import com.chess.api.dto.GameDTO;
import com.chess.api.dto.GameInviteDTO;
import com.chess.api.dto.GameInviteRequest;
import com.chess.api.dto.MoveDTO;
import com.chess.api.dto.MoveRequest;
import com.chess.api.dto.PlayerDTO;
import com.chess.api.model.Game;
import com.chess.api.model.Player;
import com.chess.api.model.enums.GameStatus;
import com.chess.api.service.ChessGameService;
import com.chess.api.service.GameService;
import com.chess.api.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Slf4j
public class GameController {

  private final GameService gameService;
  private final ChessGameService chessGameService;
  private final PlayerService playerService;

  @PostMapping
  public ResponseEntity<ApiResponse<GameDTO>> createGame(
      @Valid @RequestBody GameCreateRequest request,
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено на UserDetails

    // Загружаем полного игрока из базы данных
    Player creator = playerService.getPlayerByUsername(userDetails.getUsername());

    Game game = gameService.createGame(request, creator);
    log.info("Игра создана: ID={}, тип={}, игрок={}",
        game.getId(), game.getGameType(), creator.getUsername());
    return ResponseEntity.ok(ApiResponse.success("Игра создана", convertToDTO(game)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<GameDTO>> getGame(@PathVariable Long id) {
    Game game = gameService.getGameById(id);
    return ResponseEntity.ok(ApiResponse.success(convertToDTO(game)));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<GameDTO>>> getAllGames() {
    List<Game> games = gameService.getAllGames();
    List<GameDTO> dtos = games.stream().map(this::convertToDTO).toList();
    return ResponseEntity.ok(ApiResponse.success(dtos));
  }

  @GetMapping("/active")
  public ResponseEntity<ApiResponse<List<GameDTO>>> getActiveGames(
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    List<Game> games = gameService.getActiveGames(currentPlayer);
    List<GameDTO> dtos = games.stream().map(this::convertToDTO).toList();
    return ResponseEntity.ok(ApiResponse.success(dtos));
  }

  @GetMapping("/waiting")
  public ResponseEntity<ApiResponse<List<GameDTO>>> getWaitingGames() {
    List<Game> games = gameService.findWaitingGames();
    List<GameDTO> dtos = games.stream().map(this::convertToDTO).toList();
    return ResponseEntity.ok(ApiResponse.success(dtos));
  }

  @PostMapping("/{id}/join")
  public ResponseEntity<ApiResponse<GameDTO>> joinGame(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    Game game = gameService.joinGame(id, currentPlayer);

    if (game.getStatus() == GameStatus.ACTIVE) {
      chessGameService.startGameTimer(game);
      log.info("Игра {} началась: {} vs {}",
          game.getId(), game.getWhitePlayer().getUsername(), game.getBlackPlayer().getUsername());
    }

    return ResponseEntity.ok(ApiResponse.success("Вы присоединились к игре", convertToDTO(game)));
  }

  @PostMapping("/{id}/move")
  public ResponseEntity<ApiResponse<MoveDTO>> makeMove(
      @PathVariable Long id,
      @Valid @RequestBody MoveRequest moveRequest,
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    try {
      Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
      Game game = gameService.getGameById(id);
      var move = chessGameService.makeMove(game, currentPlayer, moveRequest);
      return ResponseEntity.ok(ApiResponse.success("Ход принят", convertToDTO(move)));
    } catch (ChessGameService.IllegalMoveException e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }

  @GetMapping("/{id}/moves")
  public ResponseEntity<ApiResponse<List<MoveDTO>>> getGameMoves(@PathVariable Long id) {
    List<MoveDTO> moves = gameService.getGameMoves(id);
    return ResponseEntity.ok(ApiResponse.success(moves));
  }

  @PostMapping("/{id}/resign")
  public ResponseEntity<ApiResponse<Void>> resignGame(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    gameService.resignGame(id, currentPlayer);
    chessGameService.stopGameTimer(id);
    return ResponseEntity.ok(ApiResponse.<Void>success("Вы сдались", null));
  }

  @PostMapping("/{id}/draw-offer")
  public ResponseEntity<ApiResponse<Void>> offerDraw(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    gameService.offerDraw(id, currentPlayer);
    return ResponseEntity.ok(ApiResponse.<Void>success("Предложение ничьи отправлено", null));
  }

  @PostMapping("/{id}/draw-accept")
  public ResponseEntity<ApiResponse<Void>> acceptDraw(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    gameService.acceptDraw(id, currentPlayer);
    chessGameService.stopGameTimer(id);
    return ResponseEntity.ok(ApiResponse.<Void>success("Ничья принята", null));
  }

  @GetMapping("/{id}/legal-moves")
  public ResponseEntity<ApiResponse<List<String>>> getLegalMoves(
      @PathVariable Long id,
      @RequestParam String square) {

    Game game = gameService.getGameById(id);
    List<String> legalMoves = chessGameService.getLegalMoves(game, square);
    return ResponseEntity.ok(ApiResponse.success(legalMoves));
  }

  @GetMapping("/{id}/time-left")
  public ResponseEntity<ApiResponse<Integer[]>> getTimeLeft(@PathVariable Long id) {
    Integer[] timeLeft = chessGameService.getTimeLeft(id);
    return ResponseEntity.ok(ApiResponse.success(timeLeft));
  }

  @PostMapping("/{id}/pause")
  public ResponseEntity<ApiResponse<Void>> pauseGame(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    gameService.pauseGame(id, currentPlayer);
    return ResponseEntity.ok(ApiResponse.<Void>success("Игра приостановлена", null));
  }

  @PostMapping("/{id}/resume")
  public ResponseEntity<ApiResponse<Void>> resumeGame(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    gameService.resumeGame(id, currentPlayer);
    return ResponseEntity.ok(ApiResponse.<Void>success("Игра возобновлена", null));
  }

  @GetMapping("/history")
  public ResponseEntity<ApiResponse<List<GameDTO>>> getGameHistory(
      @AuthenticationPrincipal UserDetails userDetails, // Изменено
      @RequestParam(defaultValue = "10") int limit) {

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    List<Game> games = gameService.getPlayerGameHistory(currentPlayer, limit);
    List<GameDTO> dtos = games.stream().map(this::convertToDTO).toList();
    return ResponseEntity.ok(ApiResponse.success(dtos));
  }

  @PostMapping("/invite")
  public ResponseEntity<ApiResponse<GameDTO>> invitePlayer(
      @Valid @RequestBody GameInviteRequest inviteRequest,
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    Game game = gameService.invitePlayer(inviteRequest, currentPlayer);
    return ResponseEntity.ok(ApiResponse.success("Приглашение отправлено", convertToDTO(game)));
  }

  @GetMapping("/invites")
  public ResponseEntity<ApiResponse<List<GameInviteDTO>>> getInvites(
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    List<GameInviteDTO> invites = gameService.getPlayerInvites(currentPlayer);
    return ResponseEntity.ok(ApiResponse.success(invites));
  }

  @PostMapping("/invites/{inviteId}/accept")
  public ResponseEntity<ApiResponse<GameDTO>> acceptInvite(
      @PathVariable Long inviteId,
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    Game game = gameService.acceptInvite(inviteId, currentPlayer);
    chessGameService.startGameTimer(game);
    return ResponseEntity.ok(ApiResponse.success("Приглашение принято", convertToDTO(game)));
  }

  @PostMapping("/invites/{inviteId}/reject")
  public ResponseEntity<ApiResponse<Void>> rejectInvite(
      @PathVariable Long inviteId,
      @AuthenticationPrincipal UserDetails userDetails) { // Изменено

    Player currentPlayer = playerService.getPlayerByUsername(userDetails.getUsername());
    gameService.rejectInvite(inviteId, currentPlayer);
    return ResponseEntity.ok(ApiResponse.<Void>success("Приглашение отклонено", null));
  }

  private GameDTO convertToDTO(Game game) {
    GameDTO dto = new GameDTO();
    dto.setId(game.getId());

    if (game.getWhitePlayer() != null) {
      dto.setWhitePlayer(new PlayerDTO(
          game.getWhitePlayer().getId(),
          game.getWhitePlayer().getUsername(),
          getRatingForGameType(game.getWhitePlayer(), game.getGameType())
      ));
    }

    if (game.getBlackPlayer() != null) {
      dto.setBlackPlayer(new PlayerDTO(
          game.getBlackPlayer().getId(),
          game.getBlackPlayer().getUsername(),
          getRatingForGameType(game.getBlackPlayer(), game.getGameType())
      ));
    }

    dto.setGameType(game.getGameType());
    dto.setStatus(game.getStatus());
    dto.setCurrentFen(game.getCurrentFen());
    dto.setPgn(game.getPgn());
    dto.setTimeControl(game.getTimeControl());
    dto.setTimeIncrement(game.getTimeIncrement());
    dto.setWhiteTimeLeft(game.getWhiteTimeLeft());
    dto.setBlackTimeLeft(game.getBlackTimeLeft());
    dto.setCreatedAt(game.getCreatedAt());
    dto.setStartedAt(game.getStartedAt());
    dto.setFinishedAt(game.getFinishedAt());
    dto.setResult(game.getResult() != null ? game.getResult().name() : null);

    return dto;
  }

  private MoveDTO convertToDTO(com.chess.api.model.Move move) {
    return new MoveDTO(
        move.getId(),
        move.getMoveNumber(),
        move.getFromSquare(),
        move.getToSquare(),
        move.getPromotion(),
        move.getSan(),
        move.getWhiteTimeLeft(),
        move.getBlackTimeLeft(),
        move.getTimestamp()
    );
  }

  private int getRatingForGameType(Player player, com.chess.api.model.enums.GameType gameType) {
    return switch (gameType) {
      case CLASSICAL -> player.getClassicalRating();
      case RAPID -> player.getRapidRating();
      case BLITZ -> player.getBlitzRating();
    };
  }
}