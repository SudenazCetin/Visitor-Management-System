package com.vms.dto.personnel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PersonnelRequest(
    @NotBlank(message = "Ad Soyad boş olamaz")
    String fullName,

    @NotBlank(message = "Departman boş olamaz")
    String department,

    String title,

    @NotBlank(message = "E-posta boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    String email,

    Boolean createAccount,
    String username,
    String password
) {}
