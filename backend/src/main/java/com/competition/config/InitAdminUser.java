package com.competition.config;

import com.competition.entity.User;
import com.competition.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InitAdminUser {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public InitAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRealName("系统管理员");
            admin.setEmail("admin@hhtc.edu.cn");
            admin.setRole("ADMIN");
            admin.setStatus(1);
            admin.setRegisterTime(LocalDateTime.now());
            admin.setCollege("计算机与人工智能学院（软件学院）");
            userRepository.save(admin);
            System.out.println("[InitAdmin] Created admin user");
        } else {
            System.out.println("[InitAdmin] Admin user already exists");
        }
    }
}
