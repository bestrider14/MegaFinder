package com.finder.demo.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UserRequest(@NotBlank String displayName, @Email @NotBlank String email,
                          @NotBlank String role, List<String> extraPermissions, Boolean active) {
}
