/* ===================== 트러블슈팅 (팀원별 문제 발견·해결, 클릭 애니메이션) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-trouble{ background:transparent; }
#s-trouble .ptitle{ position:absolute; left:52px; top:40px; width:1500px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s-trouble .psub{ position:absolute; left:52px; top:112px; width:1780px; color:#111; font-size:22px; font-weight:500; line-height:1.5; }

#s-trouble .cols{ position:absolute; left:70px; top:210px; width:1780px; display:flex; gap:28px; }
#s-trouble .tcol{ flex:1; }

#s-trouble .thead{ display:flex; align-items:center; gap:16px; margin-bottom:26px; }
#s-trouble .tavatar{ flex:none; width:56px; height:56px; border-radius:50%; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:19px; font-weight:800; }
#s-trouble .p1 .tavatar{ background:#2F6FED; }
#s-trouble .p2 .tavatar{ background:#007613; }
#s-trouble .p3 .tavatar{ background:#7C3AED; }
#s-trouble .tname{ font-size:25px; font-weight:800; color:#171717; }

#s-trouble .tcase{ background:#fff; border-radius:16px; box-shadow:0 14px 32px rgba(0,0,0,.12);
  padding:24px 26px; margin-bottom:22px; border-left:7px solid; }
#s-trouble .p1 .tcase{ border-left-color:#2F6FED; }
#s-trouble .p2 .tcase{ border-left-color:#007613; }
#s-trouble .p3 .tcase{ border-left-color:#7C3AED; }

#s-trouble .tcase .ttop{ display:flex; align-items:center; gap:10px; margin-bottom:14px; }
#s-trouble .tcase .ttag{ flex:none; background:#F3F5F7; color:#555; font-size:13px; font-weight:700;
  padding:5px 12px; border-radius:999px; }
#s-trouble .tcase .ttitle{ font-size:19px; font-weight:800; color:#171717; line-height:1.35; }
#s-trouble .tcase .trow{ font-size:15px; color:#444; font-weight:500; line-height:1.6; margin-bottom:10px; }
#s-trouble .tcase .trow:last-child{ margin-bottom:0; }
#s-trouble .tcase .lbl{ display:inline-block; font-size:13px; font-weight:800; padding:2px 9px;
  border-radius:6px; margin-right:8px; }
#s-trouble .tcase .lbl.bad{ background:#FDECEA; color:#C0392B; }
#s-trouble .tcase .lbl.good{ background:#E8F5EE; color:#1E7A3D; }

#s-trouble .placeholder{ background:#fff; border-radius:16px; border:2px dashed #D8DEE5;
  padding:40px 26px; color:#9AA5B1; font-size:16px; font-weight:700; text-align:center; line-height:1.7; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-trouble">
  <div class="ptitle">트러블슈팅</div>
  <div class="psub">개발 중 만난 문제를 어떻게 진단하고 해결했는지 팀원별로 정리했습니다. (클릭하면 한 명씩 공개됩니다)</div>

  <div class="cols">
    <div class="tcol p1">
      <div class="thead"><div class="tavatar">솔민</div><div class="tname">김솔민</div></div>

      <div class="tcase frag" data-step="1">
        <div class="ttop"><div class="ttag">탐지 정확도</div><div class="ttitle">같은 결제처, 다른 상품 오탐지</div></div>
        <div class="trow"><span class="lbl bad">문제</span>같은 결제처(alias) 안에 가격이 다른 구독이 섞이면<br>평균 금액이 흔들려 반복결제 탐지가 통째로 실패했습니다.</div>
        <div class="trow"><span class="lbl good">해결</span>금액을 먼저 10% 오차 버킷으로 나누고, 버킷별로<br>날짜 패턴을 검사해 서로 다른 구독을 각각 인식합니다.</div>
      </div>

      <div class="tcase frag" data-step="1">
        <div class="ttop"><div class="ttag">트랜잭션</div><div class="ttitle">새벽 배치 LazyInitializationException</div></div>
        <div class="trow"><span class="lbl bad">문제</span>조회·처리 트랜잭션이 분리돼 있어, LAZY 필드·프록시 접근 시점에<br>세션이 이미 닫혀 배치가 조용히 실패했습니다.</div>
        <div class="trow"><span class="lbl good">해결</span>join fetch 추가와 배치 메서드에 @Transactional을 붙여 해결하고,<br>같은 패턴의 다른 배치도 점검 목록으로 남겼습니다.</div>
      </div>

      <div class="tcase frag" data-step="1">
        <div class="ttop"><div class="ttag">성능</div><div class="ttitle">초기수집 폴링 N+1 쿼리</div></div>
        <div class="trow"><span class="lbl bad">문제</span>2초 간격 폴링마다 거의 안 바뀌는 참조 데이터(card_issuer)를<br>매번 다시 조회하고 있었습니다.</div>
        <div class="trow"><span class="lbl good">해결</span>Caffeine 캐시로 카드사명을 캐싱하고 폴링 간격을 5초로 늘려<br>쿼리 부하를 사실상 제거했습니다.</div>
      </div>
    </div>

    <div class="tcol p2">
      <div class="thead"><div class="tavatar">호연</div><div class="tname">문호연</div></div>

      <div class="placeholder frag" data-step="2">문호연님의 트러블슈팅 사례를<br>여기에 채워주세요.</div>
    </div>

    <div class="tcol p3">
      <div class="thead"><div class="tavatar">수지</div><div class="tname">임수지</div></div>

      <div class="placeholder frag" data-step="3">임수지님의 트러블슈팅 사례를<br>여기에 채워주세요.</div>
    </div>
  </div>
</section>
`);
