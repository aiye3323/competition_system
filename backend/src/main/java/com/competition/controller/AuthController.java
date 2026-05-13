package com.competition.controller;

import com.competition.dto.*;
import com.competition.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return Result.success(response);
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }

    @GetMapping("/current")
    public Result<LoginResponse.UserInfo> getCurrentUser(Authentication authentication) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            LoginResponse.UserInfo userInfo = authService.getCurrentUser(userId);
            return Result.success(userInfo);
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestParam String role) {
        try {
            authService.updateRole(id, role);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/profile")
    public Result<?> getProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        var user = authService.getProfile(userId);
        var data = new java.util.HashMap<String, Object>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("email", user.getEmail());
        data.put("studentId", user.getStudentId());
        data.put("college", user.getCollege());
        data.put("role", user.getRole());
        return Result.success(data);
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody RegisterRequest request, Authentication authentication) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            var user = authService.updateProfile(userId, request);
            var data = new java.util.HashMap<String, Object>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("email", user.getEmail());
            data.put("studentId", user.getStudentId());
            data.put("college", user.getCollege());
            data.put("role", user.getRole());
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
