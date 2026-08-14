package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestRequest;

import java.util.List;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class ItemRequestController {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    private final ItemRequestService itemRequestService;

    @PostMapping
    public ItemRequestDto create(@RequestHeader(USER_ID_HEADER) Long userId,
                                 @RequestBody NewItemRequestRequest request) {
        return itemRequestService.create(userId, request);
    }

    @GetMapping
    public List<ItemRequestDto> getOwn(@RequestHeader(USER_ID_HEADER) Long userId) {
        return itemRequestService.getOwn(userId);
    }

    @GetMapping("/all")
    public List<ItemRequestDto> getAll(@RequestHeader(USER_ID_HEADER) Long userId) {
        return itemRequestService.getAll(userId);
    }

    @GetMapping("/{requestId}")
    public ItemRequestDto getById(@RequestHeader(USER_ID_HEADER) Long userId,
                                  @PathVariable Long requestId) {
        return itemRequestService.getById(userId, requestId);
    }
}