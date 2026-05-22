package ru.practicum.explorewithme.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    @NotNull
    @Schema(description = "Широта", example = "55.754167")
    private Float lat;

    @NotNull
    @Schema(description = "Долгота", example = "37.62")
    private Float lon;
}