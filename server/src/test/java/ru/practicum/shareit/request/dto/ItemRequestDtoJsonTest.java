package ru.practicum.shareit.request.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestDtoJsonTest {

    @Autowired
    private JacksonTester<ItemRequestDto> json;

    @Test
    void serialize_shouldWriteCreatedAsIsoString() throws Exception {
        LocalDateTime created = LocalDateTime.of(2026, 8, 14, 12, 30, 0);
        ItemRequestAnswerDto answer = new ItemRequestAnswerDto(10L, "Дрель Bosch", 2L);
        ItemRequestDto dto = new ItemRequestDto(1L, "Нужна дрель", created, List.of(answer));

        var result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Нужна дрель");
        assertThat(result).extractingJsonPathStringValue("$.created").isEqualTo("2026-08-14T12:30:00");
        assertThat(result).extractingJsonPathNumberValue("$.items[0].itemId").isEqualTo(10);
        assertThat(result).extractingJsonPathStringValue("$.items[0].name").isEqualTo("Дрель Bosch");
        assertThat(result).extractingJsonPathNumberValue("$.items[0].ownerId").isEqualTo(2);
    }

    @Test
    void deserialize_shouldParseIsoStringBackToLocalDateTime() throws Exception {
        String content = "{\"id\":1,\"description\":\"Нужна дрель\",\"created\":\"2026-08-14T12:30:00\",\"items\":[]}";

        ItemRequestDto result = json.parseObject(content);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getCreated()).isEqualTo(LocalDateTime.of(2026, 8, 14, 12, 30, 0));
        assertThat(result.getItems()).isEmpty();
    }
}