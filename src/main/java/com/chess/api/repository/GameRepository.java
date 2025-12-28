package com.chess.api.repository;

import com.chess.api.model.Game;
import com.chess.api.model.Player;
import com.chess.api.model.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

  List<Game> findByStatus(GameStatus status);

  @Query("SELECT g FROM Game g WHERE g.status = :status AND " +
      "(g.whitePlayer = :player OR g.blackPlayer = :player)")
  List<Game> findActiveGamesByPlayer(@Param("player") Player player,
                                     @Param("status") GameStatus status);

  @Query("SELECT g FROM Game g WHERE g.status = 'WAITING' AND " +
      "(g.blackPlayer IS NULL OR g.whitePlayer IS NULL) " +
      "ORDER BY g.createdAt ASC")
  List<Game> findWaitingGames();

  @Query("SELECT g FROM Game g WHERE (g.whitePlayer = :player OR g.blackPlayer = :player) " +
      "AND g.status IN ('WHITE_WON', 'BLACK_WON', 'DRAW') " +
      "ORDER BY g.finishedAt DESC")
  List<Game> findFinishedGamesByPlayer(@Param("player") Player player);
}