package com.interview.backend.task;

import com.interview.backend.tag.Tag;
import com.interview.backend.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Interview topics demonstrated here:
 * - @ManyToOne with FetchType.LAZY (default for *ToOne is EAGER - common interview question!)
 * - @ManyToMany with a join table
 * - optimistic locking with @Version
 * - lifecycle callbacks @PrePersist / @PreUpdate
 *
 * ODPOWIEDŹ (fetch types - dlaczego LAZY dla @ManyToOne?):
 * Domyślne wartości: @ManyToOne i @OneToOne = EAGER, @OneToMany i @ManyToMany = LAZY.
 * EAGER jest groźny, bo:
 * - każdy odczyt Taska dociąga ownera (JOIN albo dodatkowy SELECT), nawet gdy
 *   nie jest potrzebny; przy łańcuchu relacji EAGER można wciągnąć pół bazy,
 * - w zapytaniach listowych łatwo o problem N+1 (1 zapytanie o taski + N o ownerów),
 * - EAGER-a nie da się wyłączyć per zapytanie, a LAZY można selektywnie
 *   dociągnąć przez JOIN FETCH / @EntityGraph (patrz TaskRepository).
 * Pułapka LAZY: dostęp do niezainicjalizowanego proxy poza transakcją rzuca
 * LazyInitializationException - dlatego mapujemy na DTO wewnątrz serwisu.
 *
 * ODPOWIEDŹ (optimistic locking przez @Version):
 * Hibernate dodaje do UPDATE warunek "WHERE id=? AND version=?" i podbija
 * wersję. Gdy dwóch użytkowników edytuje ten sam rekord, drugi zapis trafia
 * na zmienioną wersję -> ObjectOptimisticLockingFailureException -> u nas 409
 * Conflict (GlobalExceptionHandler). Nie blokuje wierszy w bazie (w odróżnieniu
 * od pessimistic lockingu, czyli SELECT ... FOR UPDATE) - dobry wybór, gdy
 * konflikty są rzadkie.
 */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;

    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToMany
    @JoinTable(
            name = "task_tags",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    protected Task() {
    }

    public Task(String title, String description, TaskStatus status, TaskPriority priority,
                LocalDate dueDate, User owner) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public TaskPriority getPriority() { return priority; }
    public LocalDate getDueDate() { return dueDate; }
    public User getOwner() { return owner; }
    public Set<Tag> getTags() { return tags; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setTags(Set<Tag> tags) { this.tags = tags; }
}
