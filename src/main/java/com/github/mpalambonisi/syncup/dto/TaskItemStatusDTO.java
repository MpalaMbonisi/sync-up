package com.github.mpalambonisi.syncup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskItemStatusDTO {

    @NotBlank(message = "Task status isComplete cannot be empty.")
    private boolean isComplete;
}
