package com.weaone.themoa.domain.cardconnection.repository;

import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardConnectionRepository extends JpaRepository<CardConnection, Long> {

    Optional<CardConnection> findByMember_IdAndCardIssuer_Organization(Long memberId, String organization);
}
