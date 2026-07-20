package com.weaone.themoa.domain.recommend.ingest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 가입대상(join_member) 텍스트에서 나이/소득/취업유형/서민여부를 정규식으로 뽑아낸다.
 * 정규식으로 못 잡는 복잡한 문구는 후속으로 LLM 파싱을 붙일 수 있다(현재는 정규식만).
 */
public final class JoinTargetParser {

    private JoinTargetParser() {
    }

    // 나이 범위: "만19세~만34세", "19세~39세", "19~39세"(첫 숫자에 세 없음) 등.
    // 첫 '세'는 선택, 물결표는 여러 변형(~ ∼ 〜 ～) + 하이픈까지 허용.
    private static final Pattern AGE_RANGE =
            Pattern.compile("만?\\s*(\\d{1,3})\\s*세?\\s*[~∼〜～\\-]\\s*만?\\s*(\\d{1,3})\\s*세");
    private static final Pattern AGE_MIN_INCL = Pattern.compile("만?\\s*(\\d{1,3})\\s*세\\s*이상");
    private static final Pattern AGE_MIN_EXCL = Pattern.compile("만?\\s*(\\d{1,3})\\s*세\\s*초과");
    private static final Pattern AGE_MAX_EXCL = Pattern.compile("만?\\s*(\\d{1,3})\\s*세\\s*미만");
    private static final Pattern AGE_MAX_INCL = Pattern.compile("만?\\s*(\\d{1,3})\\s*세\\s*이하");

    // 소득(단위 만원): "연소득 3천만원 이하", "소득 5000만원 이하/이상"
    private static final Pattern INCOME_CHEONMAN =
            Pattern.compile("소득\\s*([\\d,]+)\\s*천\\s*만\\s*원?\\s*(이하|미만|이상|초과)");
    private static final Pattern INCOME_MAN =
            Pattern.compile("소득\\s*([\\d,]+)\\s*만\\s*원?\\s*(이하|미만|이상|초과)");

    private static final Pattern LOW_INCOME = Pattern.compile("차상위|서민|저소득|기초생활|기초수급");

    /** 파싱 결과(모두 못 찾으면 null). isForLowIncome은 텍스트 기준만(서민전용 join_restrict는 서비스에서 합산). */
    public record ParsedCondition(Integer minAge, Integer maxAge, Integer incomeLimit, Integer incomeMin,
                                  String employmentType, Boolean isForLowIncome) {
    }

    public static ParsedCondition parse(String joinTarget) {
        if (joinTarget == null || joinTarget.isBlank()) {
            return new ParsedCondition(null, null, null, null, "무관", null);
        }
        String t = joinTarget;

        Integer minAge = null;
        Integer maxAge = null;
        Matcher range = AGE_RANGE.matcher(t);
        if (range.find()) {
            minAge = toInt(range.group(1));
            maxAge = toInt(range.group(2));
        } else {
            Matcher m;
            if ((m = AGE_MIN_INCL.matcher(t)).find()) {
                minAge = toInt(m.group(1));
            } else if ((m = AGE_MIN_EXCL.matcher(t)).find()) {
                minAge = plusOne(toInt(m.group(1)));
            }
            if ((m = AGE_MAX_EXCL.matcher(t)).find()) {
                maxAge = minusOne(toInt(m.group(1)));
            } else if ((m = AGE_MAX_INCL.matcher(t)).find()) {
                maxAge = toInt(m.group(1));
            }
        }

        // 소득: "천만원"을 먼저 시도(만원 패턴에 먹히지 않도록), 없으면 "만원"
        Integer incomeLimit = null;
        Integer incomeMin = null;
        Matcher income = INCOME_CHEONMAN.matcher(t);
        int unit = 1000;
        if (!income.find()) {
            income = INCOME_MAN.matcher(t);
            unit = 1;
            if (!income.find()) {
                income = null;
            }
        }
        if (income != null) {
            Integer value = multiply(toInt(income.group(1)), unit);   // 만원 단위
            String direction = income.group(2);
            if ("이하".equals(direction) || "미만".equals(direction)) {
                incomeLimit = value;
            } else {
                incomeMin = value;
            }
        }

        String employmentType = detectEmployment(t);
        Boolean lowIncome = LOW_INCOME.matcher(t).find() ? Boolean.TRUE : null;

        return new ParsedCondition(minAge, maxAge, incomeLimit, incomeMin, employmentType, lowIncome);
    }

    private static String detectEmployment(String t) {
        if (t.contains("근로자") || t.contains("직장인") || t.contains("재직") || t.contains("근무")) {
            return "직장인";
        }
        if (t.contains("프리랜서")) {
            return "프리랜서";
        }
        return "무관";
    }

    private static Integer toInt(String s) {
        try {
            return Integer.valueOf(s.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer plusOne(Integer v) {
        return v == null ? null : v + 1;
    }

    private static Integer minusOne(Integer v) {
        return v == null ? null : v - 1;
    }

    private static Integer multiply(Integer v, int m) {
        return v == null ? null : v * m;
    }
}
