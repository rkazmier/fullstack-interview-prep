package com.interview.backend.task;

import com.interview.backend.task.dto.TaskRequest;
import com.interview.backend.task.dto.TaskResponse;
import com.interview.backend.task.dto.TaskStats;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

/**
 * Interview topics demonstrated here:
 * - REST conventions: nouns in paths, proper HTTP verbs and status codes
 *   (201 Created + Location header, 204 No Content)
 * - @Valid triggering Bean Validation on the request body
 * - Pageable resolved from query params (?page=0&size=10&sort=dueDate,asc)
 * - Principal injected from the SecurityContext (JWT filter)
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Page<TaskResponse> list(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false, name = "q") String query,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        return taskService.findAll(principal.getName(), status, query, pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id, Principal principal) {
        return taskService.findById(id, principal.getName());
    }

    @GetMapping("/stats")
    public List<TaskStats> stats(Principal principal) {
        return taskService.stats(principal.getName());
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request, Principal principal) {
        TaskResponse created = taskService.create(request, principal.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id,
                               @Valid @RequestBody TaskRequest request,
                               Principal principal) {
        return taskService.update(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) {
        taskService.delete(id, principal.getName());
    }
}
