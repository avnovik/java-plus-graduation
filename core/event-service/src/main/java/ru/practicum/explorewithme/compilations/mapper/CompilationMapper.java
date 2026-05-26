package ru.practicum.explorewithme.compilations.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.category.dto.CategoryDto;
import ru.practicum.explorewithme.compilations.dto.CompilationDto;
import ru.practicum.explorewithme.compilations.dto.CompilationWithStats;
import ru.practicum.explorewithme.compilations.dto.NewCompilationDto;
import ru.practicum.explorewithme.compilations.model.Compilation;
import ru.practicum.explorewithme.event.dto.EventShortDto;
import ru.practicum.explorewithme.event.dto.EventStatsDto;
import ru.practicum.explorewithme.event.mapper.EventMapper;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.user.dto.UserShortDto;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@UtilityClass
public class CompilationMapper {

    public static Compilation toCompilation(NewCompilationDto dto, List<Event> events) {
        return new Compilation(events, dto.isPinned(), dto.getTitle());
    }

    public static CompilationDto toDto(CompilationWithStats param,
                                       Function<Long, CategoryDto> categoryLoader,
                                       Function<Long, UserShortDto> initiatorLoader) {
        Compilation compilation = param.getCompilation();
        List<Event> events = compilation.getEvents();
        Map<Long, EventStatsDto> statsDtoMap = param.getStatsDtoMap();

        List<EventShortDto> shortEvents = events.stream()
                .map(event -> {
                    EventStatsDto stats = statsDtoMap.getOrDefault(
                            event.getId(),
                            new EventStatsDto(0L, 0L)
                    );
                    return EventMapper.toEventShortDto(
                            event,
                            stats,
                            categoryLoader.apply(event.getCategoryId()),
                            initiatorLoader.apply(event.getInitiatorId())
                    );
                })
                .toList();

        return new CompilationDto(compilation.getId(), shortEvents, compilation.getPinned(), compilation.getTitle());
    }


    public static List<CompilationDto> toListDto(List<CompilationWithStats> params,
                                                 Function<Long, CategoryDto> categoryLoader,
                                                 Function<Long, UserShortDto> initiatorLoader) {
        return params.stream().map(p -> toDto(p, categoryLoader, initiatorLoader)).toList();
    }
}
