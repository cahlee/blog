package io.cahlee.blog.config;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

import io.cahlee.blog.repository.UserRepository;

@Configuration
public class SecurityConfig {
	
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    	http
        .authorizeHttpRequests((authorize) -> authorize
            .requestMatchers("/post/new").authenticated()
            .requestMatchers("/post/update").authenticated()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll()
        )
        .httpBasic(Customizer.withDefaults())
		.formLogin(Customizer.withDefaults());
        return http.build();
    }

	@Bean
	UserDetailsService userDetailsService() {
		return new UserDetailsService() {

			@Autowired
			private UserRepository userRepository;

			@Override
			public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
				Optional<io.cahlee.blog.domain.User> user = userRepository.findById(username);
				if(!user.isPresent()) {
					throw new UsernameNotFoundException(username + "- Username Not Found");
				} else {
					return new SecurityUser(user.get()); 
				}
			}
			
			
		};
	}

}
