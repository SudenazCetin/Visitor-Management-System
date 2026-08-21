package com.vms.dto.announcement;

import com.vms.enums.AnnouncementTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnnouncementRequest(
    @NotBlank(message = "Başlık boş olamaz.")
    @Size(max = 100, message = "Başlık en fazla 100 karakter olabilir.")
    String title,

    @NotBlank(message = "Duyuru mesajı boş olamaz.")
    @Size(max = 1000, message = "Duyuru mesajı en fazla 1000 karakter olabilir.")
    String message,

    @NotNull(message = "Hedef kitle seçilmelidir.")
    AnnouncementTarget target
) {}
