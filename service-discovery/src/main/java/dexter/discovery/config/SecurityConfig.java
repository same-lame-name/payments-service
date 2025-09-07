package dexter.discovery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Updated CSRF configuration using lambda
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated() // All requests must be authenticated
                )
                .httpBasic(withDefaults()); // Use default settings for HTTP Basic auth

        return http.build(); // Don't forget to build the chain!
    }
}
