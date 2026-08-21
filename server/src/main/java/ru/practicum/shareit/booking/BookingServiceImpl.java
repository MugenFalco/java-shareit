package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingDto create(Long userId, NewBookingRequest request) {
        User booker = findUserOrThrow(userId);
        Item item = findItemOrThrow(request.getItemId());

        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new ValidationException("Вещь недоступна для бронирования");
        }

        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Владелец не может забронировать свою вещь");
        }

        if (bookingRepository.existsApprovedOverlap(item.getId(), request.getStart(), request.getEnd())) {
            throw new ValidationException("Вещь уже забронирована на выбранные даты");
        }

        if (!request.getEnd().isAfter(request.getStart())) {
            throw new ValidationException(
                    "Дата окончания должна быть позже даты начала"
            );
        }

        Booking booking = BookingMapper.toBooking(request, item, booker);
        Booking saved = bookingRepository.save(booking);

        log.info("Пользователь {} создал бронирование {}", userId, saved.getId());
        return BookingMapper.toBookingDto(saved);
    }

    @Override
    @Transactional
    public BookingDto approve(Long userId, Long bookingId, Boolean approved) {
        Booking booking = findBookingOrThrow(bookingId);

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ForbiddenException(
                    "Подтвердить бронирование может только владелец вещи"
            );
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException(
                    "Бронирование уже обработано"
            );
        }

        booking.setStatus(
                Boolean.TRUE.equals(approved)
                        ? BookingStatus.APPROVED
                        : BookingStatus.REJECTED
        );

        log.info("Бронирование {} обработано владельцем {}", bookingId, userId);
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);

        if (!isBooker && !isOwner) {
            throw new NotFoundException(
                    "Бронирование доступно только автору или владельцу вещи"
            );
        }

        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getAllByBooker(Long userId, String state) {
        findUserOrThrow(userId);
        BookingState bookingState = parseState(state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (bookingState) {
            case ALL -> bookingRepository.findAllByBookerIdOrderByStartDesc(userId);
            case CURRENT -> bookingRepository
                    .findAllByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(userId, now, now);
            case PAST -> bookingRepository
                    .findAllByBookerIdAndEndBeforeOrderByStartDesc(userId, now);
            case FUTURE -> bookingRepository
                    .findAllByBookerIdAndStartAfterOrderByStartDesc(userId, now);
            case WAITING -> bookingRepository
                    .findAllByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository
                    .findAllByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
        };

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    @Override
    public List<BookingDto> getAllByOwner(Long userId, String state) {
        findUserOrThrow(userId);
        BookingState bookingState = parseState(state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (bookingState) {
            case ALL -> bookingRepository.findAllByItemOwnerIdOrderByStartDesc(userId);
            case CURRENT -> bookingRepository
                    .findAllByItemOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(userId, now, now);
            case PAST -> bookingRepository
                    .findAllByItemOwnerIdAndEndBeforeOrderByStartDesc(userId, now);
            case FUTURE -> bookingRepository
                    .findAllByItemOwnerIdAndStartAfterOrderByStartDesc(userId, now);
            case WAITING -> bookingRepository
                    .findAllByItemOwnerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository
                    .findAllByItemOwnerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
        };

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    private BookingState parseState(String state) {
        return BookingState.from(state)
                .orElseThrow(() -> new ValidationException("Unknown state: " + state));
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

    private Booking findBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(
                        "Бронирование с id " + bookingId + " не найдено"
                ));
    }
}