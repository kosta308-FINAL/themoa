package com.weaone.themoa.domain.financialsearch.support;

import com.weaone.themoa.domain.financialsearch.entity.BankLink;
import com.weaone.themoa.domain.financialsearch.repository.BankLinkRepository;
import com.weaone.themoa.domain.financialsearch.service.BankUrlResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 은행 공식 홈페이지 링크 기본 시드.
 *
 * <p>은행 링크는 {@code financial_bank_link} 테이블에 저장되는데, 관리자가 수동 등록한 값은 그 환경의
 * DB에만 있어 다른 팀원 로컬이나 배포 환경에는 없다. 그래서 앱 기동 시 이 기본 목록으로 <b>빠진 은행만</b>
 * 채워, 어느 환경에서든 링크가 등록돼 있게 한다.
 *
 * <p>이미 등록된 은행명(관리자가 넣었거나 수정한 것)은 절대 덮지 않는다 — 사람 수정 > 기본값.
 */
@Component
@Order(20)
@RequiredArgsConstructor
public class BankLinkSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BankLinkSeeder.class);

    private final BankLinkRepository bankLinkRepository;
    private final BankUrlResolver bankUrlResolver;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Set<String> existing = bankLinkRepository.findAll().stream()
                .map(BankLink::getCompanyName)
                .collect(Collectors.toSet());

        List<BankLink> toAdd = new ArrayList<>();
        DEFAULT_LINKS.forEach((companyName, url) -> {
            if (!existing.contains(companyName)) {
                toAdd.add(BankLink.of(companyName, url));
            }
        });
        if (toAdd.isEmpty()) {
            return;
        }
        bankLinkRepository.saveAll(toAdd);
        // BankUrlResolver는 @PostConstruct에서 캐시를 미리 로드하므로, 방금 넣은 링크가 이번 기동에도
        // 바로 반영되도록 캐시를 갱신한다(이게 없으면 다음 재시작 전까지 검색 링크로 대체됨).
        bankUrlResolver.refresh();
        log.info("은행 링크 기본 시드 {}건 추가", toAdd.size());
    }

    /** 은행명(공시 kor_co_nm 기준) → 공식 홈페이지 URL. 빠진 은행이 확인되면 여기에 추가. */
    private static final Map<String, String> DEFAULT_LINKS = Map.ofEntries(
            Map.entry("BNK저축은행", "https://mw.bnksb.com/"),
            Map.entry("CK저축은행", "https://www.cksavingsbank.co.kr/main_new.act"),
            Map.entry("DH저축은행", "https://www.dhsavingsbank.co.kr/main_new.act"),
            Map.entry("HB저축은행", "https://www.hbsb.co.kr/"),
            Map.entry("IBK기업은행", "https://www.ibk.co.kr/"),
            Map.entry("IBK저축은행", "https://www.ibksb.co.kr/"),
            Map.entry("JT저축은행", "https://jt.ibs.fsb.or.kr/main_new.act"),
            Map.entry("JT친애저축은행", "https://www.jtchinae-bank.co.kr/"),
            Map.entry("KB저축은행", "https://kbstar.careerlink.kr/"),
            Map.entry("MS저축은행", "https://www.mssb.co.kr/main_new.act"),
            Map.entry("NH저축은행", "https://www.nhsavingsbank.co.kr/"),
            Map.entry("OK저축은행", "https://www.oksavingsbank.com/#/main"),
            Map.entry("OSB저축은행", "https://osb.co.kr/ib20/mnu/HOM00001"),
            Map.entry("SBI저축은행", "https://www.sbisb.co.kr/mai0020100.act"),
            Map.entry("경남은행", "https://www.knbank.co.kr/ib20/mnu/BHP000000000001"),
            Map.entry("고려저축은행", "https://www.goryosb.co.kr/home"),
            Map.entry("광주은행", "https://www.kjbank.com"),
            Map.entry("국민은행", "https://www.kbstar.com"),
            Map.entry("국제저축은행", "https://kukje.ibs.fsb.or.kr"),
            Map.entry("금화저축은행", "https://www.kuemhwabank.co.kr"),
            Map.entry("남양저축은행", "https://www.nybank.co.kr/main_new.act"),
            Map.entry("농협은행주식회사", "https://www.nhbank.com/nhmn/KO_NHMN_01.do"),
            Map.entry("다올저축은행", "https://www.daolsb.com/main.do"),
            Map.entry("대명상호저축은행", "https://www.daemyungbank.co.kr/main_new.act"),
            Map.entry("대백저축은행", "https://www.debecbank.co.kr/main_new.act"),
            Map.entry("대신저축은행", "https://bank.daishin.com/"),
            Map.entry("대아상호저축은행", "https://daeabank.com"),
            Map.entry("대원저축은행", "https://d-banks.co.kr/main_new.act"),
            Map.entry("대한저축은행", "https://www.daehanbank.co.kr/main_new.act"),
            Map.entry("더블저축은행", "https://www.doublebank.co.kr"),
            Map.entry("더케이저축은행", "https://www.thekbank.co.kr/main/main.do"),
            Map.entry("동양저축은행", "https://www.dysbank.com"),
            Map.entry("동원제일저축은행", "https://www.dysbank.com"),
            Map.entry("드림저축은행", "https://www.dreamsb.com/main_new.act"),
            Map.entry("디비저축은행", "https://www.idbsb.com/websquare/websquare.jsp?w2xPath=/w2/itb/main.xml"),
            Map.entry("라온저축은행", "https://www.raonsb.co.kr/main_new.act"),
            Map.entry("머스트삼일저축은행", "https://www.samilbank.co.kr/main_new.act"),
            Map.entry("모아저축은행", "https://www.moasb.co.kr/main_new.act"),
            Map.entry("민국저축은행", "https://www.mkb.co.kr/main_new.act"),
            Map.entry("바로저축은행", "https://www.barosavings.com/main_new.act"),
            Map.entry("부림저축은행", "https://www.bulimbank.co.kr/main_new.act"),
            Map.entry("부산은행", "https://www.busanbank.co.kr"),
            Map.entry("삼정저축은행", "https://www.samjungsavingsbank.co.kr"),
            Map.entry("삼호저축은행", "https://www.samhosb.co.kr"),
            Map.entry("상상인저축은행", "https://www.sangsanginsb.com"),
            Map.entry("상상인플러스저축은행", "https://www.sangsanginplussb.com"),
            Map.entry("세람저축은행", "https://seram.ibs.fsb.or.kr/main_new.act"),
            Map.entry("센트럴저축은행", "https://www.centralbank.co.kr"),
            Map.entry("솔브레인저축은행", "https://www.soulbrainsb.co.kr"),
            Map.entry("수협은행", "https://www.suhyup-bank.com/"),
            Map.entry("스마트저축은행", "https://www.smartbank.co.kr/main_new.act"),
            Map.entry("스카이저축은행", "https://www.skysb.co.kr/main_new.act"),
            Map.entry("스타저축은행", "https://www.estarbank.co.kr/main_new.act"),
            Map.entry("신한은행", "https://www.shinhan.com/index.jsp"),
            Map.entry("신한저축은행", "https://www.shinhansavings.com/CM_0066"),
            Map.entry("아산저축은행", "https://www.asanbank.co.kr/main_new.act"),
            Map.entry("아이엠뱅크", "https://www.imbank.co.kr/dgb_ebz_main.jsp"),
            Map.entry("안국저축은행", "https://www.angukbank.co.kr/main_new.act"),
            Map.entry("안양저축은행", "https://www.anyangbank.co.kr/main_new.act"),
            Map.entry("애큐온저축은행", "https://www.acuonsb.co.kr/"),
            Map.entry("에스앤티저축은행", "https://hisntm.ibs.fsb.or.kr/main_new.act"),
            Map.entry("엔에이치저축은행", "https://www.nhsavingsbank.co.kr/"),
            Map.entry("영진저축은행", "https://www.yjbank.co.kr/"),
            Map.entry("예가람저축은행", "https://www.yegaramsb.co.kr/main.frm?FRST_LOAD_YN=Y"),
            Map.entry("오성저축은행", "https://www.osungbank.co.kr/main_new.act"),
            Map.entry("오투저축은행", "https://www.o2banking.com/main_new.act"),
            Map.entry("우리금융저축은행", "https://www.woorisavingsbank.com/"),
            Map.entry("우리은행", "https://www.wooribank.com/"),
            Map.entry("우리저축은행", "https://www.wooleebank.co.kr/main_new.act"),
            Map.entry("웰컴저축은행", "https://www.welcomebank.co.kr/ib20/mnu/IBN000000000"),
            Map.entry("유니온저축은행", "https://www.kusb.co.kr/contents/main/"),
            Map.entry("유안타저축은행", "https://www.yuantasavings.co.kr/"),
            Map.entry("융창저축은행", "https://www.ycbank.co.kr/main_new.act"),
            Map.entry("인성저축은행", "https://www.insungsavingsbank.co.kr/main_new.act"),
            Map.entry("인천저축은행", "https://www.incheonbank.com/main_new.act"),
            Map.entry("전북은행", "https://www.jbbank.co.kr/"),
            Map.entry("제주은행", "https://www.jejubank.co.kr/hmpg/main.do"),
            Map.entry("조은저축은행", "https://www.choeunbank.com/main_new.act"),
            Map.entry("조흥저축은행", "https://chbank.ibs.fsb.or.kr/"),
            Map.entry("주식회사 카카오뱅크", "https://www.kakaobank.com/"),
            Map.entry("주식회사 케이뱅크", "https://www.kbanknow.com/web/web-home/home/main"),
            Map.entry("주식회사 하나은행", "https://www.kebhana.com/"),
            Map.entry("중소기업은행", "https://www.ibk.co.kr/"),
            Map.entry("진주저축은행", "https://www.jinjusb.co.kr/"),
            Map.entry("참저축은행", "https://www.charmbank.co.kr/"),
            Map.entry("청주저축은행", "https://www.cheongjubank.com/main_new.act"),
            Map.entry("키움예스저축은행", "https://www.kiwoomyesbank.com/main_new.act"),
            Map.entry("키움저축은행", "https://www.kiwoombank.com/"),
            Map.entry("토스뱅크 주식회사", "https://www.tossbank.com/"),
            Map.entry("페퍼저축은행", "https://www.pepperbank.kr/index.pepper"),
            Map.entry("평택저축은행", "https://www.ptbank.co.kr/main_new.act"),
            Map.entry("푸른저축은행", "https://www.prsb.co.kr/prsb/prwww.html?w2xPath=/w2/itb/main.xml&menu=3"),
            Map.entry("하나저축은행", "https://www.hanasavings.com/"),
            Map.entry("한국산업은행", "https://www.kdb.co.kr/index.jsp"),
            Map.entry("한국스탠다드차타드은행", "https://www.standardchartered.co.kr/np/kr/Intro.jsp"),
            Map.entry("한국씨티은행", "https://www.citibank.co.kr/ComMainCnts0100.act?ref=https://www.google.com/"),
            Map.entry("한국투자저축은행", "https://sb.koreainvestment.com/"),
            Map.entry("한성저축은행", "https://hs.ibs.fsb.or.kr/main_new.act"),
            Map.entry("한화저축은행", "https://www.hanwhasbank.com/main_new.act"),
            Map.entry("흥국저축은행", "https://ehkbank.ibs.fsb.or.kr/"));
}
