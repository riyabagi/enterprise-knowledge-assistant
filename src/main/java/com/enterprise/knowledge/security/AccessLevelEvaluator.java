
package com.enterprise.knowledge.security;

import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.entity.KnowledgeDocument.AccessLevel;
import com.enterprise.knowledge.repository.AccessPolicyRepository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccessLevelEvaluator {

    private final AccessPolicyRepository accessPolicyRepository;

    // Explicit constructor replaces @RequiredArgsConstructor
    public AccessLevelEvaluator(
            AccessPolicyRepository accessPolicyRepository) {
        this.accessPolicyRepository = accessPolicyRepository;
    }

    /**
     * Returns the AccessLevel values the given role is permitted to see.
     * Permissions are resolved from the access_policy table.
     */
    public List<AccessLevel> getAllowedLevels(AppUser.Role role) {
        return accessPolicyRepository.findByRole(role)
                .stream()
                .map(policy -> policy.getAccessLevel())
                .collect(Collectors.toList());
    }

    /**
     * Convenience overload accepting an authority string,
     * such as ROLE_ADMIN.
     */
    public List<AccessLevel> getAllowedLevels(String roleAuthority) {
        String roleName = roleAuthority.startsWith("ROLE_")
                ? roleAuthority.substring(5)
                : roleAuthority;

        return getAllowedLevels(AppUser.Role.valueOf(roleName));
    }
}
