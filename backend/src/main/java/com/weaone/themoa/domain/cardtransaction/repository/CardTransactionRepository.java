package com.weaone.themoa.domain.cardtransaction.repository;

import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {

    /** 멱등성 대조 키(cardtransaction.md §2). SYNC 건만 이 키가 전부 채워진다. */
    Optional<CardTransaction> findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(
            Long memberId, Long cardId, LocalDate usedDate, LocalDateTime usedAt, String approvalNo);

    Optional<CardTransaction> findByIdAndMember_Id(Long id, Long memberId);

    Page<CardTransaction> findByMember_IdOrderByUsedAtDesc(Long memberId, Pageable pageable);
}
