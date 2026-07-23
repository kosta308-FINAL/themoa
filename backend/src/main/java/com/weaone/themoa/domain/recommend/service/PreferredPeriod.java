package com.weaone.themoa.domain.recommend.service;

/** 선호 가입기간 (financial_profile.preferred_period). 대표 구간: 초단기 1M / 단기 2~6M / 중기 7~24M / 장기 >24M. */
public enum PreferredPeriod {
    SINGLE(0),  // 1개월(초단기)
    SHORT(1),   // 단기
    MID(2),     // 중기
    LONG(3);    // 장기

    private final int bucket;

    PreferredPeriod(int bucket) {
        this.bucket = bucket;
    }

    public int bucket() {
        return bucket;
    }

    /** 가입기간(개월)을 구간 인덱스로 변환. */
    public static int bucketOf(int termMonth) {
        if (termMonth <= 1) {
            return 0;
        }
        if (termMonth <= 6) {
            return 1;
        }
        return termMonth <= 24 ? 2 : 3;
    }

    /** 개월 수를 대응하는 선호기간으로 변환(저축목표 기간 → 선호기간 자동 환산용). */
    public static PreferredPeriod fromMonths(int months) {
        return values()[bucketOf(months)];
    }
}
