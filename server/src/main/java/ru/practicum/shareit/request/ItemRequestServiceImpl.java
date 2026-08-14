package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestAnswerDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestRequest;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository itemRequestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, NewItemRequestRequest request) {
        User requestor = findUserOrThrow(userId);

        ItemRequest itemRequest = ItemRequestMapper.toItemRequest(request, requestor);
        ItemRequest saved = itemRequestRepository.save(itemRequest);

        log.info("Пользователь {} создал запрос вещи с id {}", userId, saved.getId());
        return ItemRequestMapper.toItemRequestDto(saved, List.of());
    }

    @Override
    public List<ItemRequestDto> getOwn(Long userId) {
        findUserOrThrow(userId);

        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(userId);
        return toDtosWithAnswers(requests);
    }

    @Override
    public List<ItemRequestDto> getAll(Long userId) {
        findUserOrThrow(userId);

        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdNotOrderByCreatedDesc(userId);
        return toDtosWithAnswers(requests);
    }

    @Override
    public ItemRequestDto getById(Long userId, Long requestId) {
        findUserOrThrow(userId);
        ItemRequest itemRequest = findItemRequestOrThrow(requestId);

        List<ItemRequestAnswerDto> answers = itemRepository.findAllByRequestId(requestId).stream()
                .map(ItemRequestMapper::toItemRequestAnswerDto)
                .toList();

        return ItemRequestMapper.toItemRequestDto(itemRequest, answers);
    }

    private List<ItemRequestDto> toDtosWithAnswers(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        List<Long> requestIds = requests.stream().map(ItemRequest::getId).toList();

        Map<Long, List<ItemRequestAnswerDto>> answersByRequest = itemRepository
                .findAllByRequestIdIn(requestIds).stream()
                .collect(Collectors.groupingBy(
                        item -> item.getRequest().getId(),
                        Collectors.mapping(ItemRequestMapper::toItemRequestAnswerDto, Collectors.toList())
                ));

        return requests.stream()
                .map(request -> ItemRequestMapper.toItemRequestDto(
                        request,
                        answersByRequest.getOrDefault(request.getId(), List.of())
                ))
                .toList();
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + userId + " не найден"
                ));
    }

    private ItemRequest findItemRequestOrThrow(Long requestId) {
        return itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(
                        "Запрос с id " + requestId + " не найден"
                ));
    }
}