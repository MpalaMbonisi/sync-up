package com.github.mpalambonisi.syncup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class AddCollaboratorsRequestDTO {

    @NotBlank(message = "Collaborator username should not be empty")
    @NotEmpty(message = "Please provide at least one collaborator.")
    Set<String> collaborators;
}
