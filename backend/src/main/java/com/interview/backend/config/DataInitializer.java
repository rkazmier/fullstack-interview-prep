package com.interview.backend.config;

import com.interview.backend.tag.Tag;
import com.interview.backend.tag.TagRepository;
import com.interview.backend.task.Task;
import com.interview.backend.task.TaskPriority;
import com.interview.backend.task.TaskRepository;
import com.interview.backend.task.TaskStatus;
import com.interview.backend.user.Role;
import com.interview.backend.user.User;
import com.interview.backend.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Set;

/**
 * Seeds demo data on startup. @Profile("!test") keeps it out of tests -
 * profiles are another popular interview topic.
 */
@Configuration
@Profile("!test")
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(UserRepository users,
                               TaskRepository tasks,
                               TagRepository tags,
                               PasswordEncoder encoder) {
        return args -> {
            User demo = users.save(new User("demo", "demo@example.com", encoder.encode("demo123"), Role.USER));
            users.save(new User("admin", "admin@example.com", encoder.encode("admin123"), Role.ADMIN));

            Tag backend = tags.save(new Tag("backend"));
            Tag frontend = tags.save(new Tag("frontend"));
            Tag urgent = tags.save(new Tag("urgent"));

            Task t1 = new Task("Learn Spring Security", "JWT, filters, SecurityFilterChain",
                    TaskStatus.IN_PROGRESS, TaskPriority.HIGH, LocalDate.now().plusDays(3), demo);
            t1.setTags(Set.of(backend, urgent));

            Task t2 = new Task("Practice JPA relationships", "OneToMany, ManyToMany, fetch types, N+1",
                    TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.now().plusDays(7), demo);
            t2.setTags(Set.of(backend));

            Task t3 = new Task("Review React hooks", "useState, useEffect, useMemo, custom hooks",
                    TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.now().plusDays(5), demo);
            t3.setTags(Set.of(frontend));

            Task t4 = new Task("Mock interview", "System design + live coding practice",
                    TaskStatus.DONE, TaskPriority.LOW, null, demo);

            tasks.saveAll(java.util.List.of(t1, t2, t3, t4));
        };
    }
}
