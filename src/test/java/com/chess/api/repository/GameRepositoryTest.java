package com.chess.api.repository;

import com.chess.api.model.Game;
import com.chess.api.model.Player;
import com.chess.api.model.enums.GameStatus;
import com.chess.api.model.enums.GameType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class GameRepositoryTest {

  @Autowired
  private GameRepository gameRepository;

  @Autowired
  private PlayerRepository playerRepository;

  @Test
  void shouldSaveAndFindGame() {
    // Given
    Player whitePlayer = new Player();
    whitePlayer.setUsername("whitePlayer");
    whitePlayer.setEmail("white@example.com");
    whitePlayer.setPassword("password");
    playerRepository.save(whitePlayer);

    Player blackPlayer = new Player();
    blackPlayer.setUsername("blackPlayer");
    blackPlayer.setEmail("black@example.com");
    blackPlayer.setPassword("password");
    playerRepository.save(blackPlayer);

    Game game = new Game();
    game.setWhitePlayer(whitePlayer);
    game.setBlackPlayer(blackPlayer);
    game.setGameType(GameType.BLITZ);
    game.setStatus(GameStatus.ACTIVE);
    game.setTimeControl(180);
    game.setTimeIncrement(2);
    game.setWhiteTimeLeft(180000);
    game.setBlackTimeLeft(180000);
    game.setCreatedAt(LocalDateTime.now());

    // When
    Game savedGame = gameRepository.save(game);

    // Then
    assertThat(savedGame).isNotNull();
    assertThat(savedGame.getId()).isNotNull();
    assertThat(savedGame.getWhitePlayer()).isEqualTo(whitePlayer);
    assertThat(savedGame.getBlackPlayer()).isEqualTo(blackPlayer);
    assertThat(savedGame.getGameType()).isEqualTo(GameType.BLITZ);
    assertThat(savedGame.getStatus()).isEqualTo(GameStatus.ACTIVE);
  }

  @Test
  void shouldFindGamesByStatus() {
    // Given
    Player player = new Player();
    player.setUsername("testPlayer");
    player.setEmail("test@example.com");
    player.setPassword("password");
    playerRepository.save(player);

    Game activeGame = new Game();
    activeGame.setWhitePlayer(player);
    activeGame.setGameType(GameType.BLITZ);
    activeGame.setStatus(GameStatus.ACTIVE);
    gameRepository.save(activeGame);

    Game waitingGame = new Game();
    waitingGame.setWhitePlayer(player);
    waitingGame.setGameType(GameType.CLASSICAL);
    waitingGame.setStatus(GameStatus.WAITING);
    gameRepository.save(waitingGame);

    // When
    var activeGames = gameRepository.findByStatus(GameStatus.ACTIVE);
    var waitingGames = gameRepository.findByStatus(GameStatus.WAITING);

    // Then
    assertThat(activeGames).hasSize(1);
    assertThat(waitingGames).hasSize(1);
    assertThat(activeGames.get(0).getStatus()).isEqualTo(GameStatus.ACTIVE);
    assertThat(waitingGames.get(0).getStatus()).isEqualTo(GameStatus.WAITING);
  }
}