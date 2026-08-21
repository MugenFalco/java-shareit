package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestRequest;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemRequestServiceImpl itemRequestService;

    private User requestor;

    @BeforeEach
    void setUp() {
        requestor = new User(1L, "Автор запроса", "requestor@test.ru");
    }

    @Test
    void create_shouldSaveRequestWithCurrentTimestampAndEmptyAnswers() {
        NewItemRequestRequest request = new NewItemRequestRequest("Нужна дрель");

        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));
        when(itemRequestRepository.save(any(ItemRequest.class))).thenAnswer(invocation -> {
            ItemRequest saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ItemRequestDto result = itemRequestService.create(1L, request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getCreated()).isNotNull();
        assertThat(result.getItems()).isEmpty();
        verify(itemRequestRepository).save(any(ItemRequest.class));
    }

    @Test
    void create_shouldThrowNotFound_whenUserDoesNotExist() {
        NewItemRequestRequest request = new NewItemRequestRequest("Нужна дрель");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemRequestService.create(999L, request))
                .isInstanceOf(NotFoundException.class);
        verify(itemRequestRepository, never()).save(any());
    }

    @Test
    void getOwn_shouldReturnRequestsWithAnswers_sortedNewestFirst() {
        ItemRequest older = new ItemRequest(1L, "Нужна дрель", requestor, LocalDateTime.now().minusHours(1));
        ItemRequest newer = new ItemRequest(2L, "Нужна лестница", requestor, LocalDateTime.now());

        Item answerItem = new Item();
        answerItem.setId(100L);
        answerItem.setName("Дрель Bosch");
        answerItem.setRequest(older);
        User owner = new User(2L, "Владелец дрели", "owner@test.ru");
        answerItem.setOwner(owner);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));
        when(itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(1L))
                .thenReturn(List.of(newer, older));
        when(itemRepository.findAllByRequestIdIn(List.of(2L, 1L)))
                .thenReturn(List.of(answerItem));

        List<ItemRequestDto> result = itemRequestService.getOwn(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(0).getItems()).isEmpty();
        assertThat(result.get(1).getId()).isEqualTo(1L);
        assertThat(result.get(1).getItems()).hasSize(1);
        assertThat(result.get(1).getItems().getFirst().getItemId()).isEqualTo(100L);
        assertThat(result.get(1).getItems().getFirst().getName()).isEqualTo("Дрель Bosch");
        assertThat(result.get(1).getItems().getFirst().getOwnerId()).isEqualTo(2L);
    }

    @Test
    void getOwn_shouldReturnEmptyList_whenNoRequests() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));
        when(itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(1L))
                .thenReturn(List.of());

        List<ItemRequestDto> result = itemRequestService.getOwn(1L);

        assertThat(result).isEmpty();
        verify(itemRepository, never()).findAllByRequestIdIn(any());
    }

    @Test
    void getAll_shouldReturnOtherUsersRequests() {
        ItemRequest other = new ItemRequest(3L, "Нужна дрель", new User(2L, "Другой", "other@test.ru"),
                LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));
        when(itemRequestRepository.findAllByRequestorIdNotOrderByCreatedDesc(1L))
                .thenReturn(List.of(other));
        when(itemRepository.findAllByRequestIdIn(List.of(3L)))
                .thenReturn(List.of());

        List<ItemRequestDto> result = itemRequestService.getAll(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(3L);
    }

    @Test
    void getById_shouldReturnRequestWithAnswers() {
        ItemRequest itemRequest = new ItemRequest(1L, "Нужна дрель", requestor, LocalDateTime.now());
        Item answerItem = new Item();
        answerItem.setId(100L);
        answerItem.setName("Дрель Bosch");
        User owner = new User(2L, "Владелец", "owner@test.ru");
        answerItem.setOwner(owner);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));
        when(itemRequestRepository.findById(1L)).thenReturn(Optional.of(itemRequest));
        when(itemRepository.findAllByRequestId(1L)).thenReturn(List.of(answerItem));

        ItemRequestDto result = itemRequestService.getById(1L, 1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getItemId()).isEqualTo(100L);
    }

    @Test
    void getById_shouldThrowNotFound_whenRequestDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));
        when(itemRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemRequestService.getById(1L, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemRequestService.getById(999L, 1L))
                .isInstanceOf(NotFoundException.class);
        verify(itemRequestRepository, never()).findById(anyLong());
    }
}