package com.github.mpalambonisi.syncup.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequestDTO {

    @NotEmpty(message = "Username cannot be empty.")
    @NotBlank(message = "Username cannot be blank.")
    private String username;

    @NotEmpty(message = "Password cannot be empty.")
    @NotBlank(message = "Password cannot be blank.")
    @Size(min = 8,message = "Password must be at least 8 characters long.")
    private String password;

}
