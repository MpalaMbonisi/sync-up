package com.github.mpalambonisi.syncup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskListCreateDTO {

    @NotBlank(message = "Title cannot be blank.")
    @NotEmpty(message = "Title cannot be empty.")
    private String title;
}
