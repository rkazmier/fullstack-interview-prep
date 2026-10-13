package com.interview.backend.security;

import com.interview.backend.user.User;
import com.interview.backend.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads users from the database for Spring Security.
 *
 * Kept as a separate class (instead of AuthService implementing
 * UserDetailsService) to avoid a circular bean dependency:
 * AuthService -> PasswordEncoder (SecurityConfig) -> JwtAuthFilter -> UserDetailsService.
 * Circular dependencies are a classic Spring interview topic!
 *
 * ODPOWIEDŹ (cykliczne zależności w Springu):
 * Cykl A -> B -> A przy constructor injection kończy się błędem startu
 * (BeanCurrentlyInCreationException) - Spring nie może utworzyć żadnego
 * z beanów, bo każdy czeka na drugi. Od Springa Boot 2.6 cykle są domyślnie
 * zabronione nawet dla setter injection. Sposoby rozwiązania (od najlepszego):
 * 1. Przeprojektowanie - wydzielenie wspólnej logiki do trzeciego beana
 *    (dokładnie to zrobiono tutaj: osobny JpaUserDetailsService zamiast
 *    AuthService implementującego UserDetailsService),
 * 2. @Lazy na jednej z zależności - wstrzykiwane jest leniwe proxy,
 *    inicjalizowane dopiero przy pierwszym użyciu,
 * 3. setter/field injection zamiast konstruktora (+ spring.main.allow-circular-references),
 * 4. ObjectProvider<T> / ApplicationContext.getBean() na żądanie.
 * Opcje 2-4 to obejścia; cykl prawie zawsze sygnalizuje zły podział
 * odpowiedzialności.
 */
@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
