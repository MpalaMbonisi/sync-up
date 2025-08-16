package com.github.mpalambonisi.syncup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class TaskItemCreateDTO {

    @NotBlank(message = "Task description cannot be blank.")
    @NotEmpty(message = "Task description cannot be empty.")
    private String description;

}
