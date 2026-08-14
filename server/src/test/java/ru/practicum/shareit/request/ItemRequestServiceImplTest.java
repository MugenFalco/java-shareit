package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestRequest;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemRequestServiceImplTest {

    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    private User requestor;
    private User otherUser;

    @BeforeEach
    void setUp() {
        requestor = userRepository.save(new User(null, "Автор запроса", "requestor@test.ru"));
        otherUser = userRepository.save(new User(null, "Другой пользователь", "other@test.ru"));
    }

    @Test
    void create_shouldSaveRequestWithCurrentTimestampAndEmptyAnswers() {
        NewItemRequestRequest request = new NewItemRequestRequest("Нужна дрель");

        ItemRequestDto result = itemRequestService.create(requestor.getId(), request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getCreated()).isNotNull();
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void create_shouldThrowNotFound_whenUserDoesNotExist() {
        NewItemRequestRequest request = new NewItemRequestRequest("Нужна дрель");

        assertThatThrownBy(() -> itemRequestService.create(999L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOwn_shouldReturnRequestsWithAnswers_sortedNewestFirst() {
        ItemRequestDto older = itemRequestService.create(requestor.getId(), new NewItemRequestRequest("Нужна дрель"));
        ItemRequestDto newer = itemRequestService.create(requestor.getId(), new NewItemRequestRequest("Нужна лестница"));

        ItemRequest olderRequest = itemRequestRepository.findById(older.getId()).orElseThrow();

        Item answerItem = new Item();
        answerItem.setName("Дрель Bosch");
        answerItem.setDescription("Хорошая дрель");
        answerItem.setAvailable(true);
        answerItem.setOwner(otherUser);
        answerItem.setRequest(olderRequest);
        itemRepository.save(answerItem);

        List<ItemRequestDto> result = itemRequestService.getOwn(requestor.getId());

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo(newer.getId());
        assertThat(result.get(0).getItems()).isEmpty();

        assertThat(result.get(1).getId()).isEqualTo(older.getId());
        assertThat(result.get(1).getItems()).hasSize(1);
        assertThat(result.get(1).getItems().getFirst().getItemId()).isEqualTo(answerItem.getId());
        assertThat(result.get(1).getItems().getFirst().getName()).isEqualTo("Дрель Bosch");
        assertThat(result.get(1).getItems().getFirst().getOwnerId()).isEqualTo(otherUser.getId());
    }

    @Test
    void getAll_shouldNotIncludeOwnRequests() {
        itemRequestService.create(requestor.getId(), new NewItemRequestRequest("Нужна дрель"));

        List<ItemRequestDto> resultForRequestor = itemRequestService.getAll(requestor.getId());
        List<ItemRequestDto> resultForOther = itemRequestService.getAll(otherUser.getId());

        assertThat(resultForRequestor).isEmpty();
        assertThat(resultForOther).hasSize(1);
    }

    @Test
    void getById_shouldThrowNotFound_whenRequestDoesNotExist() {
        assertThatThrownBy(() -> itemRequestService.getById(requestor.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }
}