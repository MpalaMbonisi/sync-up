package com.github.mpalambonisi.syncup.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCollaboratorsRequestDTO {

    @NotBlank(message = "Collaborator username should not be empty")
    @NotEmpty(message = "Please provide at least one collaborator.")
    Set<String> collaborators;
}
