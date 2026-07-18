package com.library.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Custom Exceptions Tests")
class ExceptionsTest {

    @Test
    @DisplayName("BadRequestException stores message only")
    void badRequestException_messageOnly() {
        BadRequestException ex = new BadRequestException("bad request");
        assertThat(ex.getMessage()).isEqualTo("bad request");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("BadRequestException stores message and cause")
    void badRequestException_messageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        BadRequestException ex = new BadRequestException("bad request", cause);
        assertThat(ex.getMessage()).isEqualTo("bad request");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("ResourceNotFoundException stores message only")
    void resourceNotFoundException_messageOnly() {
        ResourceNotFoundException ex = new ResourceNotFoundException("not found");
        assertThat(ex.getMessage()).isEqualTo("not found");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("ResourceNotFoundException stores message and cause")
    void resourceNotFoundException_messageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        ResourceNotFoundException ex = new ResourceNotFoundException("not found", cause);
        assertThat(ex.getMessage()).isEqualTo("not found");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("UnauthorizedException stores message only")
    void unauthorizedException_messageOnly() {
        UnauthorizedException ex = new UnauthorizedException("unauthorized");
        assertThat(ex.getMessage()).isEqualTo("unauthorized");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("UnauthorizedException stores message and cause")
    void unauthorizedException_messageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        UnauthorizedException ex = new UnauthorizedException("unauthorized", cause);
        assertThat(ex.getMessage()).isEqualTo("unauthorized");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
