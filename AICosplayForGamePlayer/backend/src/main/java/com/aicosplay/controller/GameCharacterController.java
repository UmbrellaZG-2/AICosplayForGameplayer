package com.aicosplay.controller;

import com.aicosplay.entity.GameCharacter;
import com.aicosplay.entity.User;
import com.aicosplay.exception.UnauthorizedException;
import com.aicosplay.model.ApiResponse;
import com.aicosplay.service.GameCharacterService;
import com.aicosplay.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
public class GameCharacterController {
    @Autowired
    private GameCharacterService gameCharacterService;

    // 创建新角色
    @PostMapping
    public ResponseEntity<ApiResponse<GameCharacter>> createCharacter(@RequestBody GameCharacter character) {
        // 获取当前登录用户
        User currentUser = UserContext.getCurrentUser();
        
        // 设置创建者
        character.setUser(currentUser);
        // 自定义角色的isPreset默认为0
        character.setIsPreset((byte) 0);
        GameCharacter createdCharacter = gameCharacterService.createCharacter(character);
        return ResponseEntity.ok(ApiResponse.success(createdCharacter));
    }

    // 获取用户可见的所有角色（预设角色+当前用户的自定义角色）
    @GetMapping
    public ResponseEntity<ApiResponse<List<GameCharacter>>> getVisibleCharacters() {
        User currentUser = UserContext.isUserLoggedIn() ? UserContext.getCurrentUser() : null;
        List<GameCharacter> characters = gameCharacterService.getVisibleCharacters(currentUser);
        return ResponseEntity.ok(ApiResponse.success(characters));
    }

    // 根据ID获取角色
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GameCharacter>> getCharacterById(@PathVariable Long id) {
        GameCharacter character = gameCharacterService.getCharacterById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        
        // 检查用户是否有权限查看该角色
        if (character.getIsPreset() == (byte) 0) {
            User currentUser = UserContext.getCurrentUser();
            if (!currentUser.getId().equals(character.getUser().getId())) {
                throw new RuntimeException("您没有权限访问该角色");
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success(character));
    }

    // 更新角色
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GameCharacter>> updateCharacter(@PathVariable Long id, @RequestBody GameCharacter character) {
        GameCharacter existingCharacter = gameCharacterService.getCharacterById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        
        // 检查用户是否有权限更新该角色
        if (existingCharacter.getIsPreset() == (byte) 0) {
            User currentUser = UserContext.getCurrentUser();
            if (!currentUser.getId().equals(existingCharacter.getUser().getId())) {
                throw new RuntimeException("您没有权限更新该角色");
            }
        }
        
        GameCharacter updatedCharacter = gameCharacterService.updateCharacter(id, character);
        return ResponseEntity.ok(ApiResponse.success(updatedCharacter));
    }

    // 删除角色
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteCharacter(@PathVariable Long id) {
        GameCharacter existingCharacter = gameCharacterService.getCharacterById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        
        // 检查用户是否有权限删除该角色
        if (existingCharacter.getIsPreset() == (byte) 0) {
            User currentUser = UserContext.getCurrentUser();
            if (!currentUser.getId().equals(existingCharacter.getUser().getId())) {
                throw new RuntimeException("您没有权限删除该角色");
            }
        }
        
        // 不允许删除预设角色
        if (existingCharacter.getIsPreset() == (byte) 1) {
            throw new RuntimeException("预设角色不能删除");
        }
        
        gameCharacterService.deleteCharacter(id);
        return ResponseEntity.ok(ApiResponse.success("角色删除成功"));
    }

}