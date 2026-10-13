package com.interview.backend.task;

import com.interview.backend.task.dto.TaskStats;
import com.interview.backend.user.Role;
import com.interview.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest: slice test - only JPA components, embedded H2,
 * each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = entityManager.persist(new User("demo", "demo@example.com", "hash", Role.USER));
        User other = entityManager.persist(new User("other", "other@example.com", "hash", Role.USER));

        entityManager.persist(new Task("Learn Spring", "IoC and DI", TaskStatus.TODO, TaskPriority.HIGH, null, owner));
        entityManager.persist(new Task("Learn JPA", "Entities", TaskStatus.DONE, TaskPriority.MEDIUM, null, owner));
        entityManager.persist(new Task("Other user task", null, TaskStatus.TODO, TaskPriority.LOW, null, other));
        entityManager.flush();
    }

    @Test
    void findByOwnerUsername_returnsOnlyOwnedTasks() {
        Page<Task> page = taskRepository.findByOwnerUsername("demo", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .allSatisfy(task -> assertThat(task.getOwner().getUsername()).isEqualTo("demo"));
    }

    @Test
    void search_matchesTitleCaseInsensitive() {
        Page<Task> page = taskRepository.search("demo", "spring", PageRequest.of(0, 10));

        assertThat(page.getContent())
                .hasSize(1)
                .first()
                .extracting(Task::getTitle)
                .isEqualTo("Learn Spring");
    }

    @Test
    void countByStatusForOwner_groupsByStatus() {
        List<TaskStats> stats = taskRepository.countByStatusForOwner("demo");

        assertThat(stats)
                .containsExactlyInAnyOrder(
                        new TaskStats(TaskStatus.TODO, 1),
                        new TaskStats(TaskStatus.DONE, 1));
    }
}
