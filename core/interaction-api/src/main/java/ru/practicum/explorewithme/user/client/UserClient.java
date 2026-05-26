package ru.practicum.explorewithme.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.explorewithme.user.dto.UserDto;

@FeignClient(name = "user-service", contextId = "userClient", path = "/internal/users")
public interface UserClient {

    @GetMapping("/{userId}")
    UserDto getUser(@PathVariable("userId") long userId);
}
