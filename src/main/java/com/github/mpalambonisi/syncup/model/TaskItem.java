package com.github.mpalambonisi.syncup.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class TaskItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private boolean isCompleted;
    @ManyToOne
    @JoinColumn(name = "tasklist_id", nullable = false)
    private TaskList taskList;
}
