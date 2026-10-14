package com.enterprise.knowledge.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsBlankQueriesAndOutOfRangePagination() {
        SearchRequest request = new SearchRequest();
        request.setQuery(" ");
        request.setPage(-1);
        request.setSize(51);

        var violations = validator.validate(request);

        assertEquals(4, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("query")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("page")));
        assertEquals(2, violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("query"))
                .count());
        assertEquals(1, violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("size"))
                .count());
    }
}
