package com.example.caro_plus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/register", "/css/**", "/js/**").permitAll() // Thêm
                                                                                                                    // thư
                                                                                                                    // mục
                                                                                                                    // tĩnh
                                                                                                                    // nếu
                                                                                                                    // có
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/") // Trang chứa form đăng nhập
                                                .loginProcessingUrl("/login") // BẮT BUỘC: Đường dẫn mà thẻ <form
                                                                              // action="..."> trong HTML gọi tới
                                                .defaultSuccessUrl("/home", true) // Thành công thì vào /home
                                                .failureUrl("/?error=true") // Thất bại thì quay lại trang chủ kèm tham
                                                                            // số error
                                                .permitAll())
                                .rememberMe(remember -> remember
                                                .key("caro_plus_secret_key") // Chuỗi bí mật để mã hóa cookie
                                                .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 ngày
                                                .rememberMeParameter("remember") // BẮT BUỘC: trùng với thuộc tính
                                                                                 // name="remember" của thẻ input
                                                                                 // checkbox trong HTML
                                )
                                .logout(logout -> logout
                                                .logoutUrl("/logout") // Đường dẫn gọi khi muốn đăng xuất
                                                .logoutSuccessUrl("/") // Đăng xuất xong về trang chủ
                                                .deleteCookies("JSESSIONID", "remember-me")
                                                .permitAll());

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}