package ru.practicum.explorewithme.compilations.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.compilations.dto.CompilationDto;
import ru.practicum.explorewithme.compilations.dto.CompilationWithStats;
import ru.practicum.explorewithme.compilations.dto.NewCompilationDto;
import ru.practicum.explorewithme.compilations.model.Compilation;
import ru.practicum.explorewithme.event.dto.EventShortDto;
import ru.practicum.explorewithme.event.dto.EventStatsDto;
import ru.practicum.explorewithme.event.mapper.EventMapper;
import ru.practicum.explorewithme.event.model.Event;

import java.util.List;
import java.util.Map;

@UtilityClass
public class CompilationMapper {

    public static Compilation toCompilation(NewCompilationDto dto, List<Event> events) {
        return new Compilation(events, dto.isPinned(), dto.getTitle());
    }

    public static CompilationDto toDto(CompilationWithStats param) {
        Compilation compilation = param.getCompilation();
        List<Event> events = compilation.getEvents();
        Map<Long, EventStatsDto> statsDtoMap = param.getStatsDtoMap();

        List<EventShortDto> shortEvents = events.stream()
                .map(event -> {
                    EventStatsDto stats = statsDtoMap.getOrDefault(
                            event.getId(),
                            new EventStatsDto(0L, 0L)
                    );
                    return EventMapper.toEventShortDto(event, stats);
                })
                .toList();

        return new CompilationDto(compilation.getId(), shortEvents, compilation.getPinned(), compilation.getTitle());
    }


    public static List<CompilationDto> toListDto(List<CompilationWithStats> params) {
        return params.stream().map(CompilationMapper::toDto).toList();
    }
}
