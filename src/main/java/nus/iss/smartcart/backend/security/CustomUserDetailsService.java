package nus.iss.smartcart.backend.security;

import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Author: Junior

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String email
    ) throws UsernameNotFoundException {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + email
                        )
                );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(user.getStatus() !=
                        nus.iss.smartcart.backend.model.UserStatus.ACTIVE)
                .build();
    }

    public UserDetails loadUserByEmail(
            String email
    ) throws UsernameNotFoundException {

        return loadUserByUsername(email);
    }
}