package com.chess.api.controller;

import com.chess.api.dto.ApiResponse;
import com.chess.api.model.Player;
import com.chess.api.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

  private final PlayerService playerService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<Player>>> getAllPlayers() {
    List<Player> players = playerService.getAllPlayers();
    return ResponseEntity.ok(ApiResponse.success(players));
  }

  @GetMapping("/top")
  public ResponseEntity<ApiResponse<List<Player>>> getTopPlayers(
      @RequestParam(defaultValue = "CLASSICAL") String gameType,
      @RequestParam(defaultValue = "10") int limit) {

    List<Player> players = playerService.getTopPlayers(gameType, limit);
    return ResponseEntity.ok(ApiResponse.success(players));
  }

  @GetMapping("/test")
  public ResponseEntity<ApiResponse<Player>> createTestPlayer() {
    Player player = new Player();
    player.setUsername("test_" + System.currentTimeMillis());
    player.setEmail(player.getUsername() + "@example.com");
    player.setPassword("password");
    player.setClassicalRating(1200);
    player.setRapidRating(1200);
    player.setBlitzRating(1200);

    Player savedPlayer = playerService.updatePlayerStatistics(player);
    return ResponseEntity.ok(ApiResponse.success(savedPlayer));
  }
}