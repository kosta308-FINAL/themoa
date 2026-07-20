package com.weaone.themoa.domain.recommend.service;

/** 위험성향 (financial_profile.risk_type). */
public enum RiskType {
    STABLE,      // 안정형 → 단리/예금 선호
    NEUTRAL,     // 중립형
    AGGRESSIVE   // 공격형 → 복리/펀드형 선호
}
