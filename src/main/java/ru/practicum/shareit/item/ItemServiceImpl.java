package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto create(Long userId, NewItemRequest request) {
        User owner = findUserOrThrow(userId);

        Item item = ItemMapper.toItem(request);
        item.setOwner(owner);

        Item saved = itemRepository.save(item);
        log.info("Пользователь {} добавил вещь с id {}", userId, saved.getId());
        return ItemMapper.toItemDto(saved);
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, UpdateItemRequest request) {
        findUserOrThrow(userId);
        Item item = findItemOrThrow(itemId);

        if (!item.getOwner().getId().equals(userId)) {
            throw new ForbiddenException(
                    "Пользователь с id " + userId + " не является владельцем вещи"
            );
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            item.setName(request.getName());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            item.setDescription(request.getDescription());
        }
        if (request.getAvailable() != null) {
            item.setAvailable(request.getAvailable());
        }

        log.info("Обновлена вещь с id {}", itemId);
        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemWithBookingsDto getById(Long userId, Long itemId) {
        Item item = findItemOrThrow(itemId);
        ItemWithBookingsDto dto = ItemMapper.toItemWithBookingsDto(item);

        if (item.getOwner().getId().equals(userId)) {
            fillBookings(dto, bookingRepository.findAllByItemIdOrderByStartAsc(itemId));
        }

        dto.setComments(
                commentRepository.findAllByItemId(itemId).stream()
                        .map(CommentMapper::toCommentDto)
                        .toList()
        );

        return dto;
    }

    @Override
    public List<ItemWithBookingsDto> getAllByOwner(Long userId) {
        findUserOrThrow(userId);

        List<Item> items = itemRepository.findAllByOwnerId(userId);
        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = items.stream().map(Item::getId).toList();

        Map<Long, List<Booking>> bookingsByItem = bookingRepository
                .findAllByItemIdInOrderByStartAsc(itemIds).stream()
                .collect(Collectors.groupingBy(booking -> booking.getItem().getId()));

        Map<Long, List<CommentDto>> commentsByItem = commentRepository
                .findAllByItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toCommentDto, Collectors.toList())
                ));

        return items.stream()
                .map(item -> {
                    ItemWithBookingsDto dto = ItemMapper.toItemWithBookingsDto(item);
                    fillBookings(dto, bookingsByItem.getOrDefault(item.getId(), List.of()));
                    dto.setComments(commentsByItem.getOrDefault(item.getId(), List.of()));
                    return dto;
                })
                .toList();
    }

    private void fillBookings(ItemWithBookingsDto dto, List<Booking> bookings) {
        LocalDateTime now = LocalDateTime.now();

        Booking last = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.APPROVED)
                .filter(booking -> !booking.getStart().isAfter(now))
                .reduce((first, second) -> second)
                .orElse(null);

        Booking next = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.APPROVED)
                .filter(booking -> booking.getStart().isAfter(now))
                .findFirst()
                .orElse(null);

        dto.setLastBooking(last != null ? ItemMapper.toBookingShortDto(last) : null);
        dto.setNextBooking(next != null ? ItemMapper.toBookingShortDto(next) : null);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + userId + " не найден"
                ));
    }

    private Item findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException(
                        "Вещь с id " + itemId + " не найдена"
                ));
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, NewCommentRequest request) {
        User author = findUserOrThrow(userId);
        Item item = findItemOrThrow(itemId);

        boolean hasCompletedBooking = bookingRepository
                .existsCompletedBooking(itemId, userId, LocalDateTime.now());

        if (!hasCompletedBooking) {
            throw new ValidationException(
                    "Оставить отзыв можно только после завершённой аренды"
            );
        }

        Comment comment = new Comment();
        comment.setText(request.getText());
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);
        log.info("Пользователь {} оставил отзыв на вещь {}", userId, itemId);
        return CommentMapper.toCommentDto(saved);
    }

    @Override
    public List<ItemDto> search(String text) {
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }
}