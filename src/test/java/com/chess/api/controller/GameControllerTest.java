package com.chess.api.controller;

import com.chess.api.dto.GameCreateRequest;
import com.chess.api.dto.RegisterRequest;
import com.chess.api.model.enums.GameType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private String authToken;
  private String username;
  private Long playerId;

  @BeforeEach
  void setUp() throws Exception {
    // Регистрируем и логиним пользователя
    username = "testuser_" + System.currentTimeMillis();

    RegisterRequest registerRequest = new RegisterRequest(
        username,
        username + "@example.com",
        "password123"
    );

    MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)))
        .andReturn();

    JsonNode registerResponse = objectMapper.readTree(registerResult.getResponse().getContentAsString());
    authToken = registerResponse.path("data").path("token").asText();
    playerId = registerResponse.path("data").path("player").path("id").asLong();
  }

  @Test
  void testCreateGame() throws Exception {
    GameCreateRequest gameRequest = new GameCreateRequest(GameType.BLITZ, null, null, true, null);

    mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(gameRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.gameType").value("BLITZ"))
        .andExpect(jsonPath("$.data.whitePlayer.username").value(username));
  }

  @Test
  void testGetAllGames() throws Exception {
    mockMvc.perform(get("/api/games")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void testGetGameById() throws Exception {
    // Сначала создаем игру
    GameCreateRequest gameRequest = new GameCreateRequest(GameType.BLITZ, null, null, true, null);

    MvcResult createResult = mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(gameRequest)))
        .andReturn();

    JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
    Long gameId = createResponse.path("data").path("id").asLong();

    // Получаем игру по ID
    mockMvc.perform(get("/api/games/" + gameId)
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(gameId));
  }

  @Test
  void testGetWaitingGames() throws Exception {
    mockMvc.perform(get("/api/games/waiting")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void testGetActiveGames() throws Exception {
    mockMvc.perform(get("/api/games/active")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void testGetGameHistory() throws Exception {
    mockMvc.perform(get("/api/games/history?limit=5")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }
}