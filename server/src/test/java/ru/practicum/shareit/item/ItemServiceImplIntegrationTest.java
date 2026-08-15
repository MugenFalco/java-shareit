package ru.practicum.shareit.item;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что реальный запрос getAllByOwner корректно группирует бронирования
 * и комментарии сразу по нескольким вещам одним запросом на настоящей базе.
 * Логика сервиса и остальные ветки уже покрыты юнит-тестами в ItemServiceImplTest.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemServiceImplIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getAllByOwner_shouldGroupBookingsAndCommentsAcrossMultipleItems_fromRealDatabase() {
        User owner = persistUser("Владелец", "owner@test.ru");
        User renter = persistUser("Арендатор", "renter@test.ru");

        Item firstItem = persistItem(owner, "Дрель");
        Item secondItem = persistItem(owner, "Шуруповёрт");

        Booking approvedBooking = new Booking(null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1),
                firstItem, renter, BookingStatus.APPROVED);
        entityManager.persist(approvedBooking);

        Comment comment = new Comment();
        comment.setText("Отличная вещь!");
        comment.setItem(secondItem);
        comment.setAuthor(renter);
        comment.setCreated(LocalDateTime.now());
        entityManager.persist(comment);

        entityManager.flush();
        entityManager.clear();

        List<ItemWithBookingsDto> result = itemService.getAllByOwner(owner.getId());

        assertThat(result).hasSize(2);

        ItemWithBookingsDto firstDto = result.stream()
                .filter(dto -> dto.getName().equals("Дрель"))
                .findFirst().orElseThrow();
        assertThat(firstDto.getLastBooking()).isNotNull();
        assertThat(firstDto.getComments()).isEmpty();

        ItemWithBookingsDto secondDto = result.stream()
                .filter(dto -> dto.getName().equals("Шуруповёрт"))
                .findFirst().orElseThrow();
        assertThat(secondDto.getLastBooking()).isNull();
        assertThat(secondDto.getComments()).hasSize(1);
        assertThat(secondDto.getComments().getFirst().getText()).isEqualTo("Отличная вещь!");
    }

    private User persistUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        entityManager.persist(user);
        return user;
    }

    private Item persistItem(User owner, String name) {
        Item item = new Item();
        item.setName(name);
        item.setDescription("Описание " + name);
        item.setAvailable(true);
        item.setOwner(owner);
        entityManager.persist(item);
        return item;
    }
}