package ru.practicum.shareit.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.user.dto.NewUserRequestDto;
import ru.practicum.shareit.user.dto.UpdateUserRequestDto;

@RestController
@RequestMapping(path = "/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserClient userClient;

    @PostMapping
    public ResponseEntity<Object> create(@Valid @RequestBody NewUserRequestDto requestDto) {
        log.info("Creating user {}", requestDto);
        return userClient.create(requestDto);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<Object> update(@PathVariable Long userId,
                                         @Valid @RequestBody UpdateUserRequestDto requestDto) {
        log.info("Updating user {}, userId={}", requestDto, userId);
        return userClient.update(userId, requestDto);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Object> getById(@PathVariable Long userId) {
        log.info("Get user {}", userId);
        return userClient.getById(userId);
    }

    @GetMapping
    public ResponseEntity<Object> getAll() {
        log.info("Get all users");
        return userClient.getAll();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Object> delete(@PathVariable Long userId) {
        log.info("Delete user {}", userId);
        return userClient.deleteUser(userId);
    }
}