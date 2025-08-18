package com.github.mpalambonisi.syncup.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RemoveCollaboratorRequestDTO {

    @NotBlank(message = "Collaborator username cannot be blank.")
    @NotEmpty(message = "Collaborator username cannot be empty.")
    private String username;
}
