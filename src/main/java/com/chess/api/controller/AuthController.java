package com.chess.api.controller;

import com.chess.api.dto.ApiResponse;
import com.chess.api.dto.LoginRequest;
import com.chess.api.dto.RegisterRequest;
import com.chess.api.security.JwtUtils;
import com.chess.api.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final PlayerService playerService;
  private final JwtUtils jwtUtils;

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<Map<String, String>>> login(@Valid @RequestBody LoginRequest request) {
    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
      );

      SecurityContextHolder.getContext().setAuthentication(authentication);

      String jwt = jwtUtils.generateToken(request.getUsername());
      var player = playerService.loginPlayer(request.getUsername());

      Map<String, String> response = new HashMap<>();
      response.put("token", jwt);
      response.put("type", "Bearer");
      response.put("username", player.getUsername());
      response.put("id", player.getId().toString());

      return ResponseEntity.ok(ApiResponse.success("Успешный вход", response));
    } catch (Exception e) {
      log.error("Login error for user {}: {}", request.getUsername(), e.getMessage());
      return ResponseEntity.badRequest().body(ApiResponse.error("Неверное имя пользователя или пароль"));
    }
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
    try {
      var player = playerService.registerPlayer(
          request.getUsername(),
          request.getEmail(),
          request.getPassword()
      );

      // Генерируем токен для автоматического входа после регистрации
      String jwt = jwtUtils.generateToken(request.getUsername());

      Map<String, Object> response = new HashMap<>();
      response.put("player", player);
      response.put("token", jwt);
      response.put("type", "Bearer");

      return ResponseEntity.ok(ApiResponse.success("Регистрация успешна", response));
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }
}