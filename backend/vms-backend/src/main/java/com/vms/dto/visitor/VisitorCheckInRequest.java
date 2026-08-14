package com.vms.dto.visitor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VisitorCheckInRequest(
    @NotBlank(message = "Ziyaretçi ad soyadı boş olamaz")
    String fullName,

    @NotNull(message = "Ziyaret edilecek personel seçilmelidir")
    Long hostId
) {}
