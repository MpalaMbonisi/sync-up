package com.github.mpalambonisi.syncup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskItemCreateDTO {

    @NotBlank(message = "Task description cannot be blank.")
    @NotEmpty(message = "Task description cannot be empty.")
    private String description;

}
