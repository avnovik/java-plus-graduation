package ru.practicum.explorewithme.dto.error;


public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
