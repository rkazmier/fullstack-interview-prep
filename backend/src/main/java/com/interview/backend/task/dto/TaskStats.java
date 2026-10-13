package com.interview.backend.task.dto;

import com.interview.backend.task.TaskStatus;

/**
 * Interface-based projection is also possible; here we use a record
 * constructor expression in JPQL (see TaskRepository.countByStatusForOwner).
 */
public record TaskStats(TaskStatus status, long count) {
}
