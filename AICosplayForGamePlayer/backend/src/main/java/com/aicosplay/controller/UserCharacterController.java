package com.aicosplay.controller;

import com.aicosplay.entity.GameCharacter;
import com.aicosplay.entity.User;
import com.aicosplay.service.UserService;
import com.aicosplay.service.UserCharacterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user-characters")
public class UserCharacterController {

    @Autowired
    private UserCharacterService userCharacterService;

    @Autowired
    private UserService userService;

    // 用户添加角色
    @PostMapping
    public ResponseEntity<?> addCharacter(@RequestBody CharacterRequest request, HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            userCharacterService.addCharacterToUser(user, request.getCharacterId());
            return ResponseEntity.ok(new AuthController.ApiResponse(true, "Character added successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 获取用户拥有的所有角色
    @GetMapping
    public ResponseEntity<?> getUserCharacters(HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<GameCharacter> characters = userCharacterService.getUserCharacters(user);
            return ResponseEntity.ok(characters);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 移除用户的一个角色
    @DeleteMapping("/{characterId}")
    public ResponseEntity<?> removeCharacter(@PathVariable Long characterId, HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            userCharacterService.removeCharacterFromUser(user, characterId);
            return ResponseEntity.ok(new AuthController.ApiResponse(true, "Character removed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 检查用户是否拥有某个角色
    @GetMapping("/check/{characterId}")
    public ResponseEntity<?> checkCharacter(@PathVariable Long characterId, HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean hasCharacter = userCharacterService.hasCharacter(user, characterId);
            return ResponseEntity.ok(new CharacterCheckResponse(hasCharacter));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 请求DTO类
    public static class CharacterRequest {
        private Long characterId;

        // Getters and Setters
        public Long getCharacterId() { return characterId; }
        public void setCharacterId(Long characterId) { this.characterId = characterId; }
    }

    // 检查角色响应DTO
    public static class CharacterCheckResponse {
        private boolean hasCharacter;

        public CharacterCheckResponse(boolean hasCharacter) {
            this.hasCharacter = hasCharacter;
        }

        public boolean isHasCharacter() { return hasCharacter; }
        public void setHasCharacter(boolean hasCharacter) { this.hasCharacter = hasCharacter; }
    }
}