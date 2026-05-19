package io.readyplz.readyplz.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RestApiExceptionHandlerTest {

    private final RestApiExceptionHandler handler = new RestApiExceptionHandler();

    @Test
    void handleIllegalArgument_returnsBadRequestWithoutInternalDetails() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("duplicate_nickname"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "duplicate_nickname");
    }

    @Test
    void handleGeneral_returnsGenericMessage() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneral(new RuntimeException("db connection leak"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "서버 오류가 발생했습니다.");
    }
}
