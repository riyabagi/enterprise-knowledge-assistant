package com.enterprise.knowledge.dto;

import com.enterprise.knowledge.entity.AppUser;

import java.util.List;

public record RoleAccessReport(
        AppUser.Role role,
        List<AccessibleDocumentSummary> documents) {
}
