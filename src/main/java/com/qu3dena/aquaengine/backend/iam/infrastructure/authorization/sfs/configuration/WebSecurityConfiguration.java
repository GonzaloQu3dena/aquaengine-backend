package com.qu3dena.aquaengine.backend.iam.infrastructure.authorization.sfs.configuration;

import com.qu3dena.aquaengine.backend.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import com.qu3dena.aquaengine.backend.iam.infrastructure.hashing.bcrypt.BCryptHashingService;
import com.qu3dena.aquaengine.backend.iam.infrastructure.tokens.jwt.BearerTokenService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * Web Security Configuration
 * <p>
 * Main configuration class for web security in the application.
 * Sets up security filters, authentication, authorization, CORS, CSRF protection,
 * exception handling, session management, and defines which endpoints are publicly accessible.
 * </p>
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {

    private final BearerTokenService tokenService;
    private final BCryptHashingService hashingService;
    private final UserDetailsService userDetailsService;
    private final AuthenticationEntryPoint unauthorizedRequestHandlerEntryPoint;

    /**
     * Constructor for WebSecurityConfiguration.
     *
     * @param userDetailsService                   User details service implementation.
     * @param tokenService                         Service for handling Bearer tokens.
     * @param hashingService                       Service for password hashing.
     * @param unauthorizedRequestHandlerEntryPoint Handler for unauthorized requests.
     */
    public WebSecurityConfiguration(
            @Qualifier("defaultUserDetailsService")
            UserDetailsService userDetailsService,
            BearerTokenService tokenService,
            BCryptHashingService hashingService,
            AuthenticationEntryPoint unauthorizedRequestHandlerEntryPoint) {

        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.hashingService = hashingService;
        this.unauthorizedRequestHandlerEntryPoint = unauthorizedRequestHandlerEntryPoint;
    }

    /**
     * Creates the BearerAuthorizationRequestFilter bean.
     *
     * @return BearerAuthorizationRequestFilter for JWT Bearer token authentication.
     */
    @Bean
    public BearerAuthorizationRequestFilter authorizationRequestFilter(BearerTokenService tokenService, @Qualifier("defaultUserDetailsService") UserDetailsService uds) {
        return new BearerAuthorizationRequestFilter(tokenService, uds);
    }

    /**
     * Provides the AuthenticationManager bean.
     *
     * @param authenticationConfiguration Spring authentication configuration.
     * @return AuthenticationManager instance.
     * @throws Exception if the manager cannot be created.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Provides the DaoAuthenticationProvider bean.
     *
     * @return DaoAuthenticationProvider configured with user details and password encoder.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(hashingService);
        return provider;
    }

    /**
     * Provides the PasswordEncoder bean.
     *
     * @return PasswordEncoder for encoding user passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return hashingService;
    }

    /**
     * Configures the security filter chain.
     * <p>
     * Sets up CORS, disables CSRF, configures exception handling, stateless session management,
     * allows unauthenticated access to specific endpoints, and adds the JWT Bearer filter.
     * </p>
     *
     * @param http HttpSecurity object for configuration.
     * @return Configured SecurityFilterChain.
     * @throws Exception if configuration fails.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // URLs we leave unsecured (including Swagger/OpenAPI)Add commentMore actions
        String[] publicMatchers = {
                "/api/v1/authentication/**",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/webjars/**"
        };
        // Cross-Origin Resource Sharing configuration
        http
                // CORS
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new CorsConfiguration();
                    corsConfig.setAllowedOrigins(List.of("*"));
                    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(List.of("*"));
                    return corsConfig;
                }))
                // Disable CSRF as we work with JWT
                .csrf(csrf -> csrf.disable())
                // Exception handling
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthorizedRequestHandlerEntryPoint))
                // Stateless (no HTTP session)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicMatchers).permitAll()
                        .anyRequest().authenticated()
                )
                // Set the authentication provider by calling the bean method.
                .authenticationProvider(authenticationProvider())
                // Insert the JWT Bearer filter before Spring's login filter.
                .addFilterBefore(
                        authorizationRequestFilter(tokenService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

}