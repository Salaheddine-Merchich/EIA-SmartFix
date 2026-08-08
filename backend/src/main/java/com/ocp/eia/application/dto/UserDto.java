package com.ocp.eia.application.dto;

import com.ocp.eia.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class UserDto {

    private UserDto() {}

    public record UserRequest(
            @NotBlank(message = "L'email est obligatoire")
            @Email(message = "Format d'email invalide")
            @Size(max = 255)
            String email,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
            String password,
            @NotNull(message = "Le rôle est obligatoire")
            Role role,
            @NotBlank(message = "Le nom prénom est obligatoire")
            @Size(max = 150)
            String nomPrenom,
            boolean actif
    ) {}

    public record UserUpdateRequest(
            @NotBlank(message = "L'email est obligatoire")
            @Email(message = "Format d'email invalide")
            @Size(max = 255)
            String email,
            @NotNull(message = "Le rôle est obligatoire")
            Role role,
            @NotBlank(message = "Le nom prénom est obligatoire")
            @Size(max = 150)
            String nomPrenom,
            boolean actif,
            @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
            String password
    ) {}

    public record UserResponse(
            UUID id,
            String email,
            Role role,
            String nomPrenom,
            boolean actif
    ) {}
}
