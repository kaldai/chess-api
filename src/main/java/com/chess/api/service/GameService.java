package com.chess.api.service;

import com.chess.api.dto.GameCreateRequest;
import com.chess.api.dto.GameInviteDTO;
import com.chess.api.dto.GameInviteRequest;
import com.chess.api.dto.MoveDTO;
import com.chess.api.model.Game;
import com.chess.api.model.GameInvite;
import com.chess.api.model.Move;
import com.chess.api.model.Player;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface GameService {
  @Transactional
  Game createGame(GameCreateRequest request, Player creator);

  Game getGameById(Long id);

  @Transactional
  Game joinGame(Long gameId, Player player);

  @Transactional
  void resignGame(Long gameId, Player player);

  @Transactional
  void offerDraw(Long gameId, Player player);

  @Transactional
  void acceptDraw(Long gameId, Player player);

  @Transactional
  void pauseGame(Long gameId, Player player);

  @Transactional
  void resumeGame(Long gameId, Player player);

  @Transactional
  GameInvite invitePlayer(GameInviteRequest inviteRequest, Player sender);

  List<GameInviteDTO> getPlayerInvites(Player player);

  List<GameInviteDTO> getSentInvites(Player player);

  @Transactional
  Game acceptInvite(Long inviteId, Player player);

  @Transactional
  void rejectInvite(Long inviteId, Player player);

  @Transactional
  void cancelInvite(Long inviteId, Player player);

  List<Game> getActiveGames(Player player);

  List<Game> getPlayerGameHistory(Player player, int limit);

  List<Game> getAllGames();

  List<Game> findWaitingGames();

  @Transactional
  void saveGame(Game game);

  @Transactional
  void saveMove(Game game, Move move);

  List<MoveDTO> getGameMoves(Long gameId);
}