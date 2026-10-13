# Full Stack Interview Prep — React + Spring Boot

Aplikacja do zarządzania zadaniami (Task Manager) zbudowana specjalnie pod
przygotowanie do rozmowy rekrutacyjnej na stanowisko **Full Stack Developer
(Java / Spring + React)**. Każdy element projektu odpowiada tematowi, który
często pojawia się na rozmowach — w kodzie znajdziesz komentarze wskazujące
te tematy.

## Uruchomienie

### Backend (port 8080)

Wymagany JDK 21 (zainstalowany w `~/.jdks/jdk-21.0.12.1+1`).

```bash
export JAVA_HOME=$(ls -d ~/.jdks/jdk-21*/Contents/Home)
cd backend
./mvnw spring-boot:run
```

Testy:

```bash
./mvnw test
```

### Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Otwórz http://localhost:5173 — konto demo: **demo / demo123**.

Dodatkowe narzędzia:
- H2 console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:interviewdb`, user `sa`)
- Actuator health: http://localhost:8080/actuator/health

## Tematy rekrutacyjne pokryte w projekcie

### Spring Core / Boot
| Temat | Gdzie w kodzie |
|---|---|
| IoC / DI, constructor injection (dlaczego nie `@Autowired` na polu?) | `TaskService`, `AuthService` |
| Cykliczne zależności beanów i jak je rozbić | komentarz w `security/JpaUserDetailsService` |
| `@Configuration`, `@Bean`, profile (`@Profile("!test")`) | `config/DataInitializer`, `SecurityConfig` |
| Externalizacja konfiguracji (`@Value`, `application.properties`) | `security/JwtService` |
| Actuator (health, metrics) | `application.properties` |

### Spring Data JPA / Hibernate
| Temat | Gdzie w kodzie |
|---|---|
| Encje, `@GeneratedValue`, `@Enumerated`, lifecycle callbacks | `user/User`, `task/Task` |
| Relacje: `@ManyToOne(LAZY)`, `@ManyToMany` + join table | `task/Task`, `tag/Tag` |
| Problem N+1 i `@EntityGraph` | `task/TaskRepository` |
| Derived queries vs `@Query` (JPQL), projekcje DTO | `task/TaskRepository` (`TaskStats`) |
| Paginacja i sortowanie (`Pageable`, `Page`) | `TaskRepository`, `TaskController` |
| `@Transactional` (readOnly, dirty checking — brak `save()` w update!) | `task/TaskService` |
| Optimistic locking (`@Version`) | `task/Task`, handler 409 w `GlobalExceptionHandler` |

### REST API / Spring Web
| Temat | Gdzie w kodzie |
|---|---|
| Konwencje REST: verbs, kody (201 + Location, 204) | `task/TaskController` |
| DTO vs encja (dlaczego nie zwracać encji?) | `task/dto/*` (rekordy Javy) |
| Bean Validation (`@Valid`, `@NotBlank`, `@FutureOrPresent`...) | `TaskRequest`, `RegisterRequest` |
| Globalna obsługa błędów (`@RestControllerAdvice`) | `exception/GlobalExceptionHandler` |
| CORS | `SecurityConfig.corsConfigurationSource()` |

### Spring Security + JWT
| Temat | Gdzie w kodzie |
|---|---|
| Stateless auth, `SecurityFilterChain`, kolejność filtrów | `security/SecurityConfig` |
| Dlaczego można wyłączyć CSRF przy tokenach | komentarz w `SecurityConfig` |
| Własny filtr JWT (`OncePerRequestFilter`) | `security/JwtAuthFilter` |
| Generowanie / walidacja JWT (claims, podpis HMAC) | `security/JwtService` |
| `UserDetailsService` z bazy, BCrypt | `security/JpaUserDetailsService` |
| Autoryzacja na poziomie zasobu (ownership check) | `TaskService.getOwnedTask` |

### Testowanie (15 testów)
| Rodzaj | Klasa |
|---|---|
| Unit test (Mockito, `@Mock`, `ArgumentCaptor`, AAA) | `TaskServiceTest` |
| Slice test JPA (`@DataJpaTest`, `TestEntityManager`) | `TaskRepositoryTest` |
| Slice test web (`@WebMvcTest`, MockMvc, `@WithMockUser`) | `TaskControllerTest` |
| Pełny test integracyjny (register → login → JWT → endpoint) | `AuthIntegrationTest` |

### React + TypeScript
| Temat | Gdzie w kodzie |
|---|---|
| Hooki: `useState`, `useEffect` (+ cleanup), `useMemo`, `useCallback`, `useRef` | cały frontend |
| Custom hooki (debounce, data fetching + race conditions) | `hooks/useDebounce`, `hooks/useApi` |
| Context API (auth) — vs Redux, prop drilling | `auth/AuthContext` |
| React Router: nested routes, route guard, `useParams` | `App.tsx`, `components/ProtectedRoute` |
| Formularze kontrolowane + walidacja (klient + serwer 400→pola) | `pages/LoginPage`, `pages/TaskFormPage` |
| Error Boundary (dlaczego musi być klasą?) | `components/ErrorBoundary` |
| Listy z kluczami, paginacja server-side, filtrowanie | `pages/TasksPage` |
| Typowany klient API, obsługa JWT | `api/client.ts`, `api/types.ts` |

## Pytania, które warto sobie zadać przy każdym pliku

1. **Dlaczego `FetchType.LAZY` dla `@ManyToOne`?** Jaki jest domyślny fetch type i czym grozi EAGER?

   **Odpowiedź:** Domyślnie `*ToOne` = EAGER, `*ToMany` = LAZY. EAGER zawsze dociąga relację
   (JOIN lub dodatkowy SELECT), nawet gdy niepotrzebna — grozi problemem N+1 i wciąganiem
   dużego grafu obiektów; nie da się go wyłączyć per zapytanie. LAZY można selektywnie
   dociągnąć (`JOIN FETCH` / `@EntityGraph`). Pułapka LAZY: `LazyInitializationException`
   przy dostępie poza transakcją — dlatego mapujemy na DTO w serwisie. (szczegóły: `task/Task`)

2. **Co się stanie, gdy w `TaskService.update` nie wywołamy `save()`?** (dirty checking)

   **Odpowiedź:** Nic złego — zmiany i tak trafią do bazy. Encja pobrana w transakcji jest
   *managed*; przy commicie Hibernate porównuje jej stan ze snapshotem i sam generuje UPDATE.
   `save()` na encji managed to w praktyce no-op. Warunek: metoda musi być `@Transactional`
   (poza transakcją encja jest *detached* i zmiany przepadają). (szczegóły: `task/TaskService`)

3. **Jak działa dirty checking i kiedy Hibernate flushuje zmiany?**

   **Odpowiedź:** Persistence context trzyma snapshot pól każdej encji z momentu odczytu.
   Flush (porównanie + wysłanie SQL) następuje: przy commicie transakcji, przed wykonaniem
   zapytania JPQL/HQL dotykającego tych tabel (żeby zapytanie widziało aktualne dane) oraz
   przy jawnym `flush()`. Przy `readOnly=true` dirty checking jest wyłączony (FlushMode.MANUAL).

4. **Czym różni się `@WebMvcTest` od `@SpringBootTest`?** Po co slice testy?

   **Odpowiedź:** `@SpringBootTest` podnosi CAŁY kontekst aplikacji (wolny, ale testuje realną
   integrację — patrz `AuthIntegrationTest`). `@WebMvcTest` to *slice test*: ładuje tylko warstwę
   MVC (kontroler, konwersja JSON, walidacja, security), a zależności mockujemy (`@MockBean`) —
   szybki start, izolacja warstwy webowej. Analogicznie `@DataJpaTest` ładuje tylko JPA
   (repozytoria + wbudowana baza, rollback po każdym teście). Slice testy = szybsze buildy
   i precyzyjna lokalizacja błędu.

5. **Gdzie bezpieczniej trzymać JWT: localStorage czy httpOnly cookie?** Jakie są trade-offy (XSS vs CSRF)?

   **Odpowiedź:** localStorage: prosty, odporny na CSRF (token nie jest wysyłany automatycznie),
   ale podatny na XSS — wstrzyknięty skrypt odczyta token. httpOnly cookie: niedostępne dla JS
   (odporne na kradzież przez XSS), ale wysyłane automatycznie, więc wymaga ochrony CSRF
   (SameSite, token CSRF). Produkcyjnie często: krótki access token w pamięci + refresh token
   w httpOnly cookie. W tym projekcie dla prostoty: localStorage. (szczegóły: `auth/AuthContext`)

6. **Dlaczego `useApi` używa licznika requestów?** (race conditions w fetchowaniu)

   **Odpowiedź:** Odpowiedzi mogą przyjść w innej kolejności niż żądania (szybka zmiana filtra:
   odpowiedź A przychodzi po B i nadpisałaby świeższe dane). Każde wywołanie dostaje `id`
   z licznika w `useRef`; wynik jest użyty tylko, gdy `id` to wciąż najnowszy request — stale
   odpowiedzi są ignorowane. Alternatywa: `AbortController` + cleanup w `useEffect`, który
   faktycznie anuluje żądanie. (szczegóły: `hooks/useApi`)

7. **Co by się zmieniło przy przejściu z H2 na PostgreSQL?** (migracje — Flyway/Liquibase, `ddl-auto`)

   **Odpowiedź:** Zmiana zależności (driver `postgresql`) i `spring.datasource.*` (URL, user,
   hasło). Kluczowe: wyłączyć `ddl-auto=create/update` (na produkcji max `validate`) i wprowadzić
   wersjonowane migracje — Flyway (SQL w `db/migration/V1__init.sql`) lub Liquibase. Do testów
   integracyjnych zamiast H2 lepiej Testcontainers (prawdziwy Postgres w Dockerze — H2 nie
   wychwyci różnic dialektów). Dane startowe (`DataInitializer`) też przenosimy do migracji.

8. **Jak skalować tę aplikację?** (stateless JWT ułatwia horizontal scaling)

   **Odpowiedź:** Backend jest stateless (brak sesji HTTP — cały stan uwierzytelnienia niesie
   JWT), więc można uruchomić N instancji za load balancerem bez sticky sessions. Dalej:
   wspólna baza (connection pool, read repliki), cache (Redis/Caffeine) na częste odczyty,
   paginacja już jest server-side. Wąskie gardło sekretu JWT: przy wielu usługach lepszy
   podpis asymetryczny (RS256 — tylko auth-serwis ma klucz prywatny). Frontend to statyczne
   pliki — serwujemy z CDN.

## Endpointy API

| Metoda | Ścieżka | Opis |
|---|---|---|
| POST | `/api/auth/register` | rejestracja, zwraca JWT |
| POST | `/api/auth/login` | logowanie, zwraca JWT |
| GET | `/api/tasks?page=0&size=5&sort=createdAt,desc&status=TODO&q=spring` | lista z paginacją/filtrem/szukaniem |
| GET | `/api/tasks/{id}` | szczegóły zadania |
| GET | `/api/tasks/stats` | agregacja po statusie (JPQL GROUP BY) |
| POST | `/api/tasks` | utworzenie (201 + Location) |
| PUT | `/api/tasks/{id}` | aktualizacja |
| DELETE | `/api/tasks/{id}` | usunięcie (204) |

Wszystkie `/api/tasks/**` wymagają nagłówka `Authorization: Bearer <token>`.
