package com.vms.dto.user;

import com.vms.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter arasında olmalıdır")
    String username,

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 4, message = "Şifre en az 4 karakter olmalıdır")
    String password,

    @NotNull(message = "Kullanıcı rolü seçilmelidir")
    Role role
) {}
