package com.interview.backend.task;

import com.interview.backend.exception.ResourceNotFoundException;
import com.interview.backend.tag.TagRepository;
import com.interview.backend.task.dto.TaskRequest;
import com.interview.backend.task.dto.TaskResponse;
import com.interview.backend.user.Role;
import com.interview.backend.user.User;
import com.interview.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test: no Spring context, dependencies mocked with Mockito.
 * Interview topics: mock vs stub, ArgumentCaptor, AAA pattern.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private final User owner = new User("demo", "demo@example.com", "hash", Role.USER);

    @Test
    void create_savesTaskForOwner() {
        // given
        TaskRequest request = new TaskRequest("Title", "Desc", TaskStatus.TODO,
                TaskPriority.HIGH, LocalDate.now().plusDays(1), Set.of());
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        TaskResponse response = taskService.create(request, "demo");

        // then
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner().getUsername()).isEqualTo("demo");
        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void findById_throwsWhenTaskDoesNotExist() {
        when(taskRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L, "demo"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findById_throwsWhenUserIsNotOwner() {
        Task task = new Task("T", null, TaskStatus.TODO, TaskPriority.LOW, null, owner);
        when(taskRepository.findWithDetailsById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.findById(1L, "someoneElse"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void delete_removesOwnedTask() {
        Task task = new Task("T", null, TaskStatus.TODO, TaskPriority.LOW, null, owner);
        when(taskRepository.findWithDetailsById(1L)).thenReturn(Optional.of(task));

        taskService.delete(1L, "demo");

        verify(taskRepository).delete(task);
    }
}
