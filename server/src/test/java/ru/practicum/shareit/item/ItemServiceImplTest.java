package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
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
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemServiceImplTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User(null, "Владелец", "owner@test.ru"));
        otherUser = userRepository.save(new User(null, "Другой пользователь", "other@test.ru"));
    }

    private Item saveItem() {
        Item item = new Item();
        item.setName("Дрель");
        item.setDescription("Мощная дрель");
        item.setAvailable(true);
        item.setOwner(owner);
        return itemRepository.save(item);
    }

    // ---------- create ----------

    @Test
    void create_shouldCreateItemWithoutRequest() {
        NewItemRequest request = new NewItemRequest("Дрель", "Мощная дрель", true, null);

        ItemDto result = itemService.create(owner.getId(), request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Дрель");
        assertThat(result.getRequestId()).isNull();
    }

    @Test
    void create_shouldLinkItemToRequest_whenRequestIdProvided() {
        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setDescription("Нужна дрель");
        itemRequest.setRequestor(otherUser);
        itemRequest.setCreated(LocalDateTime.now());
        itemRequest = itemRequestRepository.save(itemRequest);

        NewItemRequest request = new NewItemRequest("Дрель", "Мощная дрель", true, itemRequest.getId());

        ItemDto result = itemService.create(owner.getId(), request);

        assertThat(result.getRequestId()).isEqualTo(itemRequest.getId());
    }

    @Test
    void create_shouldThrowNotFound_whenRequestDoesNotExist() {
        NewItemRequest request = new NewItemRequest("Дрель", "Мощная дрель", true, 999L);

        assertThatThrownBy(() -> itemService.create(owner.getId(), request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_shouldThrowNotFound_whenUserDoesNotExist() {
        NewItemRequest request = new NewItemRequest("Дрель", "Мощная дрель", true, null);

        assertThatThrownBy(() -> itemService.create(999L, request))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- update ----------

    @Test
    void update_shouldUpdateAllFields() {
        Item item = saveItem();
        UpdateItemRequest request = new UpdateItemRequest("Новое имя", "Новое описание", false);

        ItemDto result = itemService.update(owner.getId(), item.getId(), request);

        assertThat(result.getName()).isEqualTo("Новое имя");
        assertThat(result.getDescription()).isEqualTo("Новое описание");
        assertThat(result.getAvailable()).isFalse();
    }

    @Test
    void update_shouldIgnoreBlankFields() {
        Item item = saveItem();
        UpdateItemRequest request = new UpdateItemRequest("", null, null);

        ItemDto result = itemService.update(owner.getId(), item.getId(), request);

        assertThat(result.getName()).isEqualTo("Дрель");
    }

    @Test
    void update_shouldThrowForbidden_whenNotOwner() {
        Item item = saveItem();
        UpdateItemRequest request = new UpdateItemRequest("Новое имя", null, null);

        assertThatThrownBy(() -> itemService.update(otherUser.getId(), item.getId(), request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_shouldThrowNotFound_whenItemDoesNotExist() {
        UpdateItemRequest request = new UpdateItemRequest("Новое имя", null, null);

        assertThatThrownBy(() -> itemService.update(owner.getId(), 999L, request))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- getById ----------

    @Test
    void getById_shouldIncludeBookings_whenOwner() {
        Item item = saveItem();
        Booking past = new Booking(null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1),
                item, otherUser, BookingStatus.APPROVED);
        bookingRepository.save(past);

        ItemWithBookingsDto result = itemService.getById(owner.getId(), item.getId());

        assertThat(result.getLastBooking()).isNotNull();
    }

    @Test
    void getById_shouldNotIncludeBookings_whenNotOwner() {
        Item item = saveItem();
        Booking past = new Booking(null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1),
                item, otherUser, BookingStatus.APPROVED);
        bookingRepository.save(past);

        ItemWithBookingsDto result = itemService.getById(otherUser.getId(), item.getId());

        assertThat(result.getLastBooking()).isNull();
    }

    @Test
    void getById_shouldThrowNotFound_whenItemDoesNotExist() {
        assertThatThrownBy(() -> itemService.getById(owner.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- getAllByOwner ----------

    @Test
    void getAllByOwner_shouldReturnItemsWithBookingsAndComments() {
        Item item = saveItem();
        Booking future = new Booking(null,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                item, otherUser, BookingStatus.APPROVED);
        bookingRepository.save(future);

        List<ItemWithBookingsDto> result = itemService.getAllByOwner(owner.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNextBooking()).isNotNull();
    }

    @Test
    void getAllByOwner_shouldReturnEmptyList_whenNoItems() {
        List<ItemWithBookingsDto> result = itemService.getAllByOwner(owner.getId());

        assertThat(result).isEmpty();
    }

    // ---------- addComment ----------

    @Test
    void addComment_shouldAddComment_whenBookingCompleted() {
        Item item = saveItem();
        Booking past = new Booking(null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1),
                item, otherUser, BookingStatus.APPROVED);
        bookingRepository.save(past);

        NewCommentRequest request = new NewCommentRequest("Отличная вещь!");
        CommentDto result = itemService.addComment(otherUser.getId(), item.getId(), request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getText()).isEqualTo("Отличная вещь!");
    }

    @Test
    void addComment_shouldThrowValidation_whenNoCompletedBooking() {
        Item item = saveItem();
        NewCommentRequest request = new NewCommentRequest("Отличная вещь!");

        assertThatThrownBy(() -> itemService.addComment(otherUser.getId(), item.getId(), request))
                .isInstanceOf(ValidationException.class);
    }

    // ---------- search ----------

    @Test
    void search_shouldReturnMatchingItems() {
        saveItem();

        List<ItemDto> result = itemService.search("дрель");

        assertThat(result).hasSize(1);
    }

    @Test
    void search_shouldReturnEmptyList_whenNoMatches() {
        saveItem();

        List<ItemDto> result = itemService.search("шуруповёрт");

        assertThat(result).isEmpty();
    }
}