package com.weaone.themoa.domain.financialsearch.service;

import com.weaone.themoa.domain.recommend.entity.LoanProduct;
import com.weaone.themoa.domain.recommend.entity.LoanProductOption;
import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

// DB 상품 엔티티를 Qdrant에 저장할 "문서(임베딩 대상 텍스트 + 메타데이터)"로 변환한다.
// 이 텍스트가 실제로 임베딩되어 벡터가 되므로, 검색 품질은 여기 어떤 필드를 넣느냐에 크게 좌우된다.
// (상품 엔티티는 recommend 도메인 것을 재사용한다. productType은 enum이라 name()으로 문자열화한다.)
@Component
public class FinancialDocumentBuilder {

    // 필드를 그냥 이어붙이지 않고 "라벨: 값" 형태로 구조화한다.
    // 임베딩 모델이 "가입대상"과 "우대조건"을 같은 문단으로 뭉뚱그리지 않고 구분해서 인식하도록 하기 위함
    // (예: "청년" 우대조건과 "육아/다자녀" 우대조건이 한 문장에 같이 나와서 서로 비슷하다고 착각하는 문제 완화).
    public Document build(SavingsProduct product) {
        String text = """
                상품유형: %s
                금융회사: %s
                상품명: %s
                가입대상: %s
                우대조건: %s""".formatted(
                enumName(product.getProductType()),
                nullToEmpty(product.getCompanyName()),
                nullToEmpty(product.getProductName()),
                nullToEmpty(product.getJoinTarget()),
                nullToEmpty(product.getSpecialCondition()));
        return Document.builder()
                .id(deterministicId("savings-" + product.getId()))
                .text(text)
                .metadata("financialProductId", product.getId())
                .metadata("productKind", "SAVINGS")
                .build();
    }

    // 대출은 DB에 가입대상/우대조건 텍스트가 사실상 비어있다(finlife API가 대출엔 그 정보를 안 줌).
    // 대신 옵션(담보유형/상환방식/신용등급구간)과 note/한도 텍스트를 채워 넣어야 검색할 거리가 생긴다.
    public Document build(LoanProduct product) {
        String text = """
                상품유형: %s
                금융회사: %s
                상품명: %s
                담보/상환 유형: %s
                신용등급구간: %s
                한도: %s
                우대조건: %s
                유의사항: %s""".formatted(
                enumName(product.getProductType()),
                nullToEmpty(product.getCompanyName()),
                nullToEmpty(product.getProductName()),
                optionTypeText(product),
                creditGradeText(product),
                nullToEmpty(product.getLoanLimit()),
                nullToEmpty(product.getSpecialCondition()),
                nullToEmpty(product.getNote()));
        return Document.builder()
                .id(deterministicId("loan-" + product.getId()))
                .text(text)
                .metadata("financialProductId", product.getId())
                .metadata("productKind", "LOAN")
                .build();
    }

    private String optionTypeText(LoanProduct product) {
        Set<String> types = new LinkedHashSet<>();
        for (LoanProductOption option : product.getOptions()) {
            if (StringUtils.hasText(option.getMortgageTypeName())) types.add(option.getMortgageTypeName());
            if (StringUtils.hasText(option.getRepayTypeName())) types.add(option.getRepayTypeName());
            if (StringUtils.hasText(option.getRateTypeName())) types.add(option.getRateTypeName());
        }
        return String.join(", ", types);
    }

    private String creditGradeText(LoanProduct product) {
        Set<String> grades = new LinkedHashSet<>();
        for (LoanProductOption option : product.getOptions()) {
            if (StringUtils.hasText(option.getCreditGradeSection())) grades.add(option.getCreditGradeSection());
        }
        return String.join(", ", grades);
    }

    // Qdrant는 문서 ID로 UUID(또는 정수)만 허용한다("savings-1" 같은 임의 문자열은 에러).
    // 같은 입력이면 항상 같은 UUID가 나오므로, 임베딩 배치를 다시 돌려도 중복 생성되지 않고 upsert된다.
    private String deterministicId(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private String nullToEmpty(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
