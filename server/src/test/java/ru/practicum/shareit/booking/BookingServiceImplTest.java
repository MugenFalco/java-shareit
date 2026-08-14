package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.NewBookingRequest;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceImplTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User(null, "Владелец", "owner@test.ru"));
        booker = userRepository.save(new User(null, "Арендатор", "booker@test.ru"));

        item = new Item();
        item.setName("Дрель");
        item.setDescription("Мощная дрель");
        item.setAvailable(true);
        item.setOwner(owner);
        item = itemRepository.save(item);
    }

    private NewBookingRequest futureRequest() {
        return new NewBookingRequest(item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));
    }

    // ---------- create ----------

    @Test
    void create_shouldCreateBookingInWaitingStatus() {
        BookingDto result = bookingService.create(booker.getId(), futureRequest());

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(result.getItem().getId()).isEqualTo(item.getId());
        assertThat(result.getBooker().getId()).isEqualTo(booker.getId());
    }

    @Test
    void create_shouldThrowNotFound_whenItemDoesNotExist() {
        NewBookingRequest request = new NewBookingRequest(999L,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.create(booker.getId(), request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_shouldThrowNotFound_whenUserDoesNotExist() {
        assertThatThrownBy(() -> bookingService.create(999L, futureRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_shouldThrowValidation_whenItemNotAvailable() {
        item.setAvailable(false);
        itemRepository.save(item);

        assertThatThrownBy(() -> bookingService.create(booker.getId(), futureRequest()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_shouldThrowNotFound_whenOwnerBooksOwnItem() {
        assertThatThrownBy(() -> bookingService.create(owner.getId(), futureRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_shouldThrowValidation_whenEndNotAfterStart() {
        NewBookingRequest request = new NewBookingRequest(item.getId(),
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> bookingService.create(booker.getId(), request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_shouldThrowValidation_whenOverlapsWithApprovedBooking() {
        BookingDto first = bookingService.create(booker.getId(), futureRequest());
        bookingService.approve(owner.getId(), first.getId(), true);

        assertThatThrownBy(() -> bookingService.create(booker.getId(), futureRequest()))
                .isInstanceOf(ValidationException.class);
    }

    // ---------- approve ----------

    @Test
    void approve_shouldSetApproved_whenApprovedTrue() {
        BookingDto created = bookingService.create(booker.getId(), futureRequest());

        BookingDto result = bookingService.approve(owner.getId(), created.getId(), true);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void approve_shouldSetRejected_whenApprovedFalse() {
        BookingDto created = bookingService.create(booker.getId(), futureRequest());

        BookingDto result = bookingService.approve(owner.getId(), created.getId(), false);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void approve_shouldThrowForbidden_whenNotOwner() {
        BookingDto created = bookingService.create(booker.getId(), futureRequest());

        assertThatThrownBy(() -> bookingService.approve(booker.getId(), created.getId(), true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void approve_shouldThrowValidation_whenAlreadyProcessed() {
        BookingDto created = bookingService.create(booker.getId(), futureRequest());
        bookingService.approve(owner.getId(), created.getId(), true);

        assertThatThrownBy(() -> bookingService.approve(owner.getId(), created.getId(), false))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void approve_shouldThrowNotFound_whenBookingDoesNotExist() {
        assertThatThrownBy(() -> bookingService.approve(owner.getId(), 999L, true))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- getById ----------

    @Test
    void getById_shouldReturnBooking_whenBooker() {
        BookingDto created = bookingService.create(booker.getId(), futureRequest());

        BookingDto result = bookingService.getById(booker.getId(), created.getId());

        assertThat(result.getId()).isEqualTo(created.getId());
    }

    @Test
    void getById_shouldReturnBooking_whenOwner() {
        BookingDto created = bookingService.create(booker.getId(), futureRequest());

        BookingDto result = bookingService.getById(owner.getId(), created.getId());

        assertThat(result.getId()).isEqualTo(created.getId());
    }

    @Test
    void getById_shouldThrowNotFound_whenNeitherBookerNorOwner() {
        BookingDto created = bookingService.create(booker.getId(), futureRequest());
        User stranger = userRepository.save(new User(null, "Посторонний", "stranger@test.ru"));

        assertThatThrownBy(() -> bookingService.getById(stranger.getId(), created.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_shouldThrowNotFound_whenBookingDoesNotExist() {
        assertThatThrownBy(() -> bookingService.getById(booker.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- getAllByBooker ----------

    @Test
    void getAllByBooker_shouldReturnAll() {
        bookingService.create(booker.getId(), futureRequest());

        List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "ALL");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldReturnCurrent() {
        Booking current = new Booking(null,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1),
                item, booker, BookingStatus.APPROVED);
        bookingRepository.save(current);

        List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "CURRENT");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldReturnPast() {
        Booking past = new Booking(null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1),
                item, booker, BookingStatus.APPROVED);
        bookingRepository.save(past);

        List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "PAST");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldReturnFuture() {
        bookingService.create(booker.getId(), futureRequest());

        List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "FUTURE");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldReturnWaiting() {
        bookingService.create(booker.getId(), futureRequest());

        List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "WAITING");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldReturnRejected() {
        BookingDto created = bookingService.create(booker.getId(), futureRequest());
        bookingService.approve(owner.getId(), created.getId(), false);

        List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "REJECTED");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldThrowValidation_whenUnknownState() {
        assertThatThrownBy(() -> bookingService.getAllByBooker(booker.getId(), "UNSUPPORTED"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void getAllByBooker_shouldThrowNotFound_whenUserDoesNotExist() {
        assertThatThrownBy(() -> bookingService.getAllByBooker(999L, "ALL"))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- getAllByOwner ----------

    @Test
    void getAllByOwner_shouldReturnAll() {
        bookingService.create(booker.getId(), futureRequest());

        List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "ALL");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldReturnCurrent() {
        Booking current = new Booking(null,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1),
                item, booker, BookingStatus.APPROVED);
        bookingRepository.save(current);

        List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "CURRENT");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldReturnPast() {
        Booking past = new Booking(null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1),
                item, booker, BookingStatus.APPROVED);
        bookingRepository.save(past);

        List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "PAST");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldReturnFuture() {
        bookingService.create(booker.getId(), futureRequest());

        List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "FUTURE");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldReturnWaiting() {
        bookingService.create(booker.getId(), futureRequest());

        List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "WAITING");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldReturnRejected() {
        BookingDto created = bookingService.create(booker.getId(), futureRequest());
        bookingService.approve(owner.getId(), created.getId(), false);

        List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "REJECTED");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldThrowValidation_whenUnknownState() {
        assertThatThrownBy(() -> bookingService.getAllByOwner(owner.getId(), "UNSUPPORTED"))
                .isInstanceOf(ValidationException.class);
    }
}