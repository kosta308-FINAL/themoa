package com.weaone.themoa.domain.recommend.controller;

import java.util.Map;

/**
 * 금융회사명 → 공식 홈페이지 URL 매핑.
 * ※ 링크를 지어내지 않기 위해, 실제로 검색으로 확인된 은행만 넣는다.
 *   키는 DB의 company_name과 정확히 일치해야 한다(예: "주식회사 하나은행", "농협은행주식회사").
 *   일부 소형 저축은행은 자체 홈페이지 없이 저축은행중앙회 인터넷뱅킹(*.ibs.fsb.or.kr)만 운영한다.
 *   매핑에 없는 은행은 "은행명 공식 홈페이지" 검색으로 fallback.
 */
public final class BankHomepage {

    private BankHomepage() {
    }

    private static final Map<String, String> HOMEPAGES = Map.ofEntries(
            // ── 시중/지방/인터넷/특수 은행 ──
            Map.entry("국민은행", "https://www.kbstar.com/"),
            Map.entry("신한은행", "https://www.shinhan.com/"),
            Map.entry("우리은행", "https://www.wooribank.com/"),
            Map.entry("주식회사 하나은행", "https://www.kebhana.com/"),
            Map.entry("농협은행주식회사", "https://www.nhbank.com/"),
            Map.entry("중소기업은행", "https://www.ibk.co.kr/"),
            Map.entry("한국산업은행", "https://www.kdb.co.kr/"),
            Map.entry("부산은행", "https://www.busanbank.co.kr/"),
            Map.entry("광주은행", "https://www.kjbank.com/"),
            Map.entry("전북은행", "https://www.jbbank.co.kr/"),
            Map.entry("제주은행", "https://www.jejubank.co.kr/"),
            Map.entry("아이엠뱅크", "https://www.imbank.co.kr/"),
            Map.entry("주식회사 카카오뱅크", "https://www.kakaobank.com/"),
            Map.entry("주식회사 케이뱅크", "https://www.kbanknow.com/"),
            // ── 저축은행 (검색으로 공식 도메인 확인) ──
            Map.entry("SBI저축은행", "https://www.sbisb.co.kr/"),
            Map.entry("OK저축은행", "https://www.oksavingsbank.com/"),
            Map.entry("웰컴저축은행", "https://www.welcomebank.co.kr/"),
            Map.entry("페퍼저축은행", "https://www.pepperbank.kr/"),
            Map.entry("애큐온저축은행", "https://www.acuonsb.co.kr/"),
            Map.entry("다올저축은행", "https://www.daolsb.com/"),
            Map.entry("한국투자저축은행", "https://sb.koreainvestment.com/"),
            Map.entry("상상인저축은행", "https://www.sangsanginsb.com/"),
            Map.entry("상상인플러스저축은행", "https://www.sangsanginplussb.com/"),
            Map.entry("키움저축은행", "https://www.kiwoombank.com/"),
            Map.entry("키움예스저축은행", "https://www.kiwoomyesbank.com/"),
            Map.entry("하나저축은행", "https://www.hanasavings.com/"),
            Map.entry("KB저축은행", "https://www.kbsavings.com/"),
            Map.entry("신한저축은행", "https://www.shinhansavings.com/"),
            Map.entry("우리금융저축은행", "https://www.woorisavingsbank.com/"),
            Map.entry("우리저축은행", "https://www.wooleebank.co.kr/"),
            Map.entry("IBK저축은행", "https://www.ibksb.co.kr/"),
            Map.entry("BNK저축은행", "https://www.bnksb.com/"),
            Map.entry("디비저축은행", "https://www.idbsb.com/"),
            Map.entry("유안타저축은행", "https://www.yuantasavings.co.kr/"),
            Map.entry("한화저축은행", "https://www.hanwhasbank.com/"),
            Map.entry("흥국저축은행", "https://www.hkbanking.co.kr/"),
            Map.entry("푸른저축은행", "https://www.pureunbank.co.kr/"),
            Map.entry("대신저축은행", "https://www.daishinbank.com/"),
            Map.entry("스마트저축은행", "https://www.smartbank.co.kr/"),
            Map.entry("예가람저축은행", "https://www.yegaramsb.co.kr/"),
            Map.entry("머스트삼일저축은행", "https://www.samilbank.co.kr/"),
            Map.entry("삼호저축은행", "https://www.samhosb.co.kr/"),
            Map.entry("HB저축은행", "https://www.hbsb.co.kr/"),
            Map.entry("모아저축은행", "https://www.moasb.co.kr/"),
            Map.entry("CK저축은행", "https://www.cksavingsbank.co.kr/"),
            Map.entry("JT저축은행", "https://www.jt-bank.co.kr/"),
            Map.entry("JT친애저축은행", "https://www.jtchinae-bank.co.kr/"),
            Map.entry("OSB저축은행", "https://www.osb.co.kr/"),
            Map.entry("고려저축은행", "https://www.goryosb.co.kr/"),
            Map.entry("참저축은행", "https://www.charmbank.co.kr/"),
            Map.entry("스카이저축은행", "https://www.skysb.co.kr/"),
            Map.entry("안국저축은행", "https://www.angukbank.co.kr/"),
            Map.entry("DH저축은행", "https://www.dhsavingsbank.co.kr/"),
            Map.entry("MS저축은행", "https://www.mssb.co.kr/"),
            Map.entry("남양저축은행", "https://www.nybank.co.kr/"),
            Map.entry("대백저축은행", "https://www.debecbank.co.kr/"),
            Map.entry("드림저축은행", "https://www.dreamsb.com/"),
            Map.entry("라온저축은행", "https://www.raonsb.co.kr/"),
            Map.entry("민국저축은행", "https://www.mkb.co.kr/"),
            Map.entry("바로저축은행", "https://www.barosavings.com/"),
            Map.entry("스타저축은행", "https://www.estarbank.co.kr/"),
            Map.entry("아산저축은행", "https://www.asanbank.co.kr/"),
            Map.entry("청주저축은행", "https://www.cheongjubank.com/"),
            Map.entry("대원저축은행", "https://www.d-banks.co.kr/"),
            Map.entry("조은저축은행", "https://www.choeunbank.com/"),
            Map.entry("조흥저축은행", "http://chbank.net/"),
            Map.entry("동양저축은행", "https://www.dysbank.com/"),
            Map.entry("인천저축은행", "https://www.incheonbank.com/"),
            Map.entry("솔브레인저축은행", "https://www.soulbrainsb.co.kr/"),
            Map.entry("센트럴저축은행", "https://www.centralbank.co.kr/"),
            Map.entry("에스앤티저축은행", "https://www.hisntm.com/"),
            Map.entry("더케이저축은행", "https://www.thekbank.co.kr/"),
            Map.entry("삼정저축은행", "https://www.samjungsavingsbank.co.kr/"),
            Map.entry("오투저축은행", "https://www.o2banking.com/"),
            Map.entry("금화저축은행", "https://www.kuemhwabank.co.kr/"),
            Map.entry("인성저축은행", "https://www.insungsavingsbank.co.kr/"),
            Map.entry("오성저축은행", "https://www.osungbank.co.kr/"),
            Map.entry("엔에이치저축은행", "https://www.nhsavingsbank.co.kr/"),
            Map.entry("대명상호저축은행", "https://www.daemyungbank.co.kr/"),
            Map.entry("대한저축은행", "https://www.daehanbank.co.kr/"),
            Map.entry("대아상호저축은행", "https://daeabank.com/"),
            Map.entry("안양저축은행", "https://www.anyangbank.co.kr/"),
            // ── 자체 홈페이지 없이 저축은행중앙회 인터넷뱅킹만 운영 ──
            Map.entry("세람저축은행", "https://seram.ibs.fsb.or.kr/"),
            Map.entry("국제저축은행", "https://kukje.ibs.fsb.or.kr/"),
            Map.entry("유니온저축은행", "https://unionsb.ibs.fsb.or.kr/"),
            Map.entry("영진저축은행", "https://yjbank.ibs.fsb.or.kr/"),
            Map.entry("한성저축은행", "https://hs.ibs.fsb.or.kr/")
    );

    /** 매핑된 공식 홈페이지 URL(없으면 null → 검색 fallback). */
    public static String urlFor(String companyName) {
        return companyName == null ? null : HOMEPAGES.get(companyName);
    }
}
