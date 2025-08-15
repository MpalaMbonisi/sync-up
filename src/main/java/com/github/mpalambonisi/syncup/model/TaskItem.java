package com.github.mpalambonisi.syncup.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
public class TaskItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @NonNull
    private String description;
    @NonNull
    private boolean isCompleted;
    @NonNull
    @ManyToOne
    private TaskList taskList;
}
