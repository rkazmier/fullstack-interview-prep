package com.interview.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Interview topics demonstrated here:
 * - stateless security (no HTTP session) with a custom JWT filter
 *   placed before UsernamePasswordAuthenticationFilter
 * - why CSRF protection can be disabled for stateless token-based APIs
 * - CORS configuration for the React dev server
 * - BCrypt password hashing (adaptive, salted - never store plain text!)
 *
 * ODPOWIEDŹ (dlaczego można wyłączyć CSRF?):
 * Atak CSRF polega na tym, że przeglądarka AUTOMATYCZNIE dołącza credentials
 * (cookie sesyjne) do żądania wysłanego ze złośliwej strony. Nasz API jest
 * stateless: nie ma sesji ani cookies, a token JWT trzeba jawnie dodać
 * w nagłówku Authorization - złośliwa strona nie ma do niego dostępu
 * (Same-Origin Policy), więc wektor ataku CSRF nie istnieje.
 * UWAGA: gdybyśmy trzymali JWT w cookie (httpOnly), CSRF znowu by groził
 * i trzeba by włączyć ochronę (SameSite, token CSRF).
 *
 * ODPOWIEDŹ (CORS vs CSRF - często mylone):
 * CORS to mechanizm PRZEGLĄDARKI ograniczający odczyt odpowiedzi z innego
 * originu; konfigurujemy go, by frontend z :5173 mógł wołać API na :8080
 * (preflight OPTIONS + nagłówki Access-Control-*). CSRF to atak na zapis.
 * CORS nie chroni przed CSRF.
 *
 * ODPOWIEDŹ (dlaczego BCrypt?):
 * BCrypt jest wolny z założenia (konfigurowalny work factor/strength) i sam
 * generuje sól zapisaną w hashu - utrudnia brute-force i rainbow tables.
 * SHA-256/MD5 są za szybkie do haseł. matches() porównuje bez dekodowania
 * (funkcja jest jednokierunkowa).
 *
 * ODPOWIEDŹ (kolejność filtrów):
 * addFilterBefore umieszcza JwtAuthFilter przed UsernamePasswordAuthenticationFilter,
 * żeby SecurityContext był wypełniony ZANIM zadziała autoryzacja
 * (authorizeHttpRequests). Przy STATELESS Spring nie tworzy HttpSession
 * i nie zapisuje kontekstu między żądaniami - każdy request musi przynieść token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/h2-console/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // for H2 console
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
