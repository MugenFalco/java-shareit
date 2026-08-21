package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.NewBookingRequest;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private BookingDto sampleBookingDto() {
        return new BookingDto(1L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                new ItemShortDto(1L, "Дрель"), new UserShortDto(2L), BookingStatus.WAITING);
    }

    @Test
    void create_shouldReturnCreatedBooking() throws Exception {
        NewBookingRequest request = new NewBookingRequest(1L,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        when(bookingService.create(1L, request)).thenReturn(sampleBookingDto());

        mockMvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void create_shouldReturnBadRequest_whenItemUnavailable() throws Exception {
        when(bookingService.create(anyLong(), any()))
                .thenThrow(new ValidationException("Вещь недоступна для бронирования"));

        NewBookingRequest request = new NewBookingRequest(1L,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approve_shouldReturnUpdatedBooking() throws Exception {
        BookingDto approved = sampleBookingDto();
        approved.setStatus(BookingStatus.APPROVED);

        when(bookingService.approve(1L, 1L, true)).thenReturn(approved);

        mockMvc.perform(patch("/bookings/1?approved=true")
                        .header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approve_shouldReturnForbidden_whenNotOwner() throws Exception {
        when(bookingService.approve(anyLong(), anyLong(), anyBoolean()))
                .thenThrow(new ForbiddenException("Подтвердить бронирование может только владелец вещи"));

        mockMvc.perform(patch("/bookings/1?approved=true")
                        .header(USER_ID_HEADER, 2L))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_shouldReturnBooking() throws Exception {
        when(bookingService.getById(1L, 1L)).thenReturn(sampleBookingDto());

        mockMvc.perform(get("/bookings/1").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getAllByBooker_shouldReturnList() throws Exception {
        when(bookingService.getAllByBooker(1L, "ALL")).thenReturn(List.of(sampleBookingDto()));

        mockMvc.perform(get("/bookings?state=ALL").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllByOwner_shouldReturnList() throws Exception {
        when(bookingService.getAllByOwner(1L, "ALL")).thenReturn(List.of(sampleBookingDto()));

        mockMvc.perform(get("/bookings/owner?state=ALL").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}