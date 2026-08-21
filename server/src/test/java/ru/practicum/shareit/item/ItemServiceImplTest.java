package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.dto.NewCommentRequest;
import ru.practicum.shareit.item.dto.NewItemRequest;
import ru.practicum.shareit.item.dto.UpdateItemRequest;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.request.model.ItemRequest;
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
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private User owner;
    private User otherUser;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = new User(1L, "Владелец", "owner@test.ru");
        otherUser = new User(2L, "Другой пользователь", "other@test.ru");

        item = new Item();
        item.setId(10L);
        item.setName("Дрель");
        item.setDescription("Мощная дрель");
        item.setAvailable(true);
        item.setOwner(owner);
    }

    // ---------- create ----------

    @Test
    void create_shouldCreateItemWithoutRequest() {
        NewItemRequest request = new NewItemRequest("Дрель", "Мощная дрель", true, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ItemDto result = itemService.create(1L, request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Дрель");
        assertThat(result.getRequestId()).isNull();
        verify(itemRequestRepository, never()).findById(anyLong());
    }

    @Test
    void create_shouldLinkItemToRequest_whenRequestIdProvided() {
        ItemRequest itemRequest = new ItemRequest(5L, "Нужна дрель", otherUser, LocalDateTime.now());
        NewItemRequest request = new NewItemRequest("Дрель", "Мощная дрель", true, 5L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(5L)).thenReturn(Optional.of(itemRequest));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItemDto result = itemService.create(1L, request);

        assertThat(result.getRequestId()).isEqualTo(5L);
    }

    @Test
    void create_shouldThrowNotFound_whenRequestDoesNotExist() {
        NewItemRequest request = new NewItemRequest("Дрель", "Мощная дрель", true, 999L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.create(1L, request))
                .isInstanceOf(NotFoundException.class);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowNotFound_whenUserDoesNotExist() {
        NewItemRequest request = new NewItemRequest("Дрель", "Мощная дрель", true, null);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.create(999L, request))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- update ----------

    @Test
    void update_shouldUpdateAllFields() {
        UpdateItemRequest request = new UpdateItemRequest("Новое имя", "Новое описание", false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        ItemDto result = itemService.update(1L, 10L, request);

        assertThat(result.getName()).isEqualTo("Новое имя");
        assertThat(result.getDescription()).isEqualTo("Новое описание");
        assertThat(result.getAvailable()).isFalse();
    }

    @Test
    void update_shouldIgnoreBlankFields() {
        UpdateItemRequest request = new UpdateItemRequest("", null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        ItemDto result = itemService.update(1L, 10L, request);

        assertThat(result.getName()).isEqualTo("Дрель");
    }

    @Test
    void update_shouldThrowForbidden_whenNotOwner() {
        UpdateItemRequest request = new UpdateItemRequest("Новое имя", null, null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.update(2L, 10L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_shouldThrowNotFound_whenItemDoesNotExist() {
        UpdateItemRequest request = new UpdateItemRequest("Новое имя", null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.update(1L, 999L, request))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- getById ----------

    @Test
    void getById_shouldIncludeBookings_whenOwner() {
        Booking booking = new Booking(1L, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(1),
                item, otherUser, BookingStatus.APPROVED);

        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.findAllByItemIdAndStatusOrderByStartAsc(10L, BookingStatus.APPROVED))
                .thenReturn(List.of(booking));
        when(commentRepository.findAllByItemId(10L)).thenReturn(List.of());

        ItemWithBookingsDto result = itemService.getById(1L, 10L);

        assertThat(result.getLastBooking()).isNotNull();
    }

    @Test
    void getById_shouldNotIncludeBookings_whenNotOwner() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentRepository.findAllByItemId(10L)).thenReturn(List.of());

        ItemWithBookingsDto result = itemService.getById(2L, 10L);

        assertThat(result.getLastBooking()).isNull();
        verify(bookingRepository, never()).findAllByItemIdAndStatusOrderByStartAsc(anyLong(), any());
    }

    @Test
    void getById_shouldThrowNotFound_whenItemDoesNotExist() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getById(1L, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- getAllByOwner ----------

    @Test
    void getAllByOwner_shouldReturnItemsWithBookingsAndComments() {
        Booking booking = new Booking(1L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                item, otherUser, BookingStatus.APPROVED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findAllByOwnerId(1L)).thenReturn(List.of(item));
        when(bookingRepository.findAllByItemIdInAndStatusOrderByStartAsc(eq(List.of(10L)), eq(BookingStatus.APPROVED)))
                .thenReturn(List.of(booking));
        when(commentRepository.findAllByItemIdIn(List.of(10L))).thenReturn(List.of());

        List<ItemWithBookingsDto> result = itemService.getAllByOwner(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNextBooking()).isNotNull();
    }

    @Test
    void getAllByOwner_shouldReturnEmptyList_whenNoItems() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findAllByOwnerId(1L)).thenReturn(List.of());

        List<ItemWithBookingsDto> result = itemService.getAllByOwner(1L);

        assertThat(result).isEmpty();
        verify(bookingRepository, never()).findAllByItemIdInAndStatusOrderByStartAsc(any(), any());
    }

    // ---------- addComment ----------

    @Test
    void addComment_shouldAddComment_whenBookingCompleted() {
        NewCommentRequest request = new NewCommentRequest("Отличная вещь!");

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsCompletedBooking(eq(10L), eq(2L), any())).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        CommentDto result = itemService.addComment(2L, 10L, request);

        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getText()).isEqualTo("Отличная вещь!");
    }

    @Test
    void addComment_shouldThrowValidation_whenNoCompletedBooking() {
        NewCommentRequest request = new NewCommentRequest("Отличная вещь!");

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsCompletedBooking(eq(10L), eq(2L), any())).thenReturn(false);

        assertThatThrownBy(() -> itemService.addComment(2L, 10L, request))
                .isInstanceOf(ValidationException.class);
        verify(commentRepository, never()).save(any());
    }

    // ---------- search ----------

    @Test
    void search_shouldReturnMatchingItems() {
        when(itemRepository.search("дрель")).thenReturn(List.of(item));

        List<ItemDto> result = itemService.search("дрель");

        assertThat(result).hasSize(1);
    }

    @Test
    void search_shouldReturnEmptyList_whenNoMatches() {
        when(itemRepository.search("шуруповёрт")).thenReturn(List.of());

        List<ItemDto> result = itemService.search("шуруповёрт");

        assertThat(result).isEmpty();
    }
}