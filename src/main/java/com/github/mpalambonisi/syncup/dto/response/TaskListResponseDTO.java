package com.github.mpalambonisi.syncup.dto.response;

import com.github.mpalambonisi.syncup.model.TaskItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskListResponseDTO {

    private long id;
    private String title;
    private String owner;
    private Set<String> collaborators;
    private List<TaskItemResponseDTO> tasks;

}
