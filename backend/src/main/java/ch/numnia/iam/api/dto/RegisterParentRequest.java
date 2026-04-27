package ch.numnia.iam.api.dto;

import jakarta.validation.constraints.*;

/**
 * Request body for {@code POST /api/parents} — parent registration.
 *
 * <p>Server-side Bean Validation enforced (NFR-SEC-001). All fields are
 * mandatory; password must be at least 8 characters.
 */
public record RegisterParentRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Salutation is required")
        String salutation,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @AssertTrue(message = "Privacy consent is required")
        boolean privacyConsented,

        @AssertTrue(message = "Terms acceptance is required")
        boolean termsAccepted
) {}
