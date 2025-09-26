package com.aicosplay.controller;

import com.aicosplay.entity.User;
import com.aicosplay.exception.BusinessException;
import com.aicosplay.model.ApiResponse;
import com.aicosplay.service.UserService;
import com.aicosplay.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 退出登录接口
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout() {
        // 清除用户登录状态
        UserContext.clearLoginStatus();
        return ResponseEntity.ok(ApiResponse.success("退出登录成功"));
    }
    
    // 验证用户名和邮箱是否匹配
    @PostMapping("/verify-user-email")
    public ResponseEntity<ApiResponse<?>> verifyUserEmail(@RequestBody VerifyUserEmailRequest request) {
        // 检查用户名和邮箱是否匹配
        Optional<User> user = userService.findByUsername(request.getUsername());
        if (user.isEmpty()) {
            throw new BusinessException("USER_NOT_FOUND", "用户名不存在");
        }
        
        if (!user.get().getEmail().equals(request.getEmail())) {
            throw new BusinessException("USER_EMAIL_MISMATCH", "用户名与邮箱不匹配");
        }
        
        return ResponseEntity.ok(ApiResponse.success("验证成功"));
    }
    
    // 检查用户名是否已存在
    @PostMapping("/check-username")
    public ResponseEntity<ApiResponse<?>> checkUsername(@RequestBody UsernameRequest usernameRequest) {
        boolean exists = userService.existsByUsername(usernameRequest.getUsername());
        if (exists) {
            throw new BusinessException("USERNAME_ALREADY_EXISTS", "用户名已被使用");
        }
        return ResponseEntity.ok(ApiResponse.success("用户名可用"));
    }

    // 检查邮箱是否已存在
    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<?>> checkEmail(@RequestBody EmailRequest emailRequest) {
        boolean exists = userService.existsByEmail(emailRequest.getEmail());
        if (exists) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "邮箱已被使用");
        }
        return ResponseEntity.ok(ApiResponse.success("邮箱可用"));
    }
    
    @PostMapping("/generate-code")
    public ResponseEntity<ApiResponse<?>> generateVerificationCode(@RequestBody EmailRequest emailRequest) {
        // 生成6位数字验证码
        Random random = new Random();
        int codeInt = 100000 + random.nextInt(900000); // 生成100000-999999之间的随机数
        String code = String.valueOf(codeInt);
        
        // 存储验证码
        VerificationCodeCache.put(emailRequest.getEmail(), code);
        
        // 打印验证码到控制台
        System.out.println("生成的验证码：" + code + " 用于邮箱：" + emailRequest.getEmail());
        
        return ResponseEntity.ok(ApiResponse.success("验证码已发送"));
    }
    
    // 重置密码
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody ResetPasswordRequest request) {
        // 1. 验证用户名和邮箱是否匹配
        Optional<User> userOpt = userService.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            throw new BusinessException("USER_NOT_FOUND", "用户名不存在");
        }
        
        User user = userOpt.get();
        if (!user.getEmail().equals(request.getEmail())) {
            throw new BusinessException("USER_EMAIL_MISMATCH", "用户名与邮箱不匹配");
        }
        
        // 2. 验证验证码
        String storedCode = VerificationCodeCache.get(request.getEmail());
        if (storedCode == null) {
            throw new BusinessException("VERIFICATION_CODE_EXPIRED", "验证码已过期或不存在");
        }
        
        if (!storedCode.equals(request.getVerificationCode())) {
            throw new BusinessException("INVALID_VERIFICATION_CODE", "验证码错误");
        }
        
        // 3. 更新用户密码
        userService.updatePassword(user.getId(), request.getNewPassword());
        
        // 4. 清除验证码
        VerificationCodeCache.put(request.getEmail(), null);
        
        return ResponseEntity.ok(ApiResponse.success("密码重置成功"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody RegisterRequest registerRequest) {
        System.out.println("接收到注册请求: " + registerRequest.getUsername() + ", " + registerRequest.getEmail());
        
        // 验证验证码
        String storedCode = VerificationCodeCache.get(registerRequest.getEmail());
        System.out.println("缓存中的验证码: " + storedCode);
        if (storedCode == null) {
            System.out.println("验证码已过期或不存在");
            throw new BusinessException("VERIFICATION_CODE_EXPIRED", "验证码已过期或不存在");
        }
        
        if (!storedCode.equals(registerRequest.getVerificationCode())) {
            System.out.println("验证码错误: 输入的验证码: " + registerRequest.getVerificationCode());
            throw new BusinessException("INVALID_VERIFICATION_CODE", "验证码错误");
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
        
        return ResponseEntity.ok(ApiResponse.success("注册成功"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody LoginRequest loginRequest) {
        // 1. 验证用户名和密码
        User user = userService.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户名不存在"));
        
        // 2. 验证密码
        boolean authenticated = userService.authenticate(
                loginRequest.getUsername(),
                loginRequest.getPassword()
        );
        
        if (!authenticated) {
            throw new BusinessException("INVALID_PASSWORD", "密码错误");
        }
        
        // 3. 设置登录状态
        UserContext.setLoggedIn(user);
        
        // 4. 返回用户信息
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setNickname(user.getNickname());
        userResponse.setAvatar(user.getAvatar());
        
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    // 获取用户信息
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<?>> getUserInfo() {
        // 使用UserContext获取当前用户
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("USER_NOT_AUTHENTICATED", "用户未登录");
        }
        
        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));
        
        // 创建用户信息响应对象，不包含密码等敏感信息
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setNickname(user.getNickname());
        userResponse.setAvatar(user.getAvatar());
        userResponse.setStatus(user.getStatus());
        userResponse.setCreatedAt(user.getCreatedAt().toString());
        userResponse.setUpdatedAt(user.getUpdatedAt().toString());

        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    // 更新用户信息
    @PutMapping("/user")
    public ResponseEntity<ApiResponse<?>> updateUserInfo(@RequestBody UserUpdateRequest request) {
        // 验证登录状态
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("USER_NOT_AUTHENTICATED", "用户未登录");
        }
        
        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));
        
        // 更新用户信息
        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            user.setNickname(request.getNickname());
        }
        
        // 如果用户上传了头像
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            user.setAvatar(request.getAvatar());
        }
        
        // 使用updateUserInfo方法更新用户信息
        user = userService.updateUserInfo(user.getUsername(), 
                                          request.getNickname() != null ? request.getNickname() : user.getNickname(),
                                          request.getAvatar() != null ? request.getAvatar() : user.getAvatar());
        
        // 返回更新后的用户信息
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setNickname(user.getNickname());
        userResponse.setAvatar(user.getAvatar());
        userResponse.setStatus(user.getStatus());
        userResponse.setCreatedAt(user.getCreatedAt().toString());
        userResponse.setUpdatedAt(user.getUpdatedAt().toString());
        
        return ResponseEntity.ok(ApiResponse.success(userResponse));
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


}