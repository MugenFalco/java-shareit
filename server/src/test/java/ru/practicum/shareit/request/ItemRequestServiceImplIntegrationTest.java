package ru.practicum.shareit.request;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Проверяет, что JPA-запросы (группировка ответов по нескольким запросам,
 * LAZY-связь Item -> ItemRequest) работают на настоящей базе,
 * логика сервиса уже покрыта юнит-тестами в ItemRequestServiceImplTest.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemRequestServiceImplIntegrationTest {

    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getOwn_shouldLoadAnswersAcrossMultipleRequests_fromRealDatabase() {
        User requestor = persistUser("Автор запроса", "requestor@test.ru");
        User owner = persistUser("Владелец вещи", "owner@test.ru");

        ItemRequest request = new ItemRequest();
        request.setDescription("Нужна дрель");
        request.setRequestor(requestor);
        request.setCreated(LocalDateTime.now());
        entityManager.persist(request);

        Item item = new Item();
        item.setName("Дрель Bosch");
        item.setDescription("Мощная дрель");
        item.setAvailable(true);
        item.setOwner(owner);
        item.setRequest(request);
        entityManager.persist(item);

        entityManager.flush();
        entityManager.clear();

        List<ItemRequestDto> result = itemRequestService.getOwn(requestor.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getItems()).hasSize(1);
        assertThat(result.getFirst().getItems().getFirst().getName()).isEqualTo("Дрель Bosch");
        assertThat(result.getFirst().getItems().getFirst().getOwnerId()).isEqualTo(owner.getId());
    }

    @Test
    void getById_shouldThrowNotFound_whenRequestDoesNotExistInDatabase() {
        User user = persistUser("Пользователь", "user@test.ru");
        entityManager.flush();

        assertThatThrownBy(() -> itemRequestService.getById(user.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    private User persistUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        entityManager.persist(user);
        return user;
    }
}