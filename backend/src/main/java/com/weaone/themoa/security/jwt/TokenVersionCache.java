package com.weaone.themoa.security.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * member.token_version을 짧은 TTL로 캐싱한다. 매 요청 DB를 읽으면 stateless JWT의 이점이 사라진다.
 * TTL만큼 무효화 반영이 늦어지지만(최대 30초) 그 창은 수용 범위로 본다.
 */
@Component
public class TokenVersionCache {

    private static final Duration TTL = Duration.ofSeconds(30);
    private static final int MAX_ENTRIES = 10_000;

    private final MemberRepository memberRepository;
    private final Cache<Long, Integer> cache;

    public TokenVersionCache(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(MAX_ENTRIES)
                .build();
    }

    /** 회원이 없으면 비어 있는 Optional. 캐시는 존재하는 회원만 담는다. */
    public Optional<Integer> find(Long memberId) {
        return Optional.ofNullable(
                cache.get(memberId, id -> memberRepository.findTokenVersionById(id).orElse(null))
        );
    }

    /** 토큰 버전을 올린 직후 옛 값이 남지 않도록 비운다. */
    public void evict(Long memberId) {
        cache.invalidate(memberId);
    }
}