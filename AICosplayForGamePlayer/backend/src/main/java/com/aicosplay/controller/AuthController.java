package com.aicosplay.controller;

import com.aicosplay.entity.User;
import com.aicosplay.exception.EmailAlreadyExistsException;
import com.aicosplay.exception.UsernameAlreadyExistsException;
import com.aicosplay.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// 临时存储验证码的内存缓存
class VerificationCodeCache {
    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> expirationTimeCache = new ConcurrentHashMap<>();
    private static final long EXPIRATION_TIME = 5 * 60 * 1000; // 5分钟过期

    public static void put(String email, String code) {
        if (code == null) {
            // 如果code为null，则从缓存中移除
            cache.remove(email);
            expirationTimeCache.remove(email);
        } else {
            // 否则正常存储验证码和过期时间
            cache.put(email, code);
            expirationTimeCache.put(email, System.currentTimeMillis() + EXPIRATION_TIME);
        }
    }

    public static String get(String email) {
        // 检查是否过期
        Long expirationTime = expirationTimeCache.get(email);
        if (expirationTime != null && System.currentTimeMillis() > expirationTime) {
            cache.remove(email);
            expirationTimeCache.remove(email);
            return null;
        }
        return cache.get(email);
    }
}

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    // 验证用户名和邮箱是否匹配
    @PostMapping("/verify-user-email")
    public ResponseEntity<?> verifyUserEmail(@RequestBody VerifyUserEmailRequest request) {
        try {
            // 检查用户名和邮箱是否匹配
            Optional<User> user = userService.findByUsername(request.getUsername());
            if (user.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "用户名不存在"));
            }
            
            if (!user.get().getEmail().equals(request.getEmail())) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "用户名与邮箱不匹配"));
            }
            
            return ResponseEntity.ok(new ApiResponse(true, "验证成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "验证失败，请稍后重试"));
        }
    }
    
    // 生成验证码
    @PostMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestBody UsernameRequest usernameRequest) {
        try {
            boolean exists = userService.existsByUsername(usernameRequest.getUsername());
            if (exists) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "用户名已被使用"));
            }
            return ResponseEntity.ok(new ApiResponse(true, "用户名可用"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "检查用户名失败"));
        }
    }
    
    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestBody EmailRequest emailRequest) {
        try {
            boolean exists = userService.existsByEmail(emailRequest.getEmail());
            if (exists) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "邮箱已被使用"));
            }
            return ResponseEntity.ok(new ApiResponse(true, "邮箱可用"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "检查邮箱失败"));
        }
    }
    
    @PostMapping("/generate-code")
    public ResponseEntity<?> generateVerificationCode(@RequestBody EmailRequest emailRequest) {
        try {
            // 生成6位数字验证码
            Random random = new Random();
            int codeInt = 100000 + random.nextInt(900000); // 生成100000-999999之间的随机数
            String code = String.valueOf(codeInt);
            
            // 存储验证码
            VerificationCodeCache.put(emailRequest.getEmail(), code);
            
            // 打印验证码到控制台
            System.out.println("生成的验证码：" + code + " 用于邮箱：" + emailRequest.getEmail());
            
            return ResponseEntity.ok(new ApiResponse(true, "验证码已发送"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "生成验证码失败"));
        }
    }
    
    // 重置密码
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            // 1. 验证用户名和邮箱是否匹配
            Optional<User> userOpt = userService.findByUsername(request.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "用户名不存在"));
            }
            
            User user = userOpt.get();
            if (!user.getEmail().equals(request.getEmail())) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "用户名与邮箱不匹配"));
            }
            
            // 2. 验证验证码
            String storedCode = VerificationCodeCache.get(request.getEmail());
            if (storedCode == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "验证码已过期或不存在"));
            }
            
            if (!storedCode.equals(request.getVerificationCode())) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "验证码错误"));
            }
            
            // 3. 更新用户密码
            userService.updatePassword(user.getId(), request.getNewPassword());
            
            // 4. 清除验证码
            VerificationCodeCache.put(request.getEmail(), null);
            
            return ResponseEntity.ok(new ApiResponse(true, "密码重置成功"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse(false, "密码重置失败，请稍后重试"));
        }
    }

    @PostMapping("/register")
    // 移除控制器层的事务注解，让服务层处理事务
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            System.out.println("接收到注册请求: " + registerRequest.getUsername() + ", " + registerRequest.getEmail());
            
            // 验证验证码
            String storedCode = VerificationCodeCache.get(registerRequest.getEmail());
            System.out.println("缓存中的验证码: " + storedCode);
            if (storedCode == null) {
                System.out.println("验证码已过期或不存在");
                return ResponseEntity.badRequest().body(new ApiResponse(false, "验证码已过期或不存在"));
            }
            
            if (!storedCode.equals(registerRequest.getVerificationCode())) {
                System.out.println("验证码错误: 输入的验证码: " + registerRequest.getVerificationCode());
                return ResponseEntity.badRequest().body(new ApiResponse(false, "验证码错误"));
            }
            
            // 所有验证通过后，再创建用户
            System.out.println("所有验证通过，开始创建用户");
            User user = userService.registerUser(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword()
            );
            
            System.out.println("用户创建成功，用户ID: " + user.getId());
            
            // 注册成功后移除验证码
            VerificationCodeCache.put(registerRequest.getEmail(), null);
            
            return ResponseEntity.ok(new ApiResponse(true, "注册成功"));
        } catch (UsernameAlreadyExistsException e) {
            System.out.println("用户名已存在异常: " + e.getMessage());
            e.printStackTrace();
            // 返回用户名已被使用的错误信息
            return ResponseEntity.badRequest().body(new ApiResponse(false, "用户名已被使用"));
        } catch (EmailAlreadyExistsException e) {
            System.out.println("邮箱已存在异常: " + e.getMessage());
            e.printStackTrace();
            // 返回邮箱已被使用的错误信息
            return ResponseEntity.badRequest().body(new ApiResponse(false, "邮箱已被使用"));
        } catch (RuntimeException e) {
            System.out.println("运行时异常: " + e.getMessage());
            e.printStackTrace();
            // 让服务层的事务正常回滚，然后返回错误响应
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage() != null ? e.getMessage() : "注册失败"));
        } catch (Exception e) {
            System.out.println("一般异常: " + e.getMessage());
            e.printStackTrace();
            // 捕获所有其他异常，确保返回有意义的错误消息
            return ResponseEntity.badRequest().body(new ApiResponse(false, "注册失败，请稍后重试"));
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
    public static class EmailRequest {
        private String email;

        // Getters and Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
    
    // 验证用户名和邮箱的请求类
    public static class VerifyUserEmailRequest {
        private String username;
        private String email;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
    
    // 重置密码的请求类
    public static class ResetPasswordRequest {
        private String username;
        private String email;
        private String verificationCode;
        private String newPassword;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getVerificationCode() { return verificationCode; }
        public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
    
    public static class UsernameRequest {
        private String username;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
    
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String verificationCode;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getVerificationCode() { return verificationCode; }
        public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
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