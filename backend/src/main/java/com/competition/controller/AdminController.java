package com.competition.controller;

import com.competition.dto.Result;
import com.competition.entity.User;
import com.competition.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public Result<Page<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            if (role != null && !role.isEmpty()) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (keyword != null && !keyword.isEmpty()) {
                String like = "%" + keyword + "%";
                Predicate nameLike = cb.like(root.get("realName"), like);
                Predicate usernameLike = cb.like(root.get("username"), like);
                Predicate studentIdLike = cb.like(root.get("studentId"), like);
                predicates.add(cb.or(nameLike, usernameLike, studentIdLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> userPage = userRepository.findAll(spec, pageRequest);

        Page<Map<String, Object>> result = userPage.map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("role", u.getRole());
            m.put("email", u.getEmail());
            m.put("studentId", u.getStudentId());
            m.put("college", u.getCollege());
            m.put("status", u.getStatus());
            m.put("registerTime", u.getRegisterTime());
            m.put("lastLoginTime", u.getLastLoginTime());
            return m;
        });

        return Result.success(result);
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userRepository.save(user);
        return Result.success(null);
    }

    @PutMapping("/users/{id}/role")
    public Result<Void> changeRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String newRole = body.get("role");
        if (newRole == null || !newRole.matches("STUDENT|TEACHER|SECRETARY|LEADER|ADMIN")) {
            throw new RuntimeException("无效的角色: " + newRole);
        }
        user.setRole(newRole);
        userRepository.save(user);
        return Result.success(null);
    }
}
