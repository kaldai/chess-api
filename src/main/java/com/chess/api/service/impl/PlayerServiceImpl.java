package com.chess.api.service.impl;

import com.chess.api.model.Player;
import com.chess.api.repository.PlayerRepository;
import com.chess.api.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerServiceImpl implements PlayerService, UserDetailsService {

  private final PlayerRepository playerRepository;

  private PasswordEncoder passwordEncoder;

  @Override
  @Autowired
  public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Player player = playerRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("Игрок не найден: " + username));

    return new User(
        player.getUsername(),
        player.getPassword(),
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
    );
  }

  @Override
  @Transactional
  public Player registerPlayer(String username, String email, String password) {
    if (playerRepository.existsByUsername(username)) {
      throw new RuntimeException("Имя пользователя уже занято");
    }

    if (playerRepository.existsByEmail(email)) {
      throw new RuntimeException("Email уже используется");
    }

    Player player = new Player();
    player.setUsername(username);
    player.setEmail(email);
    player.setPassword(passwordEncoder.encode(password));
    player.setClassicalRating(1200);
    player.setRapidRating(1200);
    player.setBlitzRating(1200);
    player.setCreatedAt(LocalDateTime.now());

    log.info("Новый игрок зарегистрирован: {}", username);
    return playerRepository.save(player);
  }


  @Override
  @Transactional
  public Player loginPlayer(String username) {
    Player player = getPlayerByUsername(username);
    player.setLastLoginAt(LocalDateTime.now());
    return playerRepository.save(player);
  }

  @Override
  public Player getPlayerById(Long id) {
    return playerRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Игрок не найден"));
  }

  @Override
  public Player getPlayerByUsername(String username) {
    return playerRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("Игрок не найден: " + username));
  }

  @Override
  public List<Player> getAllPlayers() {
    return playerRepository.findAll();
  }

  @Override
  public List<Player> getTopPlayers(String gameType, int limit) {
    return switch (gameType.toUpperCase()) {
      case "CLASSICAL" -> playerRepository.findTopByClassicalRating(limit);
      case "RAPID" -> playerRepository.findTopByRapidRating(limit);
      case "BLITZ" -> playerRepository.findTopByBlitzRating(limit);
      default -> throw new IllegalArgumentException("Неверный тип игры: " + gameType);
    };
  }

  @Override
  @Transactional
  public Player updatePlayerStatistics(Player player) {
    return playerRepository.save(player);
  }

  @Override
  @Transactional
  public void updatePlayerRating(Long playerId, String gameType, int ratingChange) {
    Player player = getPlayerById(playerId);

    switch (gameType.toUpperCase()) {
      case "CLASSICAL" ->
          player.setClassicalRating(Math.max(100, player.getClassicalRating() + ratingChange));
      case "RAPID" ->
          player.setRapidRating(Math.max(100, player.getRapidRating() + ratingChange));
      case "BLITZ" ->
          player.setBlitzRating(Math.max(100, player.getBlitzRating() + ratingChange));
    }

    log.info("Обновлен рейтинг игрока {} ({}): изменение {}",
        player.getUsername(), gameType, ratingChange);
    playerRepository.save(player);
  }
}