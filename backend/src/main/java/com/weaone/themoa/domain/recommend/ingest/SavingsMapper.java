package com.weaone.themoa.domain.recommend.ingest;

import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsProductOption;
import com.weaone.themoa.domain.recommend.entity.SavingsType;
import com.weaone.themoa.domain.recommend.dto.SavingsBaseItem;
import com.weaone.themoa.domain.recommend.dto.SavingsOptionItem;

/**
 * finlife 예·적금 DTO → 엔티티 변환 담당(정적 유틸).
 * 저장 로직(Upsert)은 SavingsIngestService가 맡고, 여기선 "필드 옮겨담기"만 한다.
 */
public final class SavingsMapper {

    private SavingsMapper() {
    }

    /** 상품 기본정보 DTO → SavingsProduct 엔티티(옵션 미포함). */
    public static SavingsProduct toProduct(SavingsBaseItem b, SavingsType type) {
        return SavingsProduct.builder()
                .productType(type)
                .companyCode(b.finCoNo())
                .productCode(b.finPrdtCd())
                .companyName(b.korCoNm())
                .productName(b.finPrdtNm())
                .joinMethod(b.joinWay())
                .joinRestrict(b.joinDeny())
                .joinTarget(b.joinMember())
                .specialCondition(b.spclCnd())
                .maturityInterest(b.mtrtInt())
                .note(b.etcNote())
                .maxAmount(toIntOrNull(b.maxLimit()))
                .openDate(b.dclsStrtDay())
                .closeDate(b.dclsEndDay())
                .build();
    }

    /** 금리 옵션 DTO → SavingsProductOption 엔티티. */
    public static SavingsProductOption toOption(SavingsOptionItem o) {
        return SavingsProductOption.builder()
                .rateTypeCode(o.intrRateType())
                .rateTypeName(o.intrRateTypeNm())
                .termMonth(parseTermMonth(o.saveTrm()))
                .baseRate(o.intrRate())
                .maxRate(o.intrRate2())
                .reserveTypeCode(o.rsrvType())
                .reserveTypeName(o.rsrvTypeNm())
                .build();
    }

    /** max_limit(원, Long) → Integer. int 범위를 넘거나 null이면 null(=한도없음 취급). */
    public static Integer toIntOrNull(Long v) {
        if (v == null || v > Integer.MAX_VALUE || v < Integer.MIN_VALUE) {
            return null;
        }
        return v.intValue();
    }

    /** save_trm("24" 같은 문자열) → 개월 수 Integer. 비거나 숫자 아니면 null. */
    public static Integer parseTermMonth(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
