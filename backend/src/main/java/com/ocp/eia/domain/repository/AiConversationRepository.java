package com.ocp.eia.domain.repository;

import com.ocp.eia.domain.model.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {

    List<AiConversation> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<AiConversation> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT c FROM AiConversation c
            LEFT JOIN FETCH c.messages
            WHERE c.id = :id AND c.user.id = :userId
            """)
    Optional<AiConversation> findByIdAndUserIdWithMessages(@Param("id") UUID id, @Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}
