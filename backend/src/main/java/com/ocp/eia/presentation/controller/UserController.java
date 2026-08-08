package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.UserDto.UserRequest;
import com.ocp.eia.application.dto.UserDto.UserResponse;
import com.ocp.eia.application.dto.UserDto.UserUpdateRequest;
import com.ocp.eia.modules.iam.application.CreateUserUseCase;
import com.ocp.eia.modules.iam.application.DeleteUserUseCase;
import com.ocp.eia.modules.iam.application.FindUserByIdUseCase;
import com.ocp.eia.modules.iam.application.ListUsersUseCase;
import com.ocp.eia.modules.iam.application.UpdateUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Utilisateurs")
public class UserController {

    private final ListUsersUseCase listUsersUseCase;
    private final FindUserByIdUseCase findUserByIdUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    @GetMapping
    @Operation(summary = "Lister les utilisateurs")
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(listUsersUseCase.execute());
    }

    @GetMapping("/assignable")
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    @Operation(summary = "Responsables assignables aux pannes")
    public ResponseEntity<List<UserResponse>> findAssignable() {
        return ResponseEntity.ok(listUsersUseCase.executeAssignable());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(findUserByIdUseCase.execute(id));
    }

    @PostMapping
    @Operation(summary = "Créer un utilisateur")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createUserUseCase.execute(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(updateUserUseCase.execute(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteUserUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
