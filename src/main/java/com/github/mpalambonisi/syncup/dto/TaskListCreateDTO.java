package com.github.mpalambonisi.syncup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskListCreateDTO {

    @NotBlank(message = "Title cannot be blank.")
    @NotEmpty(message = "Title cannot be empty.")
    private String title;
}
