package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = new User(1L, "Владелец", "owner@test.ru");
        booker = new User(2L, "Арендатор", "booker@test.ru");

        item = new Item();
        item.setId(10L);
        item.setName("Дрель");
        item.setAvailable(true);
        item.setOwner(owner);
    }

    private NewBookingRequest futureRequest() {
        return new NewBookingRequest(item.getId(),
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
    }

    private Booking savedBooking(Long id, BookingStatus status) {
        NewBookingRequest req = futureRequest();
        return new Booking(id, req.getStart(), req.getEnd(), item, booker, status);
    }

    // ---------- create ----------

    @Test
    void create_shouldCreateBookingInWaitingStatus() {
        NewBookingRequest request = futureRequest();

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsApprovedOverlap(eq(10L), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(100L);
            return booking;
        });

        BookingDto result = bookingService.create(2L, request);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    void create_shouldThrowNotFound_whenItemDoesNotExist() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(2L, futureRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(999L, futureRequest()))
                .isInstanceOf(NotFoundException.class);
        verify(itemRepository, never()).findById(anyLong());
    }

    @Test
    void create_shouldThrowValidation_whenItemNotAvailable() {
        item.setAvailable(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(2L, futureRequest()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_shouldThrowNotFound_whenOwnerBooksOwnItem() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(1L, futureRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_shouldThrowValidation_whenOverlapsWithApprovedBooking() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsApprovedOverlap(eq(10L), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(2L, futureRequest()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_shouldThrowValidation_whenEndNotAfterStart() {
        NewBookingRequest request = new NewBookingRequest(10L,
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1));

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsApprovedOverlap(eq(10L), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> bookingService.create(2L, request))
                .isInstanceOf(ValidationException.class);
    }

    // ---------- approve ----------

    @Test
    void approve_shouldSetApproved_whenApprovedTrue() {
        Booking booking = savedBooking(100L, BookingStatus.WAITING);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.approve(1L, 100L, true);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void approve_shouldSetRejected_whenApprovedFalse() {
        Booking booking = savedBooking(100L, BookingStatus.WAITING);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.approve(1L, 100L, false);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void approve_shouldThrowForbidden_whenNotOwner() {
        Booking booking = savedBooking(100L, BookingStatus.WAITING);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.approve(2L, 100L, true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void approve_shouldThrowValidation_whenAlreadyProcessed() {
        Booking booking = savedBooking(100L, BookingStatus.APPROVED);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.approve(1L, 100L, false))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void approve_shouldThrowNotFound_whenBookingDoesNotExist() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.approve(1L, 999L, true))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- getById ----------

    @Test
    void getById_shouldReturnBooking_whenBooker() {
        Booking booking = savedBooking(100L, BookingStatus.WAITING);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.getById(2L, 100L);

        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    void getById_shouldReturnBooking_whenOwner() {
        Booking booking = savedBooking(100L, BookingStatus.WAITING);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.getById(1L, 100L);

        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    void getById_shouldThrowNotFound_whenNeitherBookerNorOwner() {
        Booking booking = savedBooking(100L, BookingStatus.WAITING);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getById(999L, 100L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_shouldThrowNotFound_whenBookingDoesNotExist() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getById(2L, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- getAllByBooker ----------

    @Test
    void getAllByBooker_shouldReturnAll() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdOrderByStartDesc(2L))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.WAITING)));

        List<BookingDto> result = bookingService.getAllByBooker(2L, "ALL");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldReturnCurrent() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(eq(2L), any(), any()))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.APPROVED)));

        List<BookingDto> result = bookingService.getAllByBooker(2L, "CURRENT");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldReturnPast() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndEndBeforeOrderByStartDesc(eq(2L), any()))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.APPROVED)));

        List<BookingDto> result = bookingService.getAllByBooker(2L, "PAST");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldReturnFuture() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStartAfterOrderByStartDesc(eq(2L), any()))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.WAITING)));

        List<BookingDto> result = bookingService.getAllByBooker(2L, "FUTURE");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldReturnWaiting() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(2L, BookingStatus.WAITING))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.WAITING)));

        List<BookingDto> result = bookingService.getAllByBooker(2L, "WAITING");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldReturnRejected() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(2L, BookingStatus.REJECTED))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.REJECTED)));

        List<BookingDto> result = bookingService.getAllByBooker(2L, "REJECTED");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByBooker_shouldThrowValidation_whenUnknownState() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));

        assertThatThrownBy(() -> bookingService.getAllByBooker(2L, "UNSUPPORTED"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void getAllByBooker_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getAllByBooker(999L, "ALL"))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- getAllByOwner ----------

    @Test
    void getAllByOwner_shouldReturnAll() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdOrderByStartDesc(1L))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.WAITING)));

        List<BookingDto> result = bookingService.getAllByOwner(1L, "ALL");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldReturnCurrent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(eq(1L), any(), any()))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.APPROVED)));

        List<BookingDto> result = bookingService.getAllByOwner(1L, "CURRENT");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldReturnPast() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdAndEndBeforeOrderByStartDesc(eq(1L), any()))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.APPROVED)));

        List<BookingDto> result = bookingService.getAllByOwner(1L, "PAST");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldReturnFuture() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdAndStartAfterOrderByStartDesc(eq(1L), any()))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.WAITING)));

        List<BookingDto> result = bookingService.getAllByOwner(1L, "FUTURE");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldReturnWaiting() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdAndStatusOrderByStartDesc(1L, BookingStatus.WAITING))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.WAITING)));

        List<BookingDto> result = bookingService.getAllByOwner(1L, "WAITING");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldReturnRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findAllByItemOwnerIdAndStatusOrderByStartDesc(1L, BookingStatus.REJECTED))
                .thenReturn(List.of(savedBooking(1L, BookingStatus.REJECTED)));

        List<BookingDto> result = bookingService.getAllByOwner(1L, "REJECTED");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldThrowValidation_whenUnknownState() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> bookingService.getAllByOwner(1L, "UNSUPPORTED"))
                .isInstanceOf(ValidationException.class);
    }
}