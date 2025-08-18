package com.github.mpalambonisi.syncup.dto;

import jakarta.validation.constraints.Email;
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
public class UserRegistrationDTO {

    @NotBlank(message = "First Name cannot be blank.")
    @NotEmpty(message = "First Name cannot be empty.")
    private String firstName;

    @NotBlank(message = "Last Name cannot be blank.")
    @NotEmpty(message = "Last Name cannot be empty.")
    private String lastName;

    @NotBlank(message = "Username cannot be blank.")
    @NotEmpty(message = "Username cannot be empty.")
    private String username;

    @NotEmpty(message = "Email cannot be empty.")
    @NotBlank(message = "Email cannot be blank.")
    @Email(message = "Please provide a valid email address.")
    private String email;

    @NotEmpty(message = "Password cannot be empty.")
    @NotBlank(message = "Password cannot be blank.")
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    private String password;
}
