package ru.practicum.explorewithme.category.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.explorewithme.category.dto.CategoryDto;

@FeignClient(name = "category-service", contextId = "categoryClient", path = "/internal/categories")
public interface CategoryClient {

    @GetMapping("/{categoryId}")
    CategoryDto getCategory(@PathVariable("categoryId") long categoryId);
}
