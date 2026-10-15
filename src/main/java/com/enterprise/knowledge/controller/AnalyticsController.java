
package com.enterprise.knowledge.controller;

import com.enterprise.knowledge.dto.AnalyticsResponse;
import com.enterprise.knowledge.dto.RoleAccessReport;
import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/knowledge/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(analyticsService.getAnalytics());
    }

    @GetMapping("/access")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleAccessReport> getAccessibleDocuments(
            @RequestParam AppUser.Role role) {
        return ResponseEntity.ok(analyticsService.getAccessibleDocuments(role));
    }
}
