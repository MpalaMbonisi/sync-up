package com.github.mpalambonisi.syncup.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskItemResponseDTO {

    private long id;
    private String description;
    private boolean completed;
    private String taskListTitle;
}
