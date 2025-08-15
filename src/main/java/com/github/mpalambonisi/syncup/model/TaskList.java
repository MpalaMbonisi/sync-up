package com.github.mpalambonisi.syncup.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
public class TaskList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String title;
    @ManyToOne
    private User owner;
    @OneToMany
    private List<TaskItem> tasks;
    @ManyToMany
    private Set<User> collaborators;
}
