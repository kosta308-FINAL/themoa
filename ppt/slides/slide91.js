/* ===================== Slide 91 (고정지출 · 화면 주석 설명) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s91{ background:transparent; }   /* 배경은 #deck에 고정된 공통 그라데이션을 그대로 사용 */
#s91 .htitle{ left:70px; top:50px; width:900px; color:#171717; font-size:40px; font-weight:800; }
#s91 .hsub{ left:70px; top:112px; width:900px; color:#333; font-size:20px; font-weight:600; line-height:1.5; }

/* 스크린샷 카드 */
#s91 .shotcard{
  left:70px; top:230px; width:1150px; height:820px; border-radius:18px;
  overflow:hidden; background:#fff; border:1px solid #E7ECE8;
  box-shadow:0 24px 55px rgba(0,0,0,.14);
}
#s91 .shotcard .img{
  width:100%; height:100%;
  background-repeat:no-repeat; background-size:100% auto; background-position:top;
}

/* 스크린샷 위 강조 박스 */
#s91 .hl{
  position:absolute; border:3px solid #007613; border-radius:12px;
  box-shadow:0 0 0 4px rgba(0,118,19,.12); pointer-events:none;
}

/* 스크린샷 위 번호 배지 */
#s91 .badge{
  position:absolute; width:40px; height:40px; border-radius:50%;
  background:#007613; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:17px; font-weight:800; box-shadow:0 8px 18px rgba(0,118,19,.35); z-index:4;
}

/* 연결선 */
#s91 .wires{ z-index:1; pointer-events:none; }

/* 우측 설명 리스트 */
#s91 .citem{
  position:absolute; left:1300px; width:560px; display:flex; align-items:flex-start; gap:18px;
}
#s91 .cnum{
  width:44px; height:44px; border-radius:50%; flex:none;
  background:#007613; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800; box-shadow:0 8px 18px rgba(0,118,19,.3);
}
#s91 .ctitle{ color:#171717; font-size:21px; font-weight:800; margin-bottom:6px; }
#s91 .cdesc{ color:#666; font-size:15px; font-weight:500; line-height:1.5; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s91">
  <div class="abs htitle">매달 반복되는 지출을 한곳에서</div>
  <div class="abs hsub">카드내역에서 반복 결제를 자동으로 찾아내고<br>결제일이 가까운 순서로 정리해 보여줍니다.</div>

  <div class="abs shotcard">
    <div class="img" style="background-image:url('assets/s90_fixed_expense.png')"></div>
  </div>

  <div class="abs hl" style="left:167px; top:367px; width:967px; height:165px;"></div>
  <div class="abs hl" style="left:167px; top:562px; width:661px; height:135px;"></div>
  <div class="abs hl" style="left:846px; top:562px; width:288px; height:193px;"></div>
  <div class="abs hl" style="left:167px; top:722px; width:661px; height:255px;"></div>
  <div class="abs hl" style="left:846px; top:845px; width:288px; height:201px;"></div>

  <div class="abs badge" style="left:151px; top:351px;">01</div>
  <div class="abs badge" style="left:151px; top:546px;">02</div>
  <div class="abs badge" style="left:818px; top:530px;">03</div>
  <div class="abs badge" style="left:151px; top:706px;">04</div>
  <div class="abs badge" style="left:818px; top:813px;">05</div>

  <!-- 선은 번호 배지가 아니라, 화면과 가장 가까운 카드 가장자리(오른쪽 중앙)에서 바로 출발 -->
  <svg class="abs wires" viewBox="0 0 1920 1080" width="1920" height="1080" style="left:0;top:0">
    <path d="M1134,450 Q1230,390 1300,322" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    <path d="M828,630  Q1080,540 1300,452" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    <path d="M1134,659 Q1230,620 1300,582" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    <path d="M828,850  Q1080,780 1300,712" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    <path d="M1134,946 Q1230,894 1300,842" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
  </svg>

  <div class="citem" style="top:300px;">
    <div class="cnum">01</div>
    <div>
      <div class="ctitle">고정지출 자동 합산</div>
      <div class="cdesc">등록된 항목을 합산해 급여 대비<br>비중까지 계산합니다.</div>
    </div>
  </div>

  <div class="citem" style="top:430px;">
    <div class="cnum">02</div>
    <div>
      <div class="ctitle">반복 결제 자동 감지</div>
      <div class="cdesc">카드내역에서 매달 반복되는 결제를<br>찾아 등록을 제안합니다.</div>
    </div>
  </div>

  <div class="citem" style="top:560px;">
    <div class="cnum">03</div>
    <div>
      <div class="ctitle">다가오는 결제 미리 확인</div>
      <div class="cdesc">다음 결제 예정일을 리스트로<br>미리 보여줍니다.</div>
    </div>
  </div>

  <div class="citem" style="top:690px;">
    <div class="cnum">04</div>
    <div>
      <div class="ctitle">결제일 가까운 순 정렬</div>
      <div class="cdesc">카드 결제·계좌이체를 구분해<br>결제 상태를 표시합니다.</div>
    </div>
  </div>

  <div class="citem" style="top:820px;">
    <div class="cnum">05</div>
    <div>
      <div class="ctitle">연간으로 환산해 보기</div>
      <div class="cdesc">한 달 금액이 1년으로 보면<br>얼마인지 바로 알려줍니다.</div>
    </div>
  </div>
</section>
`);
