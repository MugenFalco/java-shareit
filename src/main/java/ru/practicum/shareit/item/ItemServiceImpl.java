package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = findUserOrThrow(userId);

        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);

        Item saved = itemRepository.save(item);
        log.info("Пользователь {} добавил вещь с id {}", userId, saved.getId());
        return ItemMapper.toItemDto(saved);
    }

    @Override
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        findUserOrThrow(userId);
        Item item = findItemOrThrow(itemId);

        if (!item.getOwner().getId().equals(userId)) {
            throw new ForbiddenException(
                    "Пользователь с id " + userId + " не является владельцем вещи"
            );
        }

        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        log.info("Обновлена вещь с id {}", itemId);
        return ItemMapper.toItemDto(itemRepository.update(item));
    }

    @Override
    public ItemDto getById(Long itemId) {
        return ItemMapper.toItemDto(findItemOrThrow(itemId));
    }

    @Override
    public List<ItemDto> getAllByOwner(Long userId) {
        findUserOrThrow(userId);

        return itemRepository.findAllByOwnerId(userId).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
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
}