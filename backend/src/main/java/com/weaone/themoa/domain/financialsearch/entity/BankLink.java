package com.weaone.themoa.domain.financialsearch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// 은행/저축은행 공식 홈페이지 링크. company_name(savings_product/loan_product의 company_name과 동일 문자열)을
// 그대로 키로 쓴다. 확인된 링크만 여기에 채워 넣는다 — 검증 안 된 URL을 임의로 추가하면 안 됨.
@Entity
@Table(name = "financial_bank_link")
public class BankLink {

    @Id
    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(name = "official_url", nullable = false, length = 500)
    private String officialUrl;

    protected BankLink() {
    }

    private BankLink(String companyName, String officialUrl) {
        this.companyName = companyName;
        this.officialUrl = officialUrl;
    }

    public static BankLink of(String companyName, String officialUrl) {
        return new BankLink(companyName, officialUrl);
    }

    /** 확인된 공식 URL이 바뀌었을 때 갱신한다(회사명은 식별자라 바꾸지 않는다). */
    public void changeOfficialUrl(String officialUrl) {
        this.officialUrl = officialUrl;
    }

    public String getCompanyName() { return companyName; }
    public String getOfficialUrl() { return officialUrl; }
}
