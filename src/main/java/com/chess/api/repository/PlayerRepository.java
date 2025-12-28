package com.chess.api.repository;

import com.chess.api.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

  Optional<Player> findByUsername(String username);

  Optional<Player> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @Query("SELECT p FROM Player p ORDER BY p.classicalRating DESC")
  List<Player> findTopByClassicalRating(int limit);

  @Query("SELECT p FROM Player p ORDER BY p.rapidRating DESC")
  List<Player> findTopByRapidRating(int limit);

  @Query("SELECT p FROM Player p ORDER BY p.blitzRating DESC")
  List<Player> findTopByBlitzRating(int limit);
}