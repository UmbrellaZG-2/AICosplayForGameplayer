package com.aicosplay.service;

import com.aicosplay.entity.User;
import com.aicosplay.entity.GameCharacter;
import com.aicosplay.entity.UserCharacter;
import com.aicosplay.repository.UserCharacterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserCharacterService {

    @Autowired
    private UserCharacterRepository userCharacterRepository;

    @Autowired
    private GameCharacterService gameCharacterService;

    // 用户添加一个角色
    public UserCharacter addCharacterToUser(User user, Long characterId) {
        // 检查角色是否存在
        GameCharacter character = gameCharacterService.getCharacterById(characterId)
                .orElseThrow(() -> new RuntimeException("Character not found"));

        // 检查用户是否已经拥有该角色
        if (userCharacterRepository.existsByUserIdAndGameCharacterId(user.getId(), characterId)) {
            throw new RuntimeException("User already has this character");
        }

        UserCharacter userCharacter = new UserCharacter();
        userCharacter.setUser(user);
        userCharacter.setGameCharacter(character);
        userCharacter.setCreatedAt(LocalDateTime.now());

        return userCharacterRepository.save(userCharacter);
    }

    // 获取用户拥有的所有角色
    public List<GameCharacter> getUserCharacters(User user) {
        List<UserCharacter> userCharacters = userCharacterRepository.findByUser(user);
        return userCharacters.stream()
                .map(UserCharacter::getGameCharacter)
                .collect(Collectors.toList());
    }

    // 检查用户是否拥有某个角色
    public boolean hasCharacter(User user, Long characterId) {
        return userCharacterRepository.existsByUserIdAndGameCharacterId(user.getId(), characterId);
    }

    // 移除用户的一个角色
    public void removeCharacterFromUser(User user, Long characterId) {
        // 检查关系是否存在
        if (!userCharacterRepository.existsByUserIdAndGameCharacterId(user.getId(), characterId)) {
            throw new RuntimeException("User does not have this character");
        }

        userCharacterRepository.deleteByUserIdAndGameCharacterId(user.getId(), characterId);
    }

    // 获取用户角色关系的详情
    public Optional<UserCharacter> getUserCharacterDetail(User user, Long characterId) {
        // 查找用户和角色的关系
        List<UserCharacter> userCharacters = userCharacterRepository.findByUser(user);
        return userCharacters.stream()
                .filter(uc -> uc.getGameCharacter().getId().equals(characterId))
                .findFirst();
    }
}