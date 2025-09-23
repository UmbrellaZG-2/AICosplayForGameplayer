package com.aicosplay.controller;

import com.aicosplay.entity.User;
import com.aicosplay.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            User user = userService.registerUser(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword()
            );
            return ResponseEntity.ok(new ApiResponse(true, "注册成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        boolean authenticated = userService.authenticate(
                loginRequest.getUsername(),
                loginRequest.getPassword()
        );
        if (authenticated) {
            // 设置会话属性
            session.setAttribute("username", loginRequest.getUsername());
            return ResponseEntity.ok(new ApiResponse(true, "Login successful"));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid username or password"));
        }
    }

    // 获取用户信息
    @GetMapping("/user")
    public ResponseEntity<?> getUserInfo(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body(new ApiResponse(false, "User not authenticated"));
        }
        
        Optional<User> user = userService.findByUsername(username);
        if (user.isPresent()) {
            // 创建用户信息响应对象，不包含密码等敏感信息
            UserResponse userResponse = new UserResponse();
            userResponse.setId(user.get().getId());
            userResponse.setUsername(user.get().getUsername());
            userResponse.setEmail(user.get().getEmail());
            userResponse.setNickname(user.get().getNickname());
            userResponse.setAvatar(user.get().getAvatar());
            userResponse.setStatus(user.get().getStatus());
            userResponse.setCreatedAt(user.get().getCreatedAt().toString());
            userResponse.setUpdatedAt(user.get().getUpdatedAt().toString());

            
            return ResponseEntity.ok(userResponse);
        } else {
            return ResponseEntity.status(404).body(new ApiResponse(false, "User not found"));
        }
    }

    // 更新用户信息
    @PutMapping("/user")
    public ResponseEntity<?> updateUserInfo(@RequestBody UserUpdateRequest request, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body(new ApiResponse(false, "User not authenticated"));
        }
        
        try {
            User updatedUser = userService.updateUserInfo(username, request.getNickname(), request.getAvatar());
            UserResponse userResponse = new UserResponse();
            userResponse.setId(updatedUser.getId());
            userResponse.setUsername(updatedUser.getUsername());
            userResponse.setEmail(updatedUser.getEmail());
            userResponse.setNickname(updatedUser.getNickname());
            userResponse.setAvatar(updatedUser.getAvatar());
            userResponse.setStatus(updatedUser.getStatus());
            userResponse.setCreatedAt(updatedUser.getCreatedAt().toString());
            userResponse.setUpdatedAt(updatedUser.getUpdatedAt().toString());
            
            return ResponseEntity.ok(userResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    // 请求和响应DTO类
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class UserUpdateRequest {
        private String nickname;
        private String avatar;

        // Getters and Setters
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
    }

    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String nickname;
        private String avatar;
        private Byte status;
        private String createdAt;
        private String updatedAt;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        
        public Byte getStatus() { return status; }
        public void setStatus(Byte status) { this.status = status; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class ApiResponse {
        private boolean success;
        private String message;

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}