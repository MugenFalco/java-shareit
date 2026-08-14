package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.NewUserRequest;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(NewUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(
                    "Пользователь с email " + request.getEmail() + " уже существует"
            );
        }

        User user = userRepository.save(UserMapper.toUser(request));
        log.info("Создан пользователь с id {}", user.getId());
        return UserMapper.toUserDto(user);
    }

    @Override
    @Transactional
    public UserDto update(Long userId, UpdateUserRequest request) {
        User user = findUserOrThrow(userId);

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
                throw new ConflictException(
                        "Пользователь с email " + request.getEmail() + " уже существует"
                );
            }
            user.setEmail(request.getEmail());
        }

        log.info("Обновлён пользователь с id {}", userId);
        return UserMapper.toUserDto(user);
    }

    @Override
    public UserDto getById(Long userId) {
        return UserMapper.toUserDto(findUserOrThrow(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        }
        log.info("Удалён пользователь с id {}", userId);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + userId + " не найден"
                ));
    }
}