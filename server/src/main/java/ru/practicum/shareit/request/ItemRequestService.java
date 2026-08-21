package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestRequest;

import java.util.List;

public interface ItemRequestService {

    ItemRequestDto create(Long userId, NewItemRequestRequest request);

    List<ItemRequestDto> getOwn(Long userId);

    List<ItemRequestDto> getAll(Long userId);

    ItemRequestDto getById(Long userId, Long requestId);
}