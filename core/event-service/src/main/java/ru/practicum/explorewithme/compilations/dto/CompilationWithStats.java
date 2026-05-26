package ru.practicum.explorewithme.compilations.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.explorewithme.compilations.model.Compilation;
import ru.practicum.explorewithme.event.dto.EventStatsDto;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompilationWithStats {
    private Compilation compilation;
    private Map<Long, EventStatsDto> statsDtoMap;
}
