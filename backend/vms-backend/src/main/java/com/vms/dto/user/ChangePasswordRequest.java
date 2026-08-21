package com.vms.dto.user;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
    @NotBlank(message = "Mevcut şifre boş olamaz")
    String currentPassword,

    @NotBlank(message = "Yeni şifre boş olamaz")
    String newPassword
) {}
