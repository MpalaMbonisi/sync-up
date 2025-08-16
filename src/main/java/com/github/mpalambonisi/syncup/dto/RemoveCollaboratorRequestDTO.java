package com.github.mpalambonisi.syncup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class RemoveCollaboratorRequestDTO {

    @NotBlank(message = "Collaborator username cannot be blank.")
    @NotEmpty(message = "Collaborator username cannot be empty.")
    private String username;
}
