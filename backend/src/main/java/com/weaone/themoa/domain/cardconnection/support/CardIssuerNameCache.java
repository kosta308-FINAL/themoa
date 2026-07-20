package com.weaone.themoa.domain.cardconnection.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * card_issuer는 CardIssuerSeeder가 기동 시 심어두는 고정 화이트리스트 10행이라 사실상 불변이다(만료 없이 캐싱).
 * 초기수집 상태 폴링처럼 짧은 주기로 반복 호출되는 경로가 매번 JPA lazy 로딩으로 재조회하지 않도록 이름만 따로 캐싱한다.
 */
@Component
public class CardIssuerNameCache {

    private final CardIssuerRepository cardIssuerRepository;
    private final Cache<String, Optional<String>> cache;

    public CardIssuerNameCache(CardIssuerRepository cardIssuerRepository) {
        this.cardIssuerRepository = cardIssuerRepository;
        this.cache = Caffeine.newBuilder().maximumSize(100).build();
    }

    public Optional<String> findName(String organization) {
        return cache.get(organization, org -> cardIssuerRepository.findById(org).map(CardIssuer::getName));
    }
}
