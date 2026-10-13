package com.interview.backend.task.dto;

import com.interview.backend.task.TaskPriority;
import com.interview.backend.task.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

/**
 * Java record as a DTO - immutable, concise, auto equals/hashCode/toString.
 * Bean Validation annotations are checked when the controller parameter
 * is annotated with @Valid.
 */
public record TaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @NotNull(message = "Status is required")
        TaskStatus status,

        @NotNull(message = "Priority is required")
        TaskPriority priority,

        @FutureOrPresent(message = "Due date cannot be in the past")
        LocalDate dueDate,

        Set<String> tags
) {
}
