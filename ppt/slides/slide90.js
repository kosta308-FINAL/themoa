/* ===================== Slide 90 (소비 가이드 · 화면 주석 설명) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s90{ background:transparent; }   /* 배경은 #deck에 고정된 공통 그라데이션을 그대로 사용 */
#s90 .htitle{ left:70px; top:50px; width:900px; color:#171717; font-size:40px; font-weight:800; }
#s90 .hsub{ left:70px; top:112px; width:900px; color:#333; font-size:20px; font-weight:600; line-height:1.5; }

/* 스크린샷 카드 */
#s90 .shotcard{
  left:70px; top:230px; width:1150px; height:720px; border-radius:18px;
  overflow:hidden; background:#fff; border:1px solid #E7ECE8;
  box-shadow:0 24px 55px rgba(0,0,0,.14);
}
#s90 .shotcard .img{
  width:100%; height:100%;
  background-repeat:no-repeat; background-size:100% auto; background-position:center top;
  transition:background-position 1.1s ease;
}
#s90.morphed .shotcard .img{ background-position:center bottom; }

/* 스크린샷 위 강조 박스 */
#s90 .hl{
  position:absolute; border:3px solid #007613; border-radius:12px;
  box-shadow:0 0 0 4px rgba(0,118,19,.12); pointer-events:none;
  transition:opacity .4s ease;
}

/* 스크린샷 위 번호 배지 */
#s90 .badge{
  position:absolute; width:40px; height:40px; border-radius:50%;
  background:#007613; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:17px; font-weight:800; box-shadow:0 8px 18px rgba(0,118,19,.35); z-index:4;
  transition:opacity .4s ease;
}

/* 연결선 */
#s90 .wires{ z-index:1; pointer-events:none; }
#s90 .wires path{ transition:opacity .4s ease; }

/* 우측 설명 리스트 */
#s90 .citem{
  position:absolute; left:1300px; width:560px; display:flex; align-items:flex-start; gap:18px;
  transition:opacity .4s ease;
}
#s90 .cnum{
  width:44px; height:44px; border-radius:50%; flex:none;
  background:#007613; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800; box-shadow:0 8px 18px rgba(0,118,19,.3);
}
#s90 .ctitle{ color:#171717; font-size:21px; font-weight:800; margin-bottom:6px; }
#s90 .cdesc{ color:#666; font-size:15px; font-weight:500; line-height:1.5; }

/* 1단계(스크롤 전) ↔ 2단계(스크롤 후) 그룹 전환 */
#s90 .gB{ opacity:0; pointer-events:none; }
#s90.morphed .gA{ opacity:0; pointer-events:none; }
/* 스크롤(1.1s)이 끝나고 0.5s 더 기다렸다가 테두리/번호/설명이 나타남 */
#s90.morphed .gB{ opacity:1; pointer-events:auto; transition-delay:1.6s; }

/* 스크롤 유도 힌트 */
#s90 .scrollhint{
  left:70px; top:970px; color:#6B7C8A; font-size:16px; font-weight:700;
  display:flex; align-items:center; gap:6px; transition:opacity .3s ease;
}
#s90.morphed .scrollhint{ opacity:0; }
#s90 .scrollhint .arrow{ display:inline-block; animation:s90bounce 1.4s ease-in-out infinite; }
@keyframes s90bounce{ 0%,100%{ transform:translateY(0); } 50%{ transform:translateY(5px); } }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s90" data-morph>
  <div class="abs htitle">오늘의 소비 기준을 한눈에</div>
  <div class="abs hsub">회원의 급여 주기와 남은 예산을 기준으로<br>오늘 써도 되는 금액을 자동으로 계산합니다.</div>

  <div class="abs shotcard">
    <div class="img" style="background-image:url('assets/s90_spending_guide.png')"></div>
  </div>

  <div class="abs scrollhint">클릭하면 화면 아래쪽도 볼 수 있어요 <span class="arrow">↓</span></div>

  <!-- ===== 1단계: 상단 영역 (예산/잉여금/카테고리) ===== -->
  <div class="abs hl gA" style="left:167px; top:429px; width:683px; height:203px;"></div>
  <div class="abs hl gA" style="left:857px; top:429px; width:277px; height:203px;"></div>
  <div class="abs hl gA" style="left:167px; top:665px; width:557px; height:118px;"></div>
  <div class="abs hl gA" style="left:738px; top:665px; width:395px; height:259px;"></div>

  <div class="abs badge gA" style="left:151px; top:413px;">01</div>
  <div class="abs badge gA" style="left:841px; top:413px;">02</div>
  <div class="abs badge gA" style="left:151px; top:649px;">03</div>
  <div class="abs badge gA" style="left:722px; top:649px;">04</div>

  <div class="citem gA" style="top:330px;">
    <div class="cnum">01</div>
    <div>
      <div class="ctitle">오늘 사용 가능 금액</div>
      <div class="cdesc">하루 권장 소비액과 순사용액을 반영해<br>자동으로 계산합니다.</div>
    </div>
  </div>

  <div class="citem gA" style="top:465px;">
    <div class="cnum">02</div>
    <div>
      <div class="ctitle">이번 급여 주기 예산</div>
      <div class="cdesc">급여일 기준 주기 동안 남은 예산을<br>실시간으로 보여줍니다.</div>
    </div>
  </div>

  <div class="citem gA" style="top:600px;">
    <div class="cnum">03</div>
    <div>
      <div class="ctitle">잉여금 누적</div>
      <div class="cdesc">권장액보다 덜 쓴 금액을 급여 주기마다<br>자동으로 쌓아줍니다.</div>
    </div>
  </div>

  <div class="citem gA" style="top:735px;">
    <div class="cnum">04</div>
    <div>
      <div class="ctitle">카테고리별 소비</div>
      <div class="cdesc">실제 소비 내역을 카테고리로 나눠<br>시각화해서 보여줍니다.</div>
    </div>
  </div>

  <!-- ===== 2단계: 하단 영역 (거래 내역/아껴쓰기 제안/소비 흐름) ===== -->
  <div class="abs hl gB" style="left:167px; top:230px; width:557px; height:324px;"></div>
  <div class="abs hl gB" style="left:738px; top:475px; width:395px; height:216px;"></div>
  <div class="abs hl gB" style="left:167px; top:568px; width:557px; height:345px;"></div>

  <div class="abs badge gB" style="left:151px; top:214px;">05</div>
  <div class="abs badge gB" style="left:722px; top:459px;">06</div>
  <div class="abs badge gB" style="left:151px; top:552px;">07</div>

  <div class="citem gB" style="top:350px;">
    <div class="cnum">05</div>
    <div>
      <div class="ctitle">오늘 거래 자동 수집</div>
      <div class="cdesc">카드내역을 자동으로 동기화하고,<br>직접 입력도 바로 할 수 있습니다.</div>
    </div>
  </div>

  <div class="citem gB" style="top:520px;">
    <div class="cnum">06</div>
    <div>
      <div class="ctitle">이번 달 이렇게 아껴봐요</div>
      <div class="cdesc">지난 소비 습관을 바탕으로<br>절약할 수 있는 부분을 알려줍니다.</div>
    </div>
  </div>

  <div class="citem gB" style="top:690px;">
    <div class="cnum">07</div>
    <div>
      <div class="ctitle">최근 7일 소비 흐름</div>
      <div class="cdesc">날짜별 순사용액과 하루 권장액을<br>그래프로 비교해서 보여줍니다.</div>
    </div>
  </div>

  <!-- 선은 번호 배지가 아니라, 화면과 가장 가까운 카드 가장자리(오른쪽 중앙)에서 바로 출발 -->
  <svg class="abs wires" viewBox="0 0 1920 1080" width="1920" height="1080" style="left:0;top:0">
    <path class="gA" d="M850,531  Q1150,430 1300,352" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    <path class="gA" d="M1134,531 Q1230,500 1300,487" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    <path class="gA" d="M724,724  Q1080,660 1300,622" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    <path class="gA" d="M1133,795 Q1230,780 1300,757" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    <path class="gB" d="M724,392  Q1080,380 1300,372" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    <path class="gB" d="M1133,583 Q1230,562 1300,542" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    <path class="gB" d="M724,740  Q1080,726 1300,712" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
  </svg>
</section>
`);
