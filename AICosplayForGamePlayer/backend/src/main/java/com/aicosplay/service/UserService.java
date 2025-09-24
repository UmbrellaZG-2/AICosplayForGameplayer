package com.aicosplay.service;

import com.aicosplay.entity.User;
import com.aicosplay.exception.EmailAlreadyExistsException;
import com.aicosplay.exception.UserNotFoundException;
import com.aicosplay.exception.UsernameAlreadyExistsException;
import com.aicosplay.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class) // 明确指定遇到任何异常都回滚事务
    public User registerUser(String username, String email, String password) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(username); // 默认使用用户名为昵称
        user.setStatus((byte) 1); // 默认状态为启用

        try {
            // 保存用户并返回
            return userRepository.save(user);
        } catch (Exception e) {
            // 捕获保存过程中的任何异常，确保事务回滚
            System.out.println("用户保存失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("用户创建失败: " + e.getMessage());
        }
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean authenticate(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // 检查用户状态是否启用
            if (user.getStatus() != (byte) 1) {
                return false;
            }
            return passwordEncoder.matches(password, user.getPassword());
        }
        return false;
    }

    // 更新用户信息
    public User updateUserInfo(String username, String nickname, String avatar) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException());

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
    
    // 检查邮箱是否已存在
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // 检查用户名是否已存在
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    // 更新用户密码
    @Transactional(rollbackFor = Exception.class)
    public User updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());
        
        // 加密新密码并更新
        user.setPassword(passwordEncoder.encode(newPassword));
        
        // 保存更新后的用户信息
        return userRepository.save(user);
    }
}