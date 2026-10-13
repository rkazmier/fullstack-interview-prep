package com.interview.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request: extracts the Bearer token, validates it and
 * populates the SecurityContext. Extends OncePerRequestFilter to avoid
 * double execution on forwards/includes (interview question!).
 *
 * ODPOWIEDŹ (dlaczego OncePerRequestFilter?):
 * Zwykły jakarta Filter może zostać wywołany kilka razy dla jednego żądania
 * HTTP — np. przy forwardzie (RequestDispatcher.forward), include, albo przy
 * obsłudze błędów (dispatch typu ERROR) i async. OncePerRequestFilter
 * gwarantuje jedno wykonanie doFilterInternal per żądanie (flaga ustawiana
 * jako atrybut requestu), więc nie parsujemy/walidujemy tokenu wielokrotnie.
 *
 * ODPOWIEDŹ (przebieg uwierzytelnienia JWT):
 * 1. Odczyt nagłówka "Authorization: Bearer <token>".
 * 2. Walidacja podpisu i daty wygaśnięcia (JwtService).
 * 3. Sprawdzenie, czy SecurityContext jest pusty (ktoś mógł już uwierzytelnić).
 * 4. Załadowanie UserDetails z bazy i utworzenie Authentication
 *    (credentials = null, bo hasło nie jest już potrzebne).
 * 5. Umieszczenie Authentication w SecurityContextHolder (ThreadLocal) —
 *    od tego momentu żądanie jest "zalogowane" dla dalszych filtrów
 *    i kontrolerów (Principal). Brak/nieprawidłowy token NIE kończy żądania
 *    tutaj — dalej zdecyduje autoryzacja (401 z entry pointu).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwtService.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
