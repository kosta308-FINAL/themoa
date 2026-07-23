package com.weaone.themoa.domain.financialsearch.service;

import com.weaone.themoa.domain.financialsearch.entity.BankLink;
import com.weaone.themoa.domain.financialsearch.repository.BankLinkRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

// 은행 공식 홈페이지 링크 매핑. financial_bank_link 테이블에서 읽어온다(코드가 아니라 DB에 저장돼 있어서,
// 확인된 URL이 늘어날 때마다 재배포 없이 INSERT만 하면 됨). 테이블에 없는 곳은 검증된 URL이 없다는 뜻이라
// 은행명 검색 링크로 대체한다 — 정확한 공식 URL을 보장하진 않는다.
@Component
public class BankUrlResolver {

    private final BankLinkRepository bankLinkRepository;
    // 관리자 화면에서 링크를 바꾸면 refresh()로 교체된다. 요청 스레드가 즉시 최신 맵을 보도록 volatile.
    private volatile Map<String, String> cache = Map.of();

    public BankUrlResolver(BankLinkRepository bankLinkRepository) {
        this.bankLinkRepository = bankLinkRepository;
    }

    @PostConstruct
    void loadCache() {
        Map<String, String> loaded = new LinkedHashMap<>();
        for (BankLink link : bankLinkRepository.findAll()) {
            loaded.put(link.getCompanyName(), link.getOfficialUrl());
        }
        this.cache = loaded;
    }

    /**
     * 링크가 추가·수정·삭제된 뒤 캐시를 다시 읽는다.
     * 이게 없으면 관리자가 링크를 등록해도 서버를 재시작하기 전까지 반영되지 않는다.
     */
    public void refresh() {
        loadCache();
    }

    /** 등록된 공식 URL이 있으면 그 값, 없으면 은행명 검색 링크(정확한 공식 URL을 보장하지 않음). */
    public String resolve(String companyName) {
        if (!StringUtils.hasText(companyName)) {
            return null;
        }
        String registered = findRegistered(companyName);
        if (registered != null) {
            return registered;
        }
        String encoded = URLEncoder.encode(companyName + " 공식 홈페이지", StandardCharsets.UTF_8);
        return "https://www.google.com/search?q=" + encoded;
    }

    /**
     * 검증된 공식 URL로 연결되는지 여부. false면 검색 링크로 대체되고 있다는 뜻이라,
     * 관리자 화면에서 "링크가 필요한 은행" 목록을 뽑는 데 쓴다.
     */
    public boolean hasVerifiedUrl(String companyName) {
        return findRegistered(companyName) != null;
    }

    /** 등록된 링크 조회. 정확히 일치하지 않아도("농협은행" vs "농협은행주식회사") 부분 포함이면 매칭한다. */
    private String findRegistered(String companyName) {
        if (!StringUtils.hasText(companyName)) {
            return null;
        }
        String direct = cache.get(companyName);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            if (companyName.contains(entry.getKey()) || entry.getKey().contains(companyName)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
