package com.matrix.cloud.controller;

import com.matrix.cloud.dto.ApiResponse;
import com.matrix.cloud.entity.User;
import com.matrix.cloud.service.AdminService;
import com.matrix.cloud.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public ApiResponse<List<User>> getAllUsers() {
        List<User> users = adminService.getAllUsers();
        return ApiResponse.success(users);
    }

    @PostMapping("/users")
    public ApiResponse<User> createUser(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        String nickname = request.get("nickname");
        String department = request.get("department");
        String position = request.get("position");

        if (email == null || email.trim().isEmpty()) {
            return ApiResponse.error("邮箱不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return ApiResponse.error("密码不能为空");
        }
        if (nickname == null || nickname.trim().isEmpty()) {
            return ApiResponse.error("昵称不能为空");
        }

        User user = userService.createUser(email.trim(), password, nickname.trim(), department, position);
        return ApiResponse.success("用户创建成功", user);
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<User> updateUser(@PathVariable Long userId,
                                        @RequestBody Map<String, String> request) {
        User user = userService.adminUpdateUser(userId, request);
        return ApiResponse.success("用户信息已更新", user);
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ApiResponse.success("用户已删除", null);
    }

    @PostMapping("/users/{userId}/ban")
    public ApiResponse<Void> banUser(@PathVariable Long userId) {
        adminService.banUser(userId);
        return ApiResponse.success("用户已封禁", null);
    }

    @PostMapping("/users/{userId}/unban")
    public ApiResponse<Void> unbanUser(@PathVariable Long userId) {
        adminService.unbanUser(userId);
        return ApiResponse.success("用户已解封", null);
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = adminService.getStatistics();
        return ApiResponse.success(stats);
    }

    @GetMapping("/system-info")
    public ApiResponse<Map<String, Object>> getSystemInfo() {
        Map<String, Object> systemInfo = adminService.getSystemInfo();
        return ApiResponse.success(systemInfo);
    }

    @GetMapping("/teams")
    public ApiResponse<List<Map<String, Object>>> getAllTeams() {
        List<Map<String, Object>> teams = adminService.getAllTeams();
        return ApiResponse.success(teams);
    }

    @GetMapping("/teams/{teamId}/messages")
    public ApiResponse<List<Map<String, Object>>> getTeamMessages(@PathVariable Long teamId) {
        List<Map<String, Object>> messages = adminService.getTeamMessages(teamId);
        return ApiResponse.success(messages);
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> request,
                                            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        adminService.changePassword(token, request.get("oldPassword"), request.get("newPassword"));
        return ApiResponse.success("密码修改成功", null);
    }

    @PostMapping("/change-email")
    public ApiResponse<Void> changeEmail(@RequestBody Map<String, String> request,
                                         @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        adminService.changeEmail(token, request.get("newEmail"), request.get("password"));
        return ApiResponse.success("邮箱修改成功", null);
    }
}
