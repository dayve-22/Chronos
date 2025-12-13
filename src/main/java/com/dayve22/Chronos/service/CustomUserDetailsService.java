package com.dayve22.Chronos.service;

import com.dayve22.Chronos.entity.User;
import com.dayve22.Chronos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Handle cases where role might be null
        String roles = (user.getRoles() == null || user.getRoles().isEmpty())
                ? "ROLE_USER" // Default fallback
                : user.getRoles();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                // USE authorities() instead of roles() to avoid double prefixing
                .authorities(AuthorityUtils.commaSeparatedStringToAuthorityList(roles))
                .build();
    }
}