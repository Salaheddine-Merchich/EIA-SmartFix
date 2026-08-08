package com.ocp.eia.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.UserRepository;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDemoPasswords() {
        return args -> {
            String defaultPassword = passwordEncoder.encode("Password123!");
            updatePassword("admin@ocp.ma", defaultPassword);
            updatePassword("responsable@ocp.ma", defaultPassword);
            updatePassword("technicien@ocp.ma", defaultPassword);
            log.info("Mots de passe demo initialisés (Password123!)");
        };
    }

    private void updatePassword(String email, String hash) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setPasswordHash(hash);
            userRepository.save(user);
        });
    }
}
