package com.vms.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 3, message = "Kullanıcı adı en az 3 karakter olmalıdır")
    String username,

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    String password,

    String fullName,

    String email,

    String registrationType,

    String adminRegistrationCode
) {}
