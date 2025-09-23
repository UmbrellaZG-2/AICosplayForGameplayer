package com.aicosplay.repository;

import com.aicosplay.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GameCharacterRepository extends JpaRepository<GameCharacter, Long> {

    Optional<GameCharacter> findByName(String name);
    boolean existsByName(String name);
}