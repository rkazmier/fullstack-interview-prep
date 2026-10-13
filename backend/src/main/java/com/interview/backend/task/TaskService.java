package com.interview.backend.task;

import com.interview.backend.exception.ResourceNotFoundException;
import com.interview.backend.tag.Tag;
import com.interview.backend.tag.TagRepository;
import com.interview.backend.task.dto.TaskRequest;
import com.interview.backend.task.dto.TaskResponse;
import com.interview.backend.task.dto.TaskStats;
import com.interview.backend.user.User;
import com.interview.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interview topics demonstrated here:
 * - constructor injection (preferred over field @Autowired - why? testability, immutability)
 * - @Transactional semantics: readOnly, rollback on RuntimeException
 * - mapping entity <-> DTO in the service layer
 * - authorization check at the service level (ownership)
 *
 * ODPOWIEDŹ (dlaczego constructor injection, a nie @Autowired na polu?):
 * - pola mogą być final -> niemutowalność i bezpieczeństwo wątkowe,
 * - łatwe testy jednostkowe: new TaskService(mock, mock, mock) bez Springa
 *   i bez refleksji (ReflectionTestUtils),
 * - fail-fast: brak zależności wykrywany przy starcie kontekstu,
 * - widać "zapach" zbyt wielu zależności (rozdęty konstruktor = klasa robi
 *   za dużo), pola z @Autowired to ukrywają,
 * - od Spring 4.3 przy jednym konstruktorze adnotacja @Autowired jest zbędna.
 *
 * ODPOWIEDŹ (@Transactional):
 * - readOnly=true na klasie: podpowiedź dla Hibernate (brak dirty checkingu,
 *   FlushMode.MANUAL) i sterownika/bazy -> mniejszy narzut dla odczytów,
 * - metody zapisu nadpisują to własnym @Transactional (readOnly=false),
 * - domyślny rollback: tylko dla RuntimeException i Error; dla wyjątków
 *   checked trzeba dodać rollbackFor,
 * - @Transactional działa przez proxy Springa -> NIE zadziała na metodzie
 *   prywatnej ani przy wywołaniu this.metoda() z tej samej klasy
 *   (self-invocation omija proxy) - częsta pułapka na rozmowach!
 */
@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository,
                       TagRepository tagRepository,
                       UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    public Page<TaskResponse> findAll(String username, TaskStatus status, String query, Pageable pageable) {
        Page<Task> page;
        if (query != null && !query.isBlank()) {
            page = taskRepository.search(username, query.trim(), pageable);
        } else if (status != null) {
            page = taskRepository.findByOwnerUsernameAndStatus(username, status, pageable);
        } else {
            page = taskRepository.findByOwnerUsername(username, pageable);
        }
        return page.map(TaskResponse::from);
    }

    public TaskResponse findById(Long id, String username) {
        Task task = getOwnedTask(id, username);
        return TaskResponse.from(task);
    }

    public List<TaskStats> stats(String username) {
        return taskRepository.countByStatusForOwner(username);
    }

    @Transactional
    public TaskResponse create(TaskRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        Task task = new Task(request.title(), request.description(), request.status(),
                request.priority(), request.dueDate(), owner);
        task.setTags(resolveTags(request.tags()));

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request, String username) {
        Task task = getOwnedTask(id, username);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setTags(resolveTags(request.tags()));

        // no explicit save() needed: the entity is managed and dirty checking
        // flushes changes at transaction commit (classic interview question)
        //
        // ODPOWIEDŹ (jak działa dirty checking?):
        // Encja pobrana w transakcji jest "managed" - persistence context
        // (sesja Hibernate) trzyma snapshot jej pól z momentu odczytu.
        // Przy flushu (commit transakcji, przed zapytaniem JPQL lub przy
        // jawnym flush()) Hibernate porównuje aktualny stan ze snapshotem
        // i generuje UPDATE tylko dla zmienionych encji. Dlatego same settery
        // wystarczą. save() na encji managed to w praktyce no-op (merge).
        // Gdyby metoda NIE była @Transactional, encja byłaby detached
        // i zmiany by przepadły - stąd @Transactional na update().
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id, String username) {
        Task task = getOwnedTask(id, username);
        taskRepository.delete(task);
    }

    private Task getOwnedTask(Long id, String username) {
        Task task = taskRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        if (!task.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("You are not the owner of this task");
        }
        return task;
    }

    private Set<Tag> resolveTags(Set<String> names) {
        Set<Tag> tags = new HashSet<>();
        if (names == null) {
            return tags;
        }
        for (String name : names) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Tag tag = tagRepository.findByNameIgnoreCase(trimmed)
                    .orElseGet(() -> tagRepository.save(new Tag(trimmed)));
            tags.add(tag);
        }
        return tags;
    }
}
