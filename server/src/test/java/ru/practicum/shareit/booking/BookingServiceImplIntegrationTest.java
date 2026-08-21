package ru.practicum.shareit.booking;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.NewBookingRequest;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Проверяет реальные JPA-запросы по датам (existsApprovedOverlap, поиск CURRENT
 * по start/end относительно now()) на настоящей базе. Логика сервиса и остальные
 * ветки уже покрыты юнит-тестами в {@link BookingServiceImplTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceImplIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EntityManager entityManager;

    private User persistUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        entityManager.persist(user);
        return user;
    }

    private Item persistItem(User owner) {
        Item item = new Item();
        item.setName("Дрель");
        item.setDescription("Мощная дрель");
        item.setAvailable(true);
        item.setOwner(owner);
        entityManager.persist(item);
        return item;
    }

    @Test
    void create_shouldThrowValidation_whenOverlapsWithRealApprovedBooking() {
        User owner = persistUser("Владелец", "owner@test.ru");
        User booker = persistUser("Арендатор", "booker@test.ru");
        Item item = persistItem(owner);

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(3);

        Booking existing = new Booking(null, start, end, item, booker, BookingStatus.APPROVED);
        entityManager.persist(existing);
        entityManager.flush();

        NewBookingRequest overlapping = new NewBookingRequest(item.getId(),
                start.plusHours(12), end.plusHours(12));

        assertThatThrownBy(() -> bookingService.create(booker.getId(), overlapping))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void getAllByOwner_shouldFindCurrentBooking_byRealDateComparison() {
        User owner = persistUser("Владелец", "owner@test.ru");
        User booker = persistUser("Арендатор", "booker@test.ru");
        Item item = persistItem(owner);

        Booking current = new Booking(null,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1),
                item, booker, BookingStatus.APPROVED);
        entityManager.persist(current);
        entityManager.flush();
        entityManager.clear();

        List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "CURRENT");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(BookingStatus.APPROVED);
    }
}