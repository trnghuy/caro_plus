// package com.example.caro_plus.config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.web.SecurityFilterChain;

// @Configuration
// public class SecurityConfig {

//     @Bean
//     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

//         http
//                 .csrf(csrf -> csrf.disable()) // tắt CSRF cho dễ test

//                 .authorizeHttpRequests(auth -> auth
//                         .requestMatchers("/", "/login", "/register", "/css/**", "/js/**").permitAll()
//                         .requestMatchers("/api/**").authenticated()
//                         .anyRequest().authenticated())

//                 .formLogin(form -> form
//                         .loginPage("/login") // trang login của bạn
//                         .defaultSuccessUrl("/", true)
//                         .permitAll())

//                 .logout(logout -> logout
//                         .logoutSuccessUrl("/login")
//                         .permitAll());

//         return http.build();
//     }
// }







package com.example.caro_plus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable()) //  tắt CSRF

                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() //  cho phép hết
                )

                .formLogin(form -> form.disable()) //  TẮT Spring Security login

                .logout(logout -> logout.disable()); //  TẮT luôn logout của nó

        return http.build();
    }
}