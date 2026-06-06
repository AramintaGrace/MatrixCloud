package com.matrix.cloud.service;

import com.matrix.cloud.dto.LoginRequest;
import com.matrix.cloud.entity.User;
import com.matrix.cloud.repository.UserRepository;
import com.matrix.cloud.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Transactional
    public User createUser(String email, String password, String nickname,
                           String department, String position) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("该邮箱已被使用");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRole(User.Role.USER);
        user.setStatus(User.Status.ACTIVE);

        if (department != null && !department.trim().isEmpty()) {
            user.setDepartment(department.trim());
        }
        if (position != null && !position.trim().isEmpty()) {
            user.setPosition(position.trim());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("不能删除管理员账号");
        }

        userRepository.delete(user);
    }

    @Transactional
    public User adminUpdateUser(Long userId, Map<String, String> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (updates.containsKey("email")) {
            String newEmail = updates.get("email");
            if (newEmail != null && !newEmail.trim().isEmpty() && !newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new RuntimeException("该邮箱已被使用");
                }
                user.setEmail(newEmail.trim());
            }
        }
        if (updates.containsKey("password")) {
            String newPassword = updates.get("password");
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(newPassword));
            }
        }
        if (updates.containsKey("nickname")) {
            String nickname = updates.get("nickname");
            if (nickname != null && !nickname.trim().isEmpty()) {
                user.setNickname(nickname.trim());
            }
        }
        if (updates.containsKey("department")) {
            user.setDepartment(updates.get("department"));
        }
        if (updates.containsKey("position")) {
            user.setPosition(updates.get("position"));
        }

        return userRepository.save(user);
    }

    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getStatus() == User.Status.BANNED) {
            throw new RuntimeException("账号已被封禁");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        redisTemplate.opsForSet().add("online_users", user.getId().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", getUserInfo(user));

        return result;
    }

    public void logout(String token) {
        redisTemplate.opsForValue().set("blacklist:" + token, "1", 24, TimeUnit.HOURS);

        Long userId = jwtUtil.getUserIdFromToken(token);
        redisTemplate.opsForSet().remove("online_users", userId.toString());
    }

    public List<User> searchUsers(String nickname) {
        List<User> users;
        if (nickname == null || nickname.trim().isEmpty()) {
            users = userRepository.findAll();
        } else {
            users = userRepository.findByNicknameContainingOrEmailContaining(nickname, nickname);
        }
        return users.stream()
                .filter(u -> u.getRole() != User.Role.ADMIN)
                .collect(Collectors.toList());
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Transactional
    public User updateProfile(Long userId, Map<String, Object> updates) {
        User user = getUserById(userId);

        if (updates.containsKey("nickname")) {
            user.setNickname((String) updates.get("nickname"));
        }
        if (updates.containsKey("avatar")) {
            user.setAvatar((String) updates.get("avatar"));
        }
        if (updates.containsKey("personalNote")) {
            user.setPersonalNote((String) updates.get("personalNote"));
        }

        return userRepository.save(user);
    }

    private Map<String, Object> getUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("email", user.getEmail());
        info.put("nickname", user.getNickname());
        info.put("avatar", user.getAvatar());
        info.put("department", user.getDepartment());
        info.put("position", user.getPosition());
        info.put("personalNote", user.getPersonalNote());
        info.put("role", user.getRole());
        info.put("status", user.getStatus());
        return info;
    }

    public javax.sql.DataSource getDataSource() {
        return dataSource;
    }
}
