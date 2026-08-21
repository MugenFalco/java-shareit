package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.request.dto.ItemRequestAnswerDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemRequestService itemRequestService;

    @Test
    void create_shouldReturnCreatedRequest() throws Exception {
        NewItemRequestRequest request = new NewItemRequestRequest("Нужна дрель");
        ItemRequestDto response = new ItemRequestDto(1L, "Нужна дрель", LocalDateTime.now(), List.of());

        when(itemRequestService.create(1L, request)).thenReturn(response);

        mockMvc.perform(post("/requests")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Нужна дрель"))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void create_shouldReturnBadRequest_whenUserIdHeaderMissing() throws Exception {
        NewItemRequestRequest request = new NewItemRequestRequest("Нужна дрель");

        mockMvc.perform(post("/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOwn_shouldReturnListOfRequestsWithAnswers() throws Exception {
        ItemRequestAnswerDto answer = new ItemRequestAnswerDto(10L, "Дрель Bosch", 2L);
        ItemRequestDto request = new ItemRequestDto(1L, "Нужна дрель", LocalDateTime.now(), List.of(answer));

        when(itemRequestService.getOwn(1L)).thenReturn(List.of(request));

        mockMvc.perform(get("/requests").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].items[0].itemId").value(10))
                .andExpect(jsonPath("$[0].items[0].name").value("Дрель Bosch"))
                .andExpect(jsonPath("$[0].items[0].ownerId").value(2));
    }

    @Test
    void getAll_shouldCallServiceAndReturnList() throws Exception {
        when(itemRequestService.getAll(1L)).thenReturn(List.of());

        mockMvc.perform(get("/requests/all").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(itemRequestService).getAll(1L);
    }

    @Test
    void getById_shouldReturnNotFound_whenRequestDoesNotExist() throws Exception {
        when(itemRequestService.getById(anyLong(), any()))
                .thenThrow(new NotFoundException("Запрос с id 999 не найден"));

        mockMvc.perform(get("/requests/999").header(USER_ID_HEADER, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Запрос с id 999 не найден"));
    }
}