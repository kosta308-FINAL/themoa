package com.weaone.themoa.domain.financialsearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yaml의 app.financial.rag.* 값을 바인딩. FINANCIAL_RAG_ENABLED 환경변수로 on/off.
// 1단계(LIKE 검색만) 이식에서는 enabled=false로 두어 Qdrant 벡터검색을 타지 않는다.
@ConfigurationProperties(prefix = "app.financial.rag")
public class FinancialRagProperties {
    private boolean enabled;
    // 금융상품 전용 Qdrant 컬렉션 이름. 정책 RAG 컬렉션(youthcenter_policies)과 반드시 분리한다.
    private String collectionName = "financial_products";
    // 1차 검색(임계값 적용) 결과 개수 상한. 최종 결과/정렬 후보군도 이 크기로 다시 잘림.
    private int topK = 20;
    // 1차 검색이 0건일 때, 임계값을 절반으로 낮춰 재시도할 때 쓰는 개수 상한.
    private int retryTopK = 40;
    // 코사인 유사도 최소값(0~1). 너무 높이면 결과가 안 나오고, 너무 낮추면 관련없는 상품이 섞임.
    private double minimumSimilarity = 0.45;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public int getRetryTopK() { return retryTopK; }
    public void setRetryTopK(int retryTopK) { this.retryTopK = retryTopK; }
    public double getMinimumSimilarity() { return minimumSimilarity; }
    public void setMinimumSimilarity(double minimumSimilarity) { this.minimumSimilarity = minimumSimilarity; }
}
