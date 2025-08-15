package com.github.mpalambonisi.syncup.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class TaskList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @NonNull
    private String title;
    @NonNull
    @ManyToOne
    private User owner;
    @NonNull
    @OneToMany
    private List<TaskItem> tasks;
    @NonNull
    @ManyToMany
    private Set<User> collaborators;
}
