package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.item.dto.NewCommentRequestDto;
import ru.practicum.shareit.item.dto.NewItemRequestDto;
import ru.practicum.shareit.item.dto.UpdateItemRequestDto;

@RestController
@RequestMapping(path = "/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    private final ItemClient itemClient;

    @PostMapping
    public ResponseEntity<Object> create(@RequestHeader(USER_ID_HEADER) long userId,
                                         @Valid @RequestBody NewItemRequestDto requestDto) {
        log.info("Creating item {}, userId={}", requestDto, userId);
        return itemClient.create(userId, requestDto);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<Object> addComment(@RequestHeader(USER_ID_HEADER) long userId,
                                             @PathVariable Long itemId,
                                             @Valid @RequestBody NewCommentRequestDto requestDto) {
        log.info("Adding comment to item {}, userId={}", itemId, userId);
        return itemClient.addComment(userId, itemId, requestDto);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<Object> update(@RequestHeader(USER_ID_HEADER) long userId,
                                         @PathVariable Long itemId,
                                         @RequestBody UpdateItemRequestDto requestDto) {
        log.info("Updating item {}, userId={}", itemId, userId);
        return itemClient.update(userId, itemId, requestDto);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<Object> getById(@RequestHeader(USER_ID_HEADER) long userId,
                                          @PathVariable Long itemId) {
        log.info("Get item {}, userId={}", itemId, userId);
        return itemClient.getById(userId, itemId);
    }

    @GetMapping
    public ResponseEntity<Object> getAllByOwner(@RequestHeader(USER_ID_HEADER) long userId) {
        log.info("Get items of owner {}", userId);
        return itemClient.getAllByOwner(userId);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam String text) {
        log.info("Search items by text '{}'", text);
        return itemClient.search(text);
    }
}