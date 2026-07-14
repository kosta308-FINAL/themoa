package com.weaone.themoa.domain.cardconnection.support;

import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.CodefValueType;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 지원 카드사 화이트리스트 10개 시드(connection.md §2-1). 광주(0316)·수협(0320)·제주(0321)는 인증서 로그인만
 * 지원해 제외, 씨티(0307)는 ID 로그인 시 SMS 2-way 추가인증을 요구해 제외한다(무인 새벽 배치 불가).
 * fx_type·cancel_type·cancel_amount_uncertain 값은 cardtransaction.md §3-5, §4(FX-00)에서 이미 확정된 값이다.
 */
@Component
@RequiredArgsConstructor
public class CardIssuerSeeder implements ApplicationRunner {

    private final CardIssuerRepository cardIssuerRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (cardIssuerRepository.count() > 0) {
            return;
        }
        cardIssuerRepository.saveAll(List.of(
                CardIssuer.seed("0301", "KB국민카드", CodefValueType.TYPE1, CodefValueType.TYPE2, false),
                CardIssuer.seed("0302", "현대카드", CodefValueType.TYPE2, CodefValueType.TYPE1, false),
                CardIssuer.seed("0303", "삼성카드", CodefValueType.TYPE1, CodefValueType.TYPE2, true),
                CardIssuer.seed("0304", "NH농협카드", CodefValueType.TYPE1, CodefValueType.TYPE1, false),
                CardIssuer.seed("0305", "BC카드", CodefValueType.TYPE1, CodefValueType.TYPE1, false),
                CardIssuer.seed("0306", "신한카드", CodefValueType.TYPE1, CodefValueType.TYPE1, true),
                CardIssuer.seed("0309", "우리카드", CodefValueType.TYPE2, CodefValueType.TYPE1, false),
                CardIssuer.seed("0311", "롯데카드", CodefValueType.TYPE2, CodefValueType.TYPE1, true),
                CardIssuer.seed("0313", "하나카드", CodefValueType.TYPE2, CodefValueType.TYPE1, false),
                CardIssuer.seed("0315", "전북카드", CodefValueType.TYPE2, CodefValueType.TYPE1, false)
        ));
    }
}
