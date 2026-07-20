package com.weaone.themoa.domain.recommend.ingest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.weaone.themoa.domain.recommend.entity.LoanProduct;
import com.weaone.themoa.domain.recommend.entity.LoanProductOption;
import com.weaone.themoa.domain.recommend.entity.LoanType;
import com.weaone.themoa.domain.recommend.dto.CreditLoanItem;
import com.weaone.themoa.domain.recommend.dto.CreditLoanOptionItem;
import com.weaone.themoa.domain.recommend.dto.LoanBaseItem;
import com.weaone.themoa.domain.recommend.dto.LoanRateOptionItem;

/**
 * finlife 대출 DTO → 엔티티 변환(정적 유틸).
 * 주담대/전세는 옵션 1건 그대로, 신용대출은 옵션 1건을 신용점수 구간별 여러 옵션으로 펼친다.
 */
public final class LoanMapper {

    private LoanMapper() {
    }

    /** 주담대/전세 상품 기본정보 → LoanProduct(옵션 미포함). loan_inci_expn(부대비용)은 note에 담는다. */
    public static LoanProduct toMortgageRentProduct(LoanBaseItem b, LoanType type) {
        return LoanProduct.builder()
                .productType(type)
                .companyCode(b.finCoNo())
                .productCode(b.finPrdtCd())
                .companyName(b.korCoNm())
                .productName(b.finPrdtNm())
                .joinMethod(b.joinWay())
                .loanLimit(b.loanLmt())
                .earlyRepayFee(b.erlyRpayFee())
                .delayRate(b.dlyRate())
                .note(b.loanInciExpn())
                .openDate(b.dclsStrtDay())
                .closeDate(b.dclsEndDay())
                .build();
    }

    /** 신용대출 상품 기본정보 → LoanProduct(옵션 미포함). CB사·상품유형은 note에 요약해 담는다. */
    public static LoanProduct toCreditProduct(CreditLoanItem b) {
        String note = "신용대출유형: " + nullToDash(b.crdtPrdtTypeNm())
                + (b.cbName() != null ? " / CB: " + b.cbName() : "");
        return LoanProduct.builder()
                .productType(LoanType.CREDIT)
                .companyCode(b.finCoNo())
                .productCode(b.finPrdtCd())
                .companyName(b.korCoNm())
                .productName(b.finPrdtNm())
                .joinMethod(b.joinWay())
                .note(note)
                .openDate(b.dclsStrtDay())
                .closeDate(b.dclsEndDay())
                .build();
    }

    /** 주담대/전세 금리 옵션 1건 → LoanProductOption 1건. */
    public static LoanProductOption toRateOption(LoanRateOptionItem o) {
        return LoanProductOption.builder()
                .mortgageTypeCode(o.mrtgType())
                .mortgageTypeName(o.mrtgTypeNm())
                .repayTypeCode(o.rpayType())
                .repayTypeName(o.rpayTypeNm())
                .rateTypeCode(o.lendRateType())
                .rateTypeName(o.lendRateTypeNm())
                .rateMin(o.lendRateMin())
                .rateMax(o.lendRateMax())
                .rateAvg(o.lendRateAvg())
                .build();
    }

    /** 신용대출 옵션 1건 → 신용점수 구간별 LoanProductOption 여러 건(값 있는 구간만). */
    public static List<LoanProductOption> toCreditOptions(CreditLoanOptionItem o) {
        List<LoanProductOption> list = new ArrayList<>();
        addGrade(list, o, "900점 초과", o.crdtGrad1());
        addGrade(list, o, "801~900점", o.crdtGrad4());
        addGrade(list, o, "701~800점", o.crdtGrad5());
        addGrade(list, o, "601~700점", o.crdtGrad6());
        addGrade(list, o, "501~600점", o.crdtGrad10());
        addGrade(list, o, "401~500점", o.crdtGrad11());
        addGrade(list, o, "301~400점", o.crdtGrad12());
        addGrade(list, o, "300점 이하", o.crdtGrad13());
        addGrade(list, o, "평균", o.crdtGradAvg());
        return list;
    }

    private static void addGrade(List<LoanProductOption> list, CreditLoanOptionItem o,
                                 String section, BigDecimal rate) {
        if (rate == null) {
            return;
        }
        list.add(LoanProductOption.builder()
                .rateTypeCode(o.crdtLendRateType())
                .rateTypeName(o.crdtLendRateTypeNm())
                .rateAvg(rate)                 // 해당 구간의 금리
                .creditGradeSection(section)   // 신용점수 구간 라벨
                .build());
    }

    private static String nullToDash(String s) {
        return s == null ? "-" : s;
    }
}
