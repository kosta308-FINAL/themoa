package com.weaone.themoa.domain.policy.policy.region;

public enum RegionCompatibility {
    EXACT_SIGUNGU("정확한 시·군·자치구 정책", 0, 50),
    MULTIPLE_SIGUNGU_MATCH("복수 지역 중 사용자 시·군·자치구 포함", 1, 45),
    CHILD_SIGUNGU_MATCH("사용자 시·도 하위 시·군·자치구 정책", 1, 40),
    MULTIPLE_CHILD_SIGUNGU_MATCH("복수 지역 중 사용자 시·도 하위 시·군·자치구 포함", 2, 38),
    EXACT_SIDO("시·도 전체 정책", 2, 35),
    PARENT_SIDO("상위 시·도 전체 정책", 2, 35),
    MULTIPLE_SIDO_MATCH("복수 지역 중 사용자 시·도 포함", 3, 30),
    NATIONWIDE("전국 정책", 4, 15),
    REGION_UNSPECIFIED("지역 제한 미지정 정책", 5, 5),
    MULTIPLE_REGION_MATCH("복수 지역 중 사용자 지역 포함", 3, 30),
    UNKNOWN("지역 확인 필요", 6, 0),
    NOT_MATCHED("다른 지역 정책", 7, 0);

    private final String label;
    private final int priority;
    private final int recommendationScore;

    RegionCompatibility(String label, int priority, int recommendationScore) {
        this.label = label;
        this.priority = priority;
        this.recommendationScore = recommendationScore;
    }

    public String label() {
        return label;
    }

    public int priority() {
        return priority;
    }

    public int recommendationScore() {
        return recommendationScore;
    }
}
