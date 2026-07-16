package com.weaone.themoa.domain.cardconnection.repository;

import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CardConnectionRepository extends JpaRepository<CardConnection, Long> {

    Optional<CardConnection> findByMember_IdAndCardIssuer_Organization(Long memberId, String organization);

    List<CardConnection> findByMember_IdAndStatus(Long memberId, ConnectionStatus status);

    /** 온디맨드 수집 대상(cardtransaction.md §6): 새벽 배치와 동일하게 자동수집 OFF 회원은 제외한다. */
    List<CardConnection> findByMember_IdAndStatusAndMember_CardSyncEnabled(
            Long memberId, ConnectionStatus status, boolean cardSyncEnabled);

    boolean existsByMember_IdAndStatus(Long memberId, ConnectionStatus status);

    /** 새벽 배치 대상(cardtransaction.md §6): 활성 커넥션 + 자동수집 ON + 마지막 이용 30일 이내. */
    @Query("select c from CardConnection c where c.status = :status "
            + "and c.member.cardSyncEnabled = true and c.member.lastActiveAt >= :activeSince")
    List<CardConnection> findEligibleForNightlyBatch(@Param("status") ConnectionStatus status,
                                                       @Param("activeSince") LocalDateTime activeSince);
}
