package com.aicosplay.service;

import com.aicosplay.entity.User;
import com.aicosplay.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(String username, String email, String password) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名重复");
        }
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱重复");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(username); // 默认使用用户名为昵称
        user.setStatus(1); // 默认状态为启用

        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean authenticate(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // 检查用户状态是否启用
            if (user.getStatus() != 1) {
                return false;
            }
            return passwordEncoder.matches(password, user.getPassword());
        }
        return false;
    }

    // 更新用户信息
    public User updateUserInfo(String username, String nickname, String avatar) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 更新非空字段
        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }
        
        if (avatar != null) {
            user.setAvatar(avatar);
        }

        // 保存更新后的用户信息
        return userRepository.save(user);
    }
}