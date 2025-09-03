package com.github.mpalambonisi.syncup.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskItemDescriptionDTO {

    @NotEmpty(message = "Description cannot be empty.")
    @NotBlank(message = "Description cannot be blank.")
    private String description;
}
