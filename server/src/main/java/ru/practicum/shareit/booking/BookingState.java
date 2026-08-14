package ru.practicum.shareit.booking;

import java.util.Arrays;
import java.util.Optional;

public enum BookingState {
    ALL,
    CURRENT,
    PAST,
    FUTURE,
    WAITING,
    REJECTED;

    public static Optional<BookingState> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(state -> state.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}