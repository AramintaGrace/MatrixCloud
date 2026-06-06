package com.matrix.cloud.controller;

import com.matrix.cloud.dto.ApiResponse;
import com.matrix.cloud.entity.User;
import com.matrix.cloud.service.UserService;
import com.matrix.cloud.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userService.getUserById(userId);
        return ApiResponse.success(user);
    }

    @PutMapping("/me")
    public ApiResponse<User> updateProfile(@RequestHeader("Authorization") String authHeader,
                                           @RequestBody Map<String, Object> updates) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userService.updateProfile(userId, updates);
        return ApiResponse.success("更新成功", user);
    }

    @GetMapping("/search")
    public ApiResponse<List<User>> searchUsers(@RequestParam(required = false) String nickname) {
        List<User> users = userService.searchUsers(nickname);
        return ApiResponse.success(users);
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ApiResponse.success(user);
    }
}
