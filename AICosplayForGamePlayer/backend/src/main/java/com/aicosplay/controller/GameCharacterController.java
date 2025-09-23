package com.aicosplay.controller;

import com.aicosplay.entity.GameCharacter;
import com.aicosplay.entity.User;
import com.aicosplay.service.GameCharacterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
public class GameCharacterController {
    @Autowired
    private GameCharacterService gameCharacterService;

    // 获取当前登录用户
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    // 创建新角色
    @PostMapping
    public ResponseEntity<?> createCharacter(@RequestBody GameCharacter character) {
        try {
            User currentUser = getCurrentUser();
            // 设置创建者
            character.setUser(currentUser);
            // 自定义角色的isPreset默认为false
            character.setIsPreset(false);
            GameCharacter createdCharacter = gameCharacterService.createCharacter(character);
            return new ResponseEntity<>(createdCharacter, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    // 获取用户可见的所有角色（预设角色+当前用户的自定义角色）
    @GetMapping
    public ResponseEntity<List<GameCharacter>> getVisibleCharacters() {
        User currentUser = getCurrentUser();
        List<GameCharacter> characters = gameCharacterService.getVisibleCharacters(currentUser);
        return new ResponseEntity<>(characters, HttpStatus.OK);
    }

    // 根据ID获取角色
    @GetMapping("/{id}")
    public ResponseEntity<?> getCharacterById(@PathVariable Long id) {
        try {
            GameCharacter character = gameCharacterService.getCharacterById(id)
                    .orElseThrow(() -> new RuntimeException("Character not found"));
            User currentUser = getCurrentUser();
            // 检查用户是否有权限查看该角色
            if (!character.getIsPreset() && (currentUser == null || !currentUser.getId().equals(character.getUser().getId()))) {
                return new ResponseEntity<>(new ApiResponse(false, "无权访问该角色"), HttpStatus.FORBIDDEN);
            }
            return new ResponseEntity<>(character, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    // 更新角色
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCharacter(@PathVariable Long id, @RequestBody GameCharacter character) {
        try {
            GameCharacter existingCharacter = gameCharacterService.getCharacterById(id)
                    .orElseThrow(() -> new RuntimeException("Character not found"));
            User currentUser = getCurrentUser();
            // 检查用户是否有权限更新该角色
            if (!existingCharacter.getIsPreset() && (currentUser == null || !currentUser.getId().equals(existingCharacter.getUser().getId()))) {
                return new ResponseEntity<>(new ApiResponse(false, "无权更新该角色"), HttpStatus.FORBIDDEN);
            }
            GameCharacter updatedCharacter = gameCharacterService.updateCharacter(id, character);
            return new ResponseEntity<>(updatedCharacter, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    // 删除角色
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCharacter(@PathVariable Long id) {
        try {
            GameCharacter existingCharacter = gameCharacterService.getCharacterById(id)
                    .orElseThrow(() -> new RuntimeException("Character not found"));
            User currentUser = getCurrentUser();
            // 检查用户是否有权限删除该角色
            if (!existingCharacter.getIsPreset() && (currentUser == null || !currentUser.getId().equals(existingCharacter.getUser().getId()))) {
                return new ResponseEntity<>(new ApiResponse(false, "无权删除该角色"), HttpStatus.FORBIDDEN);
            }
            // 不允许删除预设角色
            if (existingCharacter.getIsPreset()) {
                return new ResponseEntity<>(new ApiResponse(false, "预设角色不能删除"), HttpStatus.BAD_REQUEST);
            }
            gameCharacterService.deleteCharacter(id);
            return new ResponseEntity<>(new ApiResponse(true, "角色删除成功"), HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    // ApiResponse 内部类，用于返回操作结果
    private static class ApiResponse {
        private boolean success;
        private String message;

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}