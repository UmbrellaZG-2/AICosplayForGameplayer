package com.aicosplay.controller;

import com.aicosplay.entity.GameCharacter;
import com.aicosplay.entity.User;
import com.aicosplay.exception.BusinessException;
import com.aicosplay.model.ApiResponse;
import com.aicosplay.service.UserService;
import com.aicosplay.service.UserCharacterService;
import com.aicosplay.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user-characters")
public class UserCharacterController {

    @Autowired
    private UserCharacterService userCharacterService;

    @Autowired
    private UserService userService;

    // 用户添加角色
    @PostMapping
    public ResponseEntity<ApiResponse<?>> addCharacter(@RequestBody CharacterRequest request) {
        // 使用UserContext获取当前用户
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException("USER_NOT_AUTHENTICATED", "用户未登录");
        }
        
        userCharacterService.addCharacterToUser(user, request.getCharacterId());
        return ResponseEntity.ok(ApiResponse.success("角色添加成功"));
    }

    // 获取用户拥有的所有角色
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getUserCharacters() {
        // 使用UserContext获取当前用户
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException("USER_NOT_AUTHENTICATED", "用户未登录");
        }
        
        List<GameCharacter> characters = userCharacterService.getUserCharacters(user);
        return ResponseEntity.ok(ApiResponse.success(characters));
    }

    // 移除用户的一个角色
    @DeleteMapping("/{characterId}")
    public ResponseEntity<ApiResponse<?>> removeCharacter(@PathVariable Long characterId) {
        // 使用UserContext获取当前用户
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException("USER_NOT_AUTHENTICATED", "用户未登录");
        }
        
        userCharacterService.removeCharacterFromUser(user, characterId);
        return ResponseEntity.ok(ApiResponse.success("角色移除成功"));
    }

    // 检查用户是否拥有某个角色
    @GetMapping("/check/{characterId}")
    public ResponseEntity<ApiResponse<?>> checkCharacter(@PathVariable Long characterId) {
        // 使用UserContext获取当前用户
        User user = UserContext.getCurrentUser();
        if (user == null) {
            throw new BusinessException("USER_NOT_AUTHENTICATED", "用户未登录");
        }
        
        boolean hasCharacter = userCharacterService.hasCharacter(user, characterId);
        return ResponseEntity.ok(ApiResponse.success(hasCharacter));
    }

    // 请求DTO类
    public static class CharacterRequest {
        private Long characterId;

        // Getters and Setters
        public Long getCharacterId() { return characterId; }
        public void setCharacterId(Long characterId) { this.characterId = characterId; }
    }


}