package com.matrix.cloud.service;

import com.matrix.cloud.entity.ChatMessage;
import com.matrix.cloud.entity.ChatSession;
import com.matrix.cloud.entity.Team;
import com.matrix.cloud.entity.User;
import com.matrix.cloud.repository.*;
import com.matrix.cloud.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("不能封禁管理员账号");
        }

        user.setStatus(User.Status.BANNED);
        userRepository.save(user);

        // 从在线列表移除
        redisTemplate.opsForSet().remove("online_users", userId.toString());
    }

    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setStatus(User.Status.ACTIVE);
        userRepository.save(user);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 用户统计
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(User.Status.ACTIVE);
        long bannedUsers = userRepository.countByStatus(User.Status.BANNED);

        // 在线用户数
        Long onlineUsers = redisTemplate.opsForSet().size("online_users");

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("bannedUsers", bannedUsers);
        stats.put("onlineUsers", onlineUsers != null ? onlineUsers : 0);

        return stats;
    }

    @Transactional
    public void changePassword(String token, String oldPassword, String newPassword) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("当前密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void changeEmail(String token, String newEmail, String password) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("该邮箱已被使用");
        }

        user.setEmail(newEmail);
        userRepository.save(user);
    }

    public Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();

        // 获取系统信息
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        // CPU信息
        systemInfo.put("cpuCores", Runtime.getRuntime().availableProcessors());
        systemInfo.put("systemLoadAverage", osBean.getSystemLoadAverage());

        // 内存信息
        long totalMemory = memoryBean.getHeapMemoryUsage().getMax();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        systemInfo.put("totalMemory", totalMemory);
        systemInfo.put("usedMemory", usedMemory);
        systemInfo.put("freeMemory", totalMemory - usedMemory);

        // 用户统计
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(User.Status.ACTIVE);
        long bannedUsers = userRepository.countByStatus(User.Status.BANNED);
        Long onlineUsers = redisTemplate.opsForSet().size("online_users");

        systemInfo.put("totalUsers", totalUsers);
        systemInfo.put("activeUsers", activeUsers);
        systemInfo.put("bannedUsers", bannedUsers);
        systemInfo.put("onlineUsers", onlineUsers != null ? onlineUsers : 0);

        // 团队统计
        long totalTeams = teamRepository.count();
        systemInfo.put("totalTeams", totalTeams);

        return systemInfo;
    }

    public List<Map<String, Object>> getAllTeams() {
        List<Team> teams = teamRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Team team : teams) {
            Map<String, Object> teamInfo = new HashMap<>();
            teamInfo.put("id", team.getId());
            teamInfo.put("name", team.getName());
            teamInfo.put("creatorId", team.getCreatorId());
            teamInfo.put("createdAt", team.getCreatedAt());

            // 获取成员数量
            long memberCount = teamMemberRepository.countByTeamId(team.getId());
            teamInfo.put("memberCount", memberCount);

            result.add(teamInfo);
        }

        return result;
    }

    public List<Map<String, Object>> getTeamMessages(Long teamId) {
        // 获取团队聊天室
        ChatSession chatSession = chatSessionRepository.findByTypeAndTeamId(ChatSession.Type.TEAM, teamId)
                .orElseThrow(() -> new RuntimeException("团队聊天室不存在"));

        // 获取聊天消息
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(chatSession.getId());
        List<Map<String, Object>> result = new ArrayList<>();

        for (ChatMessage message : messages) {
            Map<String, Object> messageInfo = new HashMap<>();
            messageInfo.put("id", message.getId());
            messageInfo.put("content", message.getContent());
            messageInfo.put("senderId", message.getSenderId());
            messageInfo.put("createdAt", message.getCreatedAt());

            // 获取发送者信息
            User sender = userRepository.findById(message.getSenderId()).orElse(null);
            if (sender != null) {
                messageInfo.put("senderName", sender.getNickname());
                messageInfo.put("senderAvatar", sender.getAvatar());
            }

            result.add(messageInfo);
        }

        return result;
    }
}
