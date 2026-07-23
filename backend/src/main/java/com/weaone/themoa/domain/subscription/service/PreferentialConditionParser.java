package com.weaone.themoa.domain.subscription.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 우대조건 원문(finlife spcl_cnd)을 "설명 + 가산금리(%p)" 항목들로 쪼갠다.
 *
 * <p>은행마다 표기가 제각각이고(ㄱ. / - / ① / * …) 금액 구간·합산 상한 같은 규칙이 텍스트로만 들어있어,
 * 이 파싱 결과는 어디까지나 <b>초안</b>이다. 화면에서 사용자가 값을 확인·수정한 뒤 확정한다.
 *
 * <p>줄 단위로 보고, 한 줄에서 "0.3%p" 같은 가산금리를 찾으면 그 줄을 한 항목으로 만든다. 단
 * "최고우대금리 0.45%p"처럼 <b>조건이 아니라 상한을 안내하는 줄</b>은 제외한다 — 이 값을 조건으로
 * 오인해 더하면 실제보다 금리가 부풀려진다.
 */
@Component
public class PreferentialConditionParser {

    // "0.3%p", "0.30 %", "연 0.1%p" 등에서 숫자를 뽑는다. %p가 정석이지만 %만 쓰는 곳도 있어 둘 다 받는다.
    private static final Pattern RATE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%\\s*p?");
    // 줄 앞머리 글머리 기호만 떼어낸다(한글 조건 문구가 잘리지 않도록 한글은 건드리지 않는다).
    private static final Pattern BULLET_PREFIX = Pattern.compile("^\\s*(?:[-*·▷▸○●□■◆▪]|[0-9]{1,2}[.)]|[①-⑳]|[가-힣][.)])\\s*");
    private static final int MAX_ITEMS = 20;
    private static final int MIN_DESC_LENGTH = 2;
    private static final int MAX_DESC_LENGTH = 200;

    /** 파싱된 우대조건 항목 1건. */
    public record ParsedCondition(String description, BigDecimal ratePercent) {
    }

    public List<ParsedCondition> parse(String specialCondition) {
        if (!StringUtils.hasText(specialCondition)) {
            return List.of();
        }
        List<ParsedCondition> items = new ArrayList<>();
        for (String rawLine : specialCondition.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher matcher = RATE_PATTERN.matcher(line);
            if (!matcher.find()) {
                continue; // 금리 표기가 없는 줄(머리말·안내문)은 항목이 아니다.
            }
            if (isLimitLine(line)) {
                continue; // "…우대금리 제공: 최고 3%p" 같은 상한 안내는 개별 조건이 아니라 총량이다.
            }
            String description = cleanDescription(line);
            if (description.length() < MIN_DESC_LENGTH) {
                continue; // 설명이 사실상 없는 줄(기호+숫자만 남은 경우)은 버린다.
            }
            items.add(new ParsedCondition(description, new BigDecimal(matcher.group(1))));
            if (items.size() >= MAX_ITEMS) {
                break;
            }
        }
        return items;
    }

    /**
     * "최고/최대/합산"이 들어간 줄은 개별 충족 조건이 아니라 상한(총량)을 안내하는 공지로 보고 제외한다.
     * (예: "…우대금리 제공: 최고 3%p" — 이 줄 자체는 조건이 아니고, 실제 조건은 그 아래에 따로 나열된다)
     */
    private boolean isLimitLine(String line) {
        String text = line.replaceAll("\\s+", "");
        return text.contains("최고") || text.contains("최대") || text.contains("합산");
    }

    /** 설명에서 앞머리 글머리 기호와 금리 표기를 걷어내 사람이 읽을 문구만 남긴다. */
    private String cleanDescription(String line) {
        String cleaned = BULLET_PREFIX.matcher(line).replaceFirst("");
        // 금리 표기와, 그 앞에 붙는 "연"·구분기호를 함께 떼어낸다("… 가입: 연 0.05%p" → "… 가입").
        cleaned = cleaned.replaceAll("[:：]?\\s*연?\\s*[+]?\\d+(?:\\.\\d+)?\\s*%\\s*p?", " ").trim();
        cleaned = cleaned.replaceAll("[:：\\-–>]+\\s*$", "").trim();
        cleaned = cleaned.replaceAll("\\s{2,}", " ");
        if (cleaned.length() > MAX_DESC_LENGTH) {
            cleaned = cleaned.substring(0, MAX_DESC_LENGTH);
        }
        return cleaned;
    }
}
