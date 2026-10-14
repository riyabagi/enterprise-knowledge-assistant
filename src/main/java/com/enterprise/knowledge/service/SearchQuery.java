package com.enterprise.knowledge.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class SearchQuery {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "how", "in", "is", "of", "on", "or", "that", "the", "this",
            "to", "what", "when", "where", "which", "who", "why", "with");

    private SearchQuery() {
    }

    static List<String> terms(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return List.of();
        }

        return Arrays.stream(rawQuery.toLowerCase(Locale.ROOT)
                        .split("[^\\p{L}\\p{N}]+"))
                .filter(term -> term.length() >= 2)
                .filter(term -> !STOP_WORDS.contains(term))
                .distinct()
                .limit(10)
                .toList();
    }

    static String booleanMode(String rawQuery) {
        return terms(rawQuery).stream().collect(Collectors.joining(" "));
    }
}
