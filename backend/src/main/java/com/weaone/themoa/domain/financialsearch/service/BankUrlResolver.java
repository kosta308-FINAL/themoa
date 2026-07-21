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
    private Map<String, String> cache = Map.of();

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

    public String resolve(String companyName) {
        if (!StringUtils.hasText(companyName)) {
            return null;
        }
        String direct = cache.get(companyName);
        if (direct != null) {
            return direct;
        }
        // 정확히 일치하지 않아도("농협은행" vs "농협은행주식회사") 부분 포함이면 매칭
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            if (companyName.contains(entry.getKey()) || entry.getKey().contains(companyName)) {
                return entry.getValue();
            }
        }
        String encoded = URLEncoder.encode(companyName + " 공식 홈페이지", StandardCharsets.UTF_8);
        return "https://www.google.com/search?q=" + encoded;
    }
}
