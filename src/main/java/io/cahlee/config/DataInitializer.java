package io.cahlee.config;

import io.cahlee.domain.user.User;
import io.cahlee.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail("ctrl0703@naver.com").isEmpty()) {
            userRepository.save(User.builder()
                    .email("ctrl0703@naver.com")
                    .username("ctrl0703")
                    .password(passwordEncoder.encode("12345678"))
                    .role(User.Role.USER)
                    .provider(User.Provider.LOCAL)
                    .build());
        }
    }
}
