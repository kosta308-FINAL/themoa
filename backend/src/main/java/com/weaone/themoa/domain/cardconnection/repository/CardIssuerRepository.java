package com.weaone.themoa.domain.cardconnection.repository;

import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardIssuerRepository extends JpaRepository<CardIssuer, String> {
}
