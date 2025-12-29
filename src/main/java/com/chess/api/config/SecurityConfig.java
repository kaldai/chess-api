package com.chess.api.config;

import com.chess.api.security.JwtAuthenticationFilter;
import com.chess.api.security.JwtUtils;
import com.chess.api.service.impl.PlayerServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtUtils jwtUtils;
  private final PlayerServiceImpl playerService;

  // Убираем @Autowired с полей, используем конструктор
  public SecurityConfig(JwtUtils jwtUtils, PlayerServiceImpl playerService) {
    this.jwtUtils = jwtUtils;
    this.playerService = playerService;
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
    filter.setJwtUtils(jwtUtils);
    filter.setPlayerService(playerService);
    return filter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/players/test").permitAll()  // Добавим тестовый endpoint
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/h2-console/**").permitAll()
            .requestMatchers("/chess-websocket/**").permitAll()
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
        .headers(headers -> headers
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
        );

    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }
}