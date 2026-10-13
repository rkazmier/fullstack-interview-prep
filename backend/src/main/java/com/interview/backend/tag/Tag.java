package com.interview.backend.tag;

import jakarta.persistence.*;

/**
 * Owned side of a @ManyToMany relationship with Task.
 */
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    protected Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
