package ru.practicum.explorewithme.compilations.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explorewithme.compilations.model.Compilation;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long>, CompilationRepositoryCustom {

    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    List<Compilation> findCompilationsByPinned(boolean pinned, Pageable pageable);
}
