package com.chess.api.service;

import com.chess.api.dto.MoveRequest;
import com.chess.api.model.Game;
import com.chess.api.model.Move;
import com.chess.api.model.Player;
import com.chess.api.exception.IllegalMoveException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChessGameService {
  @Transactional
  Move makeMove(Game game, Player player, MoveRequest moveRequest)
      throws IllegalMoveException;

  void startGameTimer(Game game);

  void stopGameTimer(Long gameId);

  Integer[] getTimeLeft(Long gameId);

  List<String> getLegalMoves(Game game, String square);
}