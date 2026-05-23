package ru.practicum.explorewithme.user.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.user.dto.NewUserRequest;
import ru.practicum.explorewithme.user.dto.UserDto;
import ru.practicum.explorewithme.user.model.User;

@UtilityClass
public class UserMapper {

    public static User toUser(NewUserRequest dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        return user;
    }

    public static UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }
}
