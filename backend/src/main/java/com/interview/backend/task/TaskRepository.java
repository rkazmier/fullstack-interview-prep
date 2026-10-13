package com.interview.backend.task;

import com.interview.backend.task.dto.TaskStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Interview topics demonstrated here:
 * - derived query methods vs @Query (JPQL)
 * - pagination with Pageable / Page
 * - @EntityGraph to avoid the N+1 select problem
 * - DTO projection with a constructor expression
 *
 * ODPOWIEDŹ (problem N+1 i @EntityGraph):
 * N+1 = 1 zapytanie o listę tasków + N dodatkowych zapytań o lazy relacje
 * (owner, tags) przy dostępie do nich dla każdego z N wierszy. Rozwiązania:
 * - JOIN FETCH w JPQL (ale z paginacją bywa problematyczny dla kolekcji -
 *   Hibernate potrafi paginować w pamięci!),
 * - @EntityGraph(attributePaths=...) - deklaratywnie dociąga wskazane relacje
 *   (LEFT JOIN) i działa też z metodami derived,
 * - @BatchSize / default_batch_fetch_size - dociąganie paczkami (IN (...)).
 *
 * ODPOWIEDŹ (derived queries vs @Query):
 * Derived (findByOwnerUsernameAndStatus) - nazwa metody parsowana na zapytanie;
 * szybkie w pisaniu, ale długie nazwy robią się nieczytelne. @Query (JPQL) -
 * pełna kontrola, złożone warunki (LIKE po dwóch polach), agregacje.
 * JPQL operuje na encjach, nie tabelach; weryfikowany przy starcie aplikacji.
 *
 * ODPOWIEDŹ (projekcja DTO):
 * "SELECT new ...TaskStats(t.status, COUNT(t))" tworzy DTO bezpośrednio
 * w zapytaniu - baza zwraca tylko potrzebne kolumny, bez ładowania całych
 * encji do persistence contextu (szybsze, mniej pamięci, brak dirty checkingu).
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"owner", "tags"})
    Page<Task> findByOwnerUsername(String username, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "tags"})
    Page<Task> findByOwnerUsernameAndStatus(String username, TaskStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "tags"})
    @Query("""
            SELECT t FROM Task t
            WHERE t.owner.username = :username
              AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Task> search(@Param("username") String username,
                      @Param("query") String query,
                      Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "tags"})
    Optional<Task> findWithDetailsById(Long id);

    @Query("""
            SELECT new com.interview.backend.task.dto.TaskStats(t.status, COUNT(t))
            FROM Task t
            WHERE t.owner.username = :username
            GROUP BY t.status
            """)
    List<TaskStats> countByStatusForOwner(@Param("username") String username);
}
