package com.enterprise.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AskRequest {

    @NotBlank(message = "Question must not be blank")
    @Size(max = 500, message = "Question must not exceed 500 characters")
    private String question;
}
