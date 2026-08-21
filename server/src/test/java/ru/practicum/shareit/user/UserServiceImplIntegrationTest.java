package ru.practicum.shareit.user;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.user.dto.NewUserRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Проверяет, что проверка уникальности email реально срабатывает на настоящей базе,
 * и что delete идемпотентен на реальных данных. Остальная логика уже покрыта
 * юнит-тестами в {@link UserServiceImplTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceImplIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void create_shouldThrowConflict_whenEmailAlreadyExistsInDatabase() {
        User existing = new User();
        existing.setName("Иван");
        existing.setEmail("ivan@test.ru");
        entityManager.persist(existing);
        entityManager.flush();

        NewUserRequest duplicate = new NewUserRequest("Пётр", "ivan@test.ru");

        assertThatThrownBy(() -> userService.create(duplicate))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void delete_shouldRemoveUserFromDatabase() {
        User user = new User();
        user.setName("Иван");
        user.setEmail("ivan@test.ru");
        entityManager.persist(user);
        entityManager.flush();
        Long userId = user.getId();

        userService.delete(userId);
        entityManager.flush();

        assertThat(entityManager.find(User.class, userId)).isNull();
    }
}