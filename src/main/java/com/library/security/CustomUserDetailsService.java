package com.library.security;

import com.library.entity.User;
import com.library.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new UserDetailsImpl(user);
    }

    public static class UserDetailsImpl implements UserDetails {
        private final User user;

        public UserDetailsImpl(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getSystemRole().name())
            );
        }

        @Override
        public String getPassword() {
            return user.getPasswordHash();
        }

        @Override
        public String getUsername() {
            return user.getEmail();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return user.getAccountStatus().toString().equals("ACTIVE");
        }

        public Long getId() {
            return user.getId();
        }

        public User getUser() {
            return user;
        }
    }
}
