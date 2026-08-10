package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDto create(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException(
                    "Пользователь с email " + userDto.getEmail() + " уже существует"
            );
        }

        User user = userRepository.save(UserMapper.toUser(userDto));
        log.info("Создан пользователь с id {}", user.getId());
        return UserMapper.toUserDto(user);
    }

    @Override
    public UserDto update(Long userId, UserDto userDto) {
        User user = findUserOrThrow(userId);

        if (userDto.getName() != null && !userDto.getName().isBlank()) {
            user.setName(userDto.getName());
        }

        if (userDto.getEmail() != null && !userDto.getEmail().isBlank()) {
            if (!user.getEmail().equalsIgnoreCase(userDto.getEmail())
                    && userRepository.existsByEmail(userDto.getEmail())) {
                throw new ConflictException(
                        "Пользователь с email " + userDto.getEmail() + " уже существует"
                );
            }
            user.setEmail(userDto.getEmail());
        }

        log.info("Обновлён пользователь с id {}", userId);
        return UserMapper.toUserDto(userRepository.update(user));
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
    public void delete(Long userId) {
        userRepository.deleteById(userId);
        log.info("Удалён пользователь с id {}", userId);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + userId + " не найден"
                ));
    }
}