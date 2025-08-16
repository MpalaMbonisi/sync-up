package com.github.mpalambonisi.syncup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskItemStatusDTO {

    @NotBlank(message = "Task status isComplete cannot be empty.")
    private boolean isComplete;
}
