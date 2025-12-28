package com.chess.api.service;

import com.chess.api.model.Player;
import com.chess.api.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PlayerServiceTest {

  @Autowired
  private PlayerService playerService;

  @Autowired
  private PlayerRepository playerRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void shouldRegisterNewPlayer() {
    // Given
    String username = "test_" + System.currentTimeMillis();
    String email = username + "@example.com";
    String password = "password123";

    // When
    Player player = playerService.registerPlayer(username, email, password);

    // Then
    assertThat(player).isNotNull();
    assertThat(player.getId()).isNotNull();
    assertThat(player.getUsername()).isEqualTo(username);
    assertThat(player.getEmail()).isEqualTo(email);
    assertThat(passwordEncoder.matches(password, player.getPassword())).isTrue();
    assertThat(player.getClassicalRating()).isEqualTo(1200);
    assertThat(player.getRapidRating()).isEqualTo(1200);
    assertThat(player.getBlitzRating()).isEqualTo(1200);
  }

  @Test
  void shouldNotRegisterDuplicateUsername() {
    // Given
    String username = "duplicate_" + System.currentTimeMillis();
    String email1 = username + "@example.com";
    String email2 = username + "2@example.com";

    playerService.registerPlayer(username, email1, "password123");

    // When & Then
    assertThatThrownBy(() ->
        playerService.registerPlayer(username, email2, "password456"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Имя пользователя уже занято");
  }

  @Test
  void shouldNotRegisterDuplicateEmail() {
    // Given
    String username1 = "user1_" + System.currentTimeMillis();
    String username2 = "user2_" + System.currentTimeMillis();
    String email = "same@example.com";

    playerService.registerPlayer(username1, email, "password123");

    // When & Then
    assertThatThrownBy(() ->
        playerService.registerPlayer(username2, email, "password456"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Email уже используется");
  }

  @Test
  void shouldGetPlayerByUsername() {
    // Given
    String username = "getuser_" + System.currentTimeMillis();
    String email = username + "@example.com";

    Player saved = playerService.registerPlayer(username, email, "password123");

    // When
    Player found = playerService.getPlayerByUsername(username);

    // Then
    assertThat(found).isNotNull();
    assertThat(found.getId()).isEqualTo(saved.getId());
    assertThat(found.getUsername()).isEqualTo(username);
  }
}