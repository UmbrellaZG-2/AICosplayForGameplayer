package com.aicosplay.repository;

import com.aicosplay.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameCharacterRepository extends JpaRepository<GameCharacter, Long> {

    Optional<GameCharacter> findByName(String name);
    boolean existsByName(String name);
    List<GameCharacter> findByIsPresetTrue();
    List<GameCharacter> findByUserId(Long userId);
}