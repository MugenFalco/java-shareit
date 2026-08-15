package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.NewUserRequest;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void create_shouldCreateUser() {
        NewUserRequest request = new NewUserRequest("Иван", "ivan@test.ru");

        when(userRepository.existsByEmail("ivan@test.ru")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserDto result = userService.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Иван");
        assertThat(result.getEmail()).isEqualTo("ivan@test.ru");
    }

    @Test
    void create_shouldThrowConflict_whenEmailAlreadyExists() {
        NewUserRequest request = new NewUserRequest("Иван", "ivan@test.ru");

        when(userRepository.existsByEmail("ivan@test.ru")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateNameAndEmail() {
        User existing = new User(1L, "Иван", "ivan@test.ru");
        UpdateUserRequest request = new UpdateUserRequest("Пётр", "petr@test.ru");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("petr@test.ru", 1L)).thenReturn(false);

        UserDto result = userService.update(1L, request);

        assertThat(result.getName()).isEqualTo("Пётр");
        assertThat(result.getEmail()).isEqualTo("petr@test.ru");
    }

    @Test
    void update_shouldIgnoreBlankFields() {
        User existing = new User(1L, "Иван", "ivan@test.ru");
        UpdateUserRequest request = new UpdateUserRequest("", "");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        UserDto result = userService.update(1L, request);

        assertThat(result.getName()).isEqualTo("Иван");
        assertThat(result.getEmail()).isEqualTo("ivan@test.ru");
        verify(userRepository, never()).existsByEmailAndIdNot(any(), any());
    }

    @Test
    void update_shouldThrowConflict_whenEmailTakenByAnotherUser() {
        User existing = new User(2L, "Пётр", "petr@test.ru");
        UpdateUserRequest request = new UpdateUserRequest(null, "ivan@test.ru");

        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("ivan@test.ru", 2L)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(2L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_shouldThrowNotFound_whenUserDoesNotExist() {
        UpdateUserRequest request = new UpdateUserRequest("Пётр", null);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(999L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_shouldReturnUser() {
        User existing = new User(1L, "Иван", "ivan@test.ru");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        UserDto result = userService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getById_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAll_shouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(
                new User(1L, "Иван", "ivan@test.ru"),
                new User(2L, "Пётр", "petr@test.ru")
        ));

        List<UserDto> result = userService.getAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void delete_shouldRemoveUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void delete_shouldBeIdempotent_whenUserDoesNotExist() {
        when(userRepository.existsById(999L)).thenReturn(false);

        userService.delete(999L);

        verify(userRepository, never()).deleteById(any());
    }
}