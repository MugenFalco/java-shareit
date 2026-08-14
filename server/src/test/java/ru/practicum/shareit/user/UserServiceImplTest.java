package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.NewUserRequest;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void create_shouldCreateUser() {
        NewUserRequest request = new NewUserRequest("Иван", "ivan@test.ru");

        UserDto result = userService.create(request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Иван");
        assertThat(result.getEmail()).isEqualTo("ivan@test.ru");
    }

    @Test
    void create_shouldThrowConflict_whenEmailAlreadyExists() {
        userService.create(new NewUserRequest("Иван", "ivan@test.ru"));
        NewUserRequest duplicate = new NewUserRequest("Пётр", "ivan@test.ru");

        assertThatThrownBy(() -> userService.create(duplicate))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_shouldUpdateNameAndEmail() {
        UserDto created = userService.create(new NewUserRequest("Иван", "ivan@test.ru"));
        UpdateUserRequest request = new UpdateUserRequest("Пётр", "petr@test.ru");

        UserDto result = userService.update(created.getId(), request);

        assertThat(result.getName()).isEqualTo("Пётр");
        assertThat(result.getEmail()).isEqualTo("petr@test.ru");
    }

    @Test
    void update_shouldIgnoreBlankFields() {
        UserDto created = userService.create(new NewUserRequest("Иван", "ivan@test.ru"));
        UpdateUserRequest request = new UpdateUserRequest("", "");

        UserDto result = userService.update(created.getId(), request);

        assertThat(result.getName()).isEqualTo("Иван");
        assertThat(result.getEmail()).isEqualTo("ivan@test.ru");
    }

    @Test
    void update_shouldThrowConflict_whenEmailTakenByAnotherUser() {
        userService.create(new NewUserRequest("Иван", "ivan@test.ru"));
        UserDto second = userService.create(new NewUserRequest("Пётр", "petr@test.ru"));
        UpdateUserRequest request = new UpdateUserRequest(null, "ivan@test.ru");

        assertThatThrownBy(() -> userService.update(second.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_shouldThrowNotFound_whenUserDoesNotExist() {
        UpdateUserRequest request = new UpdateUserRequest("Пётр", null);

        assertThatThrownBy(() -> userService.update(999L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_shouldReturnUser() {
        UserDto created = userService.create(new NewUserRequest("Иван", "ivan@test.ru"));

        UserDto result = userService.getById(created.getId());

        assertThat(result.getId()).isEqualTo(created.getId());
    }

    @Test
    void getById_shouldThrowNotFound_whenUserDoesNotExist() {
        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAll_shouldReturnAllUsers() {
        userService.create(new NewUserRequest("Иван", "ivan@test.ru"));
        userService.create(new NewUserRequest("Пётр", "petr@test.ru"));

        List<UserDto> result = userService.getAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void delete_shouldRemoveUser() {
        UserDto created = userService.create(new NewUserRequest("Иван", "ivan@test.ru"));

        userService.delete(created.getId());

        assertThat(userRepository.existsById(created.getId())).isFalse();
    }

    @Test
    void delete_shouldBeIdempotent_whenUserDoesNotExist() {
        userService.delete(999L);
    }
}