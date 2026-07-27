/* ===================== 트러블슈팅 (팀원 1인 1슬라이드) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
.s-trouble{ background:transparent; }
.s-trouble .ptitle{ position:absolute; left:52px; top:40px; width:1200px; color:#6B7C8A; font-size:45px; font-weight:800; }
.s-trouble .pidx{ position:absolute; right:70px; top:56px; color:#9AA5B1; font-size:20px; font-weight:800; }

.s-trouble .phead{ position:absolute; left:52px; top:132px; width:1780px; display:flex; align-items:center; gap:18px; }
.s-trouble .tavatar{ flex:none; width:64px; height:64px; border-radius:50%; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:22px; font-weight:800; }
.s-trouble .tname{ font-size:32px; font-weight:800; color:#171717; }

.s-trouble.p1 .tavatar{ background:#2F6FED; }
.s-trouble.p2 .tavatar{ background:#007613; }
.s-trouble.p3 .tavatar{ background:#7C3AED; }

.s-trouble .cases{ position:absolute; left:52px; top:264px; width:1816px; }
.s-trouble .tcase{ background:#fff; border-radius:20px; box-shadow:0 20px 46px rgba(0,0,0,.14);
  padding:30px 40px; margin-bottom:28px; border-left:8px solid; }
.s-trouble.p1 .tcase{ border-left-color:#2F6FED; }
.s-trouble.p2 .tcase{ border-left-color:#007613; }
.s-trouble.p3 .tcase{ border-left-color:#7C3AED; }

.s-trouble .ttop{ display:flex; align-items:center; gap:14px; margin-bottom:18px; }
.s-trouble .ttag{ flex:none; background:#F3F5F7; color:#555; font-size:14px; font-weight:700;
  padding:6px 14px; border-radius:999px; }
.s-trouble .ttitle{ font-size:23px; font-weight:800; color:#171717; }

.s-trouble .body{ display:flex; }
.s-trouble .half{ flex:1; padding-right:36px; font-size:17px; color:#444; font-weight:500; line-height:1.65; }
.s-trouble .half.sol{ padding-right:0; padding-left:36px; border-left:1px solid #EAEDF0; }
.s-trouble .lbl{ display:inline-block; font-size:14px; font-weight:800; padding:3px 10px;
  border-radius:6px; margin-right:9px; vertical-align:2px; }
.s-trouble .lbl.bad{ background:#FDECEA; color:#C0392B; }
.s-trouble .lbl.good{ background:#E8F5EE; color:#1E7A3D; }

.s-trouble .placeholder{ background:#fff; border-radius:20px; border:2px dashed #D8DEE5;
  padding:120px 40px; color:#9AA5B1; font-size:22px; font-weight:700; text-align:center; line-height:1.8; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide s-trouble p1" id="s-trouble-1">
  <div class="ptitle">트러블슈팅</div>
  <div class="pidx">1 / 3</div>
  <div class="phead"><div class="tavatar">솔민</div><div class="tname">김솔민</div></div>

  <div class="cases">
    <div class="tcase">
      <div class="ttop"><div class="ttag">탐지 정확도</div><div class="ttitle">같은 결제처, 다른 상품 오탐지</div></div>
      <div class="body">
        <div class="half prob"><span class="lbl bad">문제</span>같은 결제처(alias) 안에 가격이 다른 구독이 섞이면<br>평균 금액이 흔들려 반복결제 탐지가 통째로 실패했습니다.</div>
        <div class="half sol"><span class="lbl good">해결</span>금액을 먼저 10% 오차 버킷으로 나누고, 버킷별로<br>날짜 패턴을 검사해 서로 다른 구독을 각각 인식합니다.</div>
      </div>
    </div>

    <div class="tcase">
      <div class="ttop"><div class="ttag">트랜잭션</div><div class="ttitle">새벽 배치 LazyInitializationException</div></div>
      <div class="body">
        <div class="half prob"><span class="lbl bad">문제</span>조회·처리 트랜잭션이 분리돼 있어, LAZY 필드·프록시<br>접근 시점에 세션이 이미 닫혀 배치가 조용히 실패했습니다.</div>
        <div class="half sol"><span class="lbl good">해결</span>join fetch 추가와 배치 메서드에 @Transactional을<br>붙여 해결하고, 같은 패턴의 다른 배치도 점검 목록으로 남겼습니다.</div>
      </div>
    </div>

    <div class="tcase">
      <div class="ttop"><div class="ttag">성능</div><div class="ttitle">초기수집 폴링 N+1 쿼리</div></div>
      <div class="body">
        <div class="half prob"><span class="lbl bad">문제</span>2초 간격 폴링마다 거의 안 바뀌는 참조 데이터(card_issuer)를<br>매번 다시 조회하고 있었습니다.</div>
        <div class="half sol"><span class="lbl good">해결</span>Caffeine 캐시로 카드사명을 캐싱하고 폴링 간격을 5초로<br>늘려 쿼리 부하를 사실상 제거했습니다.</div>
      </div>
    </div>
  </div>
</section>

<section class="slide s-trouble p2" id="s-trouble-2">
  <div class="ptitle">트러블슈팅</div>
  <div class="pidx">2 / 3</div>
  <div class="phead"><div class="tavatar">호연</div><div class="tname">문호연</div></div>

  <div class="cases">
    <div class="placeholder">문호연님의 트러블슈팅 사례를 여기에 채워주세요.<br>(문제 → 원인 → 해결 형태로 2~3건)</div>
  </div>
</section>

<section class="slide s-trouble p3" id="s-trouble-3">
  <div class="ptitle">트러블슈팅</div>
  <div class="pidx">3 / 3</div>
  <div class="phead"><div class="tavatar">수지</div><div class="tname">임수지</div></div>

  <div class="cases">
    <div class="placeholder">임수지님의 트러블슈팅 사례를 여기에 채워주세요.<br>(문제 → 원인 → 해결 형태로 2~3건)</div>
  </div>
</section>
`);
