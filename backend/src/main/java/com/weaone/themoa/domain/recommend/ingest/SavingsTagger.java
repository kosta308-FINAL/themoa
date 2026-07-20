package com.weaone.themoa.domain.recommend.ingest;

/**
 * 예·적금 자동 태깅(규칙 기반): 비대면가입여부 / 우대조건 난이도 / 사회초년생 적합.
 */
public final class SavingsTagger {

    private SavingsTagger() {
    }

    /** 가입방법에 인터넷/스마트폰/모바일/비대면이 있으면 비대면 가입 가능. */
    public static Boolean isOnline(String joinMethod) {
        if (joinMethod == null) {
            return false;
        }
        return joinMethod.contains("인터넷") || joinMethod.contains("스마트폰")
                || joinMethod.contains("모바일") || joinMethod.contains("비대면");
    }

    /**
     * 우대조건 난이도.
     * - 없음/"해당없음" 류 → 쉬움
     * - 120자 초과(조건이 많고 긺) → 까다로움
     * - 그 외 → 보통
     */
    public static String difficulty(String specialCondition) {
        if (isNone(specialCondition)) {
            return "쉬움";
        }
        return specialCondition.length() > 120 ? "까다로움" : "보통";
    }

    /**
     * 사회초년생 적합 여부.
     * - 상품명/가입대상에 청년·사회초년생·영(Young) 키워드가 있거나
     * - 최대나이가 39세 이하(청년 대상으로 나이 상한이 낮은 상품)면 적합.
     */
    public static Boolean youthFriendly(String productName, String joinTarget, Integer minAge, Integer maxAge) {
        String text = (nz(productName) + " " + nz(joinTarget));
        if (text.contains("청년") || text.contains("사회초년") || text.toLowerCase().contains("young")) {
            return true;
        }
        return maxAge != null && maxAge <= 39;
    }

    private static boolean isNone(String s) {
        if (s == null || s.isBlank()) {
            return true;
        }
        String normalized = s.replaceAll("[\\s▶*·\\-]", "");
        return normalized.startsWith("해당없음") || normalized.startsWith("해당사항없음")
                || normalized.startsWith("해당무") || normalized.startsWith("우대조건없음")
                || normalized.equals("없음");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
