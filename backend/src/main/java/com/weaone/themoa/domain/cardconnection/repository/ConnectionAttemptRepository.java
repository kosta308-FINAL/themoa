package com.weaone.themoa.domain.cardconnection.repository;

import com.weaone.themoa.domain.cardconnection.entity.ConnectionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConnectionAttemptRepository extends JpaRepository<ConnectionAttempt, Long> {

    Optional<ConnectionAttempt> findByMember_IdAndCardIssuer_Organization(Long memberId, String organization);
}
