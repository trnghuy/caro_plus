package com.example.caro_plus.security;

import com.example.caro_plus.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final User user;

    // Constructor truyền vào User của bạn
    public CustomUserDetails(User user) {
        this.user = user;
    }

    // Hàm này rất quan trọng để Controller lấy được thông tin User gốc
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Tạm thời chưa phân quyền (Role) nên trả về danh sách rỗng
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    // 4 hàm bên dưới bắt buộc return true để tài khoản hoạt động
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}