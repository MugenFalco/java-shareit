package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.dto.NewCommentRequest;
import ru.practicum.shareit.item.dto.NewItemRequest;
import ru.practicum.shareit.item.dto.UpdateItemRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    void create_shouldReturnCreatedItem() throws Exception {
        NewItemRequest request = new NewItemRequest("Дрель", "Мощная дрель", true, null);
        ItemDto response = new ItemDto(1L, "Дрель", "Мощная дрель", true, null);

        when(itemService.create(1L, request)).thenReturn(response);

        mockMvc.perform(post("/items")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Дрель"));
    }

    @Test
    void addComment_shouldReturnCreatedComment() throws Exception {
        NewCommentRequest request = new NewCommentRequest("Отличная вещь!");
        CommentDto response = new CommentDto(1L, "Отличная вещь!", "Иван", LocalDateTime.now());

        when(itemService.addComment(1L, 1L, request)).thenReturn(response);

        mockMvc.perform(post("/items/1/comment")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Отличная вещь!"));
    }

    @Test
    void addComment_shouldReturnBadRequest_whenNoCompletedBooking() throws Exception {
        when(itemService.addComment(anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("Оставить отзыв можно только после завершённой аренды"));

        mockMvc.perform(post("/items/1/comment")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewCommentRequest("Текст"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturnUpdatedItem() throws Exception {
        UpdateItemRequest request = new UpdateItemRequest("Новое имя", null, null);
        ItemDto response = new ItemDto(1L, "Новое имя", "Мощная дрель", true, null);

        when(itemService.update(1L, 1L, request)).thenReturn(response);

        mockMvc.perform(patch("/items/1")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Новое имя"));
    }

    @Test
    void getById_shouldReturnItem() throws Exception {
        ItemWithBookingsDto response = new ItemWithBookingsDto(1L, "Дрель", "Мощная дрель", true, null,
                null, null, List.of());

        when(itemService.getById(1L, 1L)).thenReturn(response);

        mockMvc.perform(get("/items/1").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getAllByOwner_shouldReturnList() throws Exception {
        ItemWithBookingsDto response = new ItemWithBookingsDto(1L, "Дрель", "Мощная дрель", true, null,
                null, null, List.of());

        when(itemService.getAllByOwner(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/items").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void search_shouldReturnMatchingItems() throws Exception {
        ItemDto response = new ItemDto(1L, "Дрель", "Мощная дрель", true, null);

        when(itemService.search("дрель")).thenReturn(List.of(response));

        mockMvc.perform(get("/items/search?text=дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void search_shouldReturnEmptyList_whenTextBlank() throws Exception {
        mockMvc.perform(get("/items/search?text="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}