package com.aicosplay.service;

import com.aicosplay.entity.GameCharacter;
import com.aicosplay.entity.User;
import com.aicosplay.repository.GameCharacterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GameCharacterService {

    @Autowired
    private GameCharacterRepository gameCharacterRepository;

    // 创建新游戏角色
    public GameCharacter createCharacter(GameCharacter character) {
        // 检查角色名称是否已存在
        if (gameCharacterRepository.existsByName(character.getName())) {
            throw new RuntimeException("Character name already exists");
        }
        return gameCharacterRepository.save(character);
    }

    // 获取所有游戏角色（管理员用）
    public List<GameCharacter> getAllCharacters() {
        return gameCharacterRepository.findAll();
    }

    // 获取预设角色
    public List<GameCharacter> getPresetCharacters() {
        return gameCharacterRepository.findByIsPresetTrue();
    }

    // 获取用户自定义角色
    public List<GameCharacter> getUserCharacters(Long userId) {
        return gameCharacterRepository.findByUserId(userId);
    }

    // 获取用户可见的所有角色（预设角色+当前用户的自定义角色）
    public List<GameCharacter> getVisibleCharacters(User user) {
        List<GameCharacter> allVisibleCharacters = new ArrayList<>();
        // 添加所有预设角色
        allVisibleCharacters.addAll(getPresetCharacters());
        // 添加当前用户的自定义角色
        if (user != null) {
            allVisibleCharacters.addAll(getUserCharacters(user.getId()));
        }
        return allVisibleCharacters;
    }

    // 根据ID获取游戏角色
    public Optional<GameCharacter> getCharacterById(Long id) {
        return gameCharacterRepository.findById(id);
    }

    // 根据名称获取游戏角色
    public Optional<GameCharacter> getCharacterByName(String name) {
        return gameCharacterRepository.findByName(name);
    }

    // 更新游戏角色
    public GameCharacter updateCharacter(Long id, GameCharacter characterDetails) {
        GameCharacter character = gameCharacterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Character not found"));

        // 检查是否要更新名称且新名称已存在
        if (!character.getName().equals(characterDetails.getName()) && 
            gameCharacterRepository.existsByName(characterDetails.getName())) {
            throw new RuntimeException("Character name already exists");
        }

        character.setName(characterDetails.getName());
        character.setPrompt(characterDetails.getPrompt());

        return gameCharacterRepository.save(character);
    }

    // 删除游戏角色
    public void deleteCharacter(Long id) {
        GameCharacter character = gameCharacterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Character not found"));
        gameCharacterRepository.delete(character);
    }
}