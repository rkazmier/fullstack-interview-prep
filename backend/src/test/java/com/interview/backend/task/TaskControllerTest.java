package com.interview.backend.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.backend.security.JwtService;
import com.interview.backend.task.dto.TaskRequest;
import com.interview.backend.task.dto.TaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest: slice test for the web layer only - service is mocked,
 * MockMvc performs requests without a real HTTP server.
 */
@WebMvcTest(TaskController.class)
@Import({com.interview.backend.security.SecurityConfig.class,
        com.interview.backend.security.JwtAuthFilter.class})
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void get_returns401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "demo")
    void get_returnsTask() throws Exception {
        TaskResponse response = new TaskResponse(1L, "Title", "Desc", TaskStatus.TODO,
                TaskPriority.HIGH, null, "demo", Set.of(), Instant.now(), null);
        when(taskService.findById(eq(1L), eq("demo"))).thenReturn(response);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title"))
                .andExpect(jsonPath("$.ownerUsername").value("demo"));
    }

    @Test
    @WithMockUser(username = "demo")
    void create_returns201WithLocationHeader() throws Exception {
        TaskRequest request = new TaskRequest("New task", null, TaskStatus.TODO,
                TaskPriority.MEDIUM, null, Set.of());
        TaskResponse response = new TaskResponse(5L, "New task", null, TaskStatus.TODO,
                TaskPriority.MEDIUM, null, "demo", Set.of(), Instant.now(), null);
        when(taskService.create(any(TaskRequest.class), eq("demo"))).thenReturn(response);

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @WithMockUser(username = "demo")
    void create_returns400WhenTitleIsBlank() throws Exception {
        TaskRequest invalid = new TaskRequest("", null, TaskStatus.TODO,
                TaskPriority.MEDIUM, null, Set.of());

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }
}
