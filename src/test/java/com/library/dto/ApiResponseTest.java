package com.library.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse Tests")
class ApiResponseTest {

    @Test
    @DisplayName("success(message, data) builds a successful response with data")
    void success_withData() {
        ApiResponse<String> response = ApiResponse.success("done", "payload");

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("done");
        assertThat(response.getData()).isEqualTo("payload");
        assertThat(response.getError()).isNull();
    }

    @Test
    @DisplayName("success(message) builds a successful response with no data")
    void success_messageOnly() {
        ApiResponse<Object> response = ApiResponse.success("done");

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("done");
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNull();
    }

    @Test
    @DisplayName("error(message, error) builds a failed response with error detail")
    void error_withDetail() {
        ApiResponse<Object> response = ApiResponse.error("failed", "detail");

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("failed");
        assertThat(response.getError()).isEqualTo("detail");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("error(message) builds a failed response with no error detail")
    void error_messageOnly() {
        ApiResponse<Object> response = ApiResponse.error("failed");

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("failed");
        assertThat(response.getError()).isNull();
        assertThat(response.getData()).isNull();
    }
}
