package com.weaone.themoa.domain.recommend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 연금저축 수령 옵션 (pension_product_option).
 * finlife 연금저축 API의 optionList 1건에 대응한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pension_product_option")
public class PensionProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private PensionProduct product;

    @Column(name = "receive_period_code")
    private String receivePeriodCode;   // finlife: pnsn_recp_trm (연금수령기간코드)

    @Column(name = "receive_period_name")
    private String receivePeriodName;   // finlife: pnsn_recp_trm_nm

    @Column(name = "entry_age")
    private Integer entryAge;            // finlife: pnsn_entr_age (가입연령)

    @Column(name = "monthly_payment", length = 5)
    private String monthlyPaymentCode;  // finlife: mon_paym_atm (월납입금액코드)

    @Column(name = "payment_period")
    private Integer paymentPeriod;      // finlife: paym_prd (납입기간, 년)

    @Column(name = "pension_start_age")
    private Integer pensionStartAge;    // finlife: pnsn_strt_age (연금개시나이)

    @Column(name = "pension_receive_amount")
    private Long pensionReceiveAmount;  // finlife: pnsn_recp_amt (연금수령액)

    @Builder
    private PensionProductOption(String receivePeriodCode, String receivePeriodName, Integer entryAge,
                                 String monthlyPaymentCode, Integer paymentPeriod, Integer pensionStartAge,
                                 Long pensionReceiveAmount) {
        this.receivePeriodCode = receivePeriodCode;
        this.receivePeriodName = receivePeriodName;
        this.entryAge = entryAge;
        this.monthlyPaymentCode = monthlyPaymentCode;
        this.paymentPeriod = paymentPeriod;
        this.pensionStartAge = pensionStartAge;
        this.pensionReceiveAmount = pensionReceiveAmount;
    }

    /** 연관관계 설정 - PensionProduct.addOption() 내부에서만 호출(패키지 전용). */
    void setProduct(PensionProduct product) {
        this.product = product;
    }
}
