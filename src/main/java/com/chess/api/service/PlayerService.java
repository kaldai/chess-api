package com.chess.api.service;

import com.chess.api.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PlayerService {
  @Autowired
  void setPasswordEncoder(PasswordEncoder passwordEncoder);

  @Transactional
  UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

  @Transactional
  Player registerPlayer(String username, String email, String password);

  @Transactional
  Player loginPlayer(String username);

  Player getPlayerById(Long id);

  Player getPlayerByUsername(String username);

  List<Player> getAllPlayers();

  List<Player> getTopPlayers(String gameType, int limit);

  @Transactional
  Player updatePlayerStatistics(Player player);

  @Transactional
  void updatePlayerRating(Long playerId, String gameType, int ratingChange);
}