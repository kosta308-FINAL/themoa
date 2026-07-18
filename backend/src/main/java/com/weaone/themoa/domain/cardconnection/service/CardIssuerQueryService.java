package com.weaone.themoa.domain.cardconnection.service;

import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** S-00C 지원 카드사 화이트리스트 조회(dayguide.md §8.1, connection.md §2-1). */
@Service
@RequiredArgsConstructor
public class CardIssuerQueryService {

    private final CardIssuerRepository cardIssuerRepository;

    @Transactional(readOnly = true)
    public List<CardIssuer> listAll() {
        return cardIssuerRepository.findAll().stream()
                .sorted(Comparator.comparing(CardIssuer::getName))
                .toList();
    }
}
