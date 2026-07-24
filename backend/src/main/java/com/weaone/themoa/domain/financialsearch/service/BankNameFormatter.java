package com.weaone.themoa.domain.financialsearch.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 금융회사명을 사용자에게 친숙한 표시명으로 바꾼다.
 *
 * <p>금융감독원 공시는 법적 등록명("중소기업은행")이나 법인 표기가 붙은 이름("주식회사 카카오뱅크")으로
 * 데이터를 주는데, 사용자는 통칭("IBK기업은행", "카카오뱅크")으로 알고 있어 낯설다. 흔히 헷갈리는 몇 곳만
 * 매핑하고, 나머지는 "주식회사/주식회사 " 같은 법인 접두어만 정리한다.
 *
 * <p>표시용 변환일 뿐, 저장·매칭·링크 조회에 쓰는 원본 회사명은 그대로 둔다(공시 데이터와 어긋나지 않도록).
 */
@Component
public class BankNameFormatter {

    // 정식명 → 통칭. 사용자가 특히 헷갈리는 곳만 담는다(필요하면 여기에 추가).
    private static final Map<String, String> DISPLAY_NAME = Map.ofEntries(
            Map.entry("중소기업은행", "IBK기업은행"),
            Map.entry("아이엠뱅크", "iM뱅크(대구은행)"),
            Map.entry("한국산업은행", "KDB산업은행"),
            Map.entry("농협은행주식회사", "농협은행"),
            Map.entry("엔에이치저축은행", "NH저축은행"),
            Map.entry("디비저축은행", "DB저축은행"));

    /** 화면에 보여줄 회사명. 매핑에 있으면 통칭으로, 없으면 법인 접두어만 정리한다. */
    public String toDisplayName(String companyName) {
        if (!StringUtils.hasText(companyName)) {
            return companyName;
        }
        String name = companyName.trim();
        String mapped = DISPLAY_NAME.get(name);
        if (mapped != null) {
            return mapped;
        }
        // "주식회사 카카오뱅크" → "카카오뱅크" 처럼 법인 접두어만 떼어낸다.
        return name.replaceFirst("^주식회사\\s*", "").trim();
    }
}
