
package com.enterprise.knowledge.entity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "access_policy", uniqueConstraints = @UniqueConstraint(columnNames = { "role", "access_level" }))
@Getter
@Setter
@Builder
public class AccessPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppUser.Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 20)
    private KnowledgeDocument.AccessLevel accessLevel;

    // Required by JPA
    public AccessPolicy() {
    }

    // Explicit all-arguments constructor
    public AccessPolicy(
            Long id,
            AppUser.Role role,
            KnowledgeDocument.AccessLevel accessLevel) {
        this.id = id;
        this.role = role;
        this.accessLevel = accessLevel;
    }
}
