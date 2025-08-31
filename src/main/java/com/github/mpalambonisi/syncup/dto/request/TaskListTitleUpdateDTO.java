package com.github.mpalambonisi.syncup.dto.request;

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
public class TaskListTitleUpdateDTO {

    @NotEmpty(message = "Title cannot be empty.")
    @NotBlank(message = "Title cannot be blank.")
    private String title;
}
