package com.aicosplay.controller;

import com.aicosplay.entity.GameCharacter;
import com.aicosplay.service.GameCharacterService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<?> createCharacter(@RequestBody GameCharacter character) {
        try {
            GameCharacter createdCharacter = gameCharacterService.createCharacter(character);
            return ResponseEntity.ok(createdCharacter);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 获取所有角色
    @GetMapping
    public ResponseEntity<List<GameCharacter>> getAllCharacters() {
        List<GameCharacter> characters = gameCharacterService.getAllCharacters();
        return ResponseEntity.ok(characters);
    }

    // 根据ID获取角色
    @GetMapping("/{id}")
    public ResponseEntity<?> getCharacterById(@PathVariable Long id) {
        try {
            GameCharacter character = gameCharacterService.getCharacterById(id)
                    .orElseThrow(() -> new RuntimeException("Character not found"));
            return ResponseEntity.ok(character);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 更新角色
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCharacter(@PathVariable Long id, @RequestBody GameCharacter character) {
        try {
            GameCharacter updatedCharacter = gameCharacterService.updateCharacter(id, character);
            return ResponseEntity.ok(updatedCharacter);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 删除角色
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCharacter(@PathVariable Long id) {
        try {
            gameCharacterService.deleteCharacter(id);
            return ResponseEntity.ok(new AuthController.ApiResponse(true, "Character deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }
}