package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The password policy itself is enforced by Keycloak when the password is set. */
@Schema(description = "Temporary password assigned by an administrator")
public record TemporaryPasswordRequest(
        @NotBlank(message = "رمز عبور موقت را وارد کنید")
        @Size(min = 8, max = 128, message = "رمز عبور باید بین ۸ تا ۱۲۸ نویسه باشد")
        String password) {
}
