package com.interview.backend.task.dto;

import com.interview.backend.tag.Tag;
import com.interview.backend.task.Task;
import com.interview.backend.task.TaskPriority;
import com.interview.backend.task.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entities are never exposed directly from the API - DTOs decouple
 * the persistence model from the API contract (classic interview topic).
 */
public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        String ownerUsername,
        Set<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getOwner().getUsername(),
                task.getTags().stream().map(Tag::getName).collect(Collectors.toSet()),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
