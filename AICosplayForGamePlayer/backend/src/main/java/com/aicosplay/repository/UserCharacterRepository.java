package com.aicosplay.repository;

import com.aicosplay.entity.User;
import com.aicosplay.entity.UserCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {

    List<UserCharacter> findByUser(User user);
    boolean existsByUserIdAndGameCharacterId(Long userId, Long characterId);
    void deleteByUserIdAndGameCharacterId(Long userId, Long characterId);
}