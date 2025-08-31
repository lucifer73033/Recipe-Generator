package Assignment.Recipe_Generator.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/", "/index.html", "/static/**", "/assets/**").permitAll()
                .requestMatchers("/api/recipes/generate").permitAll()
                .requestMatchers("/api/recipes").permitAll()
                .requestMatchers("/api/ingredients/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/config/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Protected endpoints - require basic authentication
                .requestMatchers("/api/recipes/{id}").authenticated()
                .requestMatchers("/api/recipes/{id}/rating").authenticated()
                .requestMatchers("/api/recipes/{id}/favorite").authenticated()
                .requestMatchers("/api/recipes/{id}/save").authenticated()
                .requestMatchers("/api/recipes/{id}/rate").authenticated()
                .requestMatchers("/api/me/**").authenticated()
                
                .anyRequest().permitAll()
            )
            .addFilterBefore(basicAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public BasicAuthenticationFilter basicAuthenticationFilter() {
        return new BasicAuthenticationFilter();
    }

    public static class BasicAuthenticationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                     FilterChain filterChain) throws ServletException, IOException {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Basic ")) {
                String token = authHeader.substring(6);
                try {
                    // Decode base64 token
                    String credentials = new String(Base64.getDecoder().decode(token));
                    String[] parts = credentials.split(":");
                    
                    if (parts.length == 2) {
                        String username = parts[0];
                        String password = parts[1];
                        
                        // Create a simple authentication object
                        // In production, you'd verify the credentials against the database
                        if (username != null && !username.isEmpty() && 
                            password != null && !password.isEmpty()) {
                            
                            // Create authentication object with USER role
                            Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                username,
                                null, // No credentials needed after authentication
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                            
                            // Set the authentication in the security context
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            log.debug("Authentication set for user: {}", username);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Invalid Basic auth token: {}", e.getMessage());
                }
            }
            
            filterChain.doFilter(request, response);
        }
    }
}



