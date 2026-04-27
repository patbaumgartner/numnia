package ch.numnia.iam.api.dto;

import jakarta.validation.constraints.*;

/**
 * Request body for {@code POST /api/parents/{parentId}/child-profiles}.
 */
public record CreateChildProfileRequest(

        @NotBlank(message = "Fantasy name is required")
        String pseudonym,

        @NotNull(message = "Year of birth is required")
        @Min(value = 1900, message = "Invalid year of birth")
        Integer yearOfBirth,

        @NotBlank(message = "Avatar base model is required")
        String avatarBaseModel
) {}
