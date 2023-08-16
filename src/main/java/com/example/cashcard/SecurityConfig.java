package com.example.cashcard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // Annotation tells Spring to use this class to configure Spring and Spring Boot
public class SecurityConfig {

    @Bean // Spring Security expects a Bean to configure its Filter Chain
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { 
        http.authorizeHttpRequests()
            .requestMatchers("/cashcards/**")
            .hasRole("CARD-OWNER")
            .and()
            .csrf().disable()
            .httpBasic();
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean // COnfigure a user named sarah1 with password abc123
    public UserDetailsService testOnlyUsers(PasswordEncoder passwordEncoder){
        
        User.UserBuilder users = User.builder();

        UserDetails sarah = users
            .username("sarah1")
            .password(passwordEncoder.encode("abc123"))
            .roles("CARD-OWNER") // New Role
            .build();
            UserDetails hankOwnsNoCards = users
            .username("hank-owns-no-cards")
            .password(passwordEncoder.encode("qrs456"))
            .roles("NON-OWNER") // New Role
            .build();
        UserDetails kumar = users
            .username("kumar2")
            .password(passwordEncoder.encode("xyz789"))
            .roles("CARD-OWNER") // New Role
            .build();
            
        return new InMemoryUserDetailsManager(sarah, hankOwnsNoCards, kumar);
    }
}