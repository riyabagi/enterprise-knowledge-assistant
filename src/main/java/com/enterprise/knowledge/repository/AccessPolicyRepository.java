package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.entity.AccessPolicy;
import com.enterprise.knowledge.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccessPolicyRepository extends JpaRepository<AccessPolicy, Long> {
    List<AccessPolicy> findByRole(AppUser.Role role);
}
