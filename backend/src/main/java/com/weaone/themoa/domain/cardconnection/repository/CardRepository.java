package com.weaone.themoa.domain.cardconnection.repository;

import com.weaone.themoa.domain.cardconnection.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByCardConnection_IdAndCardNumberMasked(Long cardConnectionId, String cardNumberMasked);
}
