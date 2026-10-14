
package com.enterprise.knowledge.security;

import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.repository.AppUserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository userRepository;

    // Explicit constructor replaces @RequiredArgsConstructor
    public UserDetailsServiceImpl(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));

        return User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.getEnabled())
                .authorities(List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name())))
                .build();
    }
}
