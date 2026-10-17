package com.pulsepass.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config,
                                                       CustomUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
// y el return que ya tienes abajo
        provider.setPasswordEncoder(passwordEncoder);
        return new org.springframework.security.authentication.ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   RestAuthenticationEntryPoint authEntryPoint,
                                                   RestAccessDeniedHandler deniedHandler) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable) // API Bearer stateless, sin cookies de sesión
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(deniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // --- Auth público ---
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                        // --- Venues: 100% público (FR-SEC-VEN-01/02) ---
                        .requestMatchers(HttpMethod.GET, "/api/venues/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/venues/*").permitAll()

                        // --- Artists: 100% público (FR-SEC-ART-01..03) ---
                        .requestMatchers(HttpMethod.GET, "/api/artists/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/artists/by-stage-name").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/artists/*").permitAll()

                        // --- Events: rutas específicas antes que {eventCode} ---
                        .requestMatchers(HttpMethod.GET, "/api/events/published").permitAll()
                        // by-artist y {eventCode} quedan autenticados aquí; el filtrado de
                        // "público solo si está publicado" lo aplica AuthorizationService/EventService
                        // (FR-SEC-EVT-02/06), no SecurityConfig.
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")

                        // --- Tickets ---
                        .requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets").hasRole("USER")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/cancel").hasAnyRole("USER", "ADMIN")

                        // --- Users: requieren login; ownership lo valida AuthorizationService ---
                        .requestMatchers(HttpMethod.GET, "/api/users/by-email").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/by-username").authenticated()

                        // --- Auth autenticado ---
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // SEC-001: todo lo demás requiere autenticación
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}