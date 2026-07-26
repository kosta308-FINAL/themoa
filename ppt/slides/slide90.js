/* ===================== Slide 90 (내 기능 소개 · 소비가이드 → 고정지출 → 전체 소비내역, 클릭으로 전환) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s90{ background:transparent; }   /* 배경은 #deck에 고정된 공통 그라데이션을 그대로 사용 */

/* 4단계(step0~3)를 한 슬라이드 안에서 클릭으로 전환. 현재 data-step 값과 일치하는 것만 보임 */
#s90 .step{ position:absolute; inset:0; opacity:0; pointer-events:none; transition:opacity .5s ease; }
#s90[data-step="0"] .step0,
#s90[data-step="1"] .step1,
#s90[data-step="2"] .step2,
#s90[data-step="3"] .step3{ opacity:1; pointer-events:auto; }

#s90 .htitle{ left:70px; top:50px; width:1000px; color:#171717; font-size:40px; font-weight:800; }
#s90 .hsub{ left:70px; top:112px; width:1050px; color:#333; font-size:20px; font-weight:600; line-height:1.5; }

/* 스크린샷 카드 */
#s90 .shotcard{
  left:70px; top:230px; width:1150px; border-radius:18px;
  overflow:hidden; background:#fff; border:1px solid #E7ECE8;
  box-shadow:0 24px 55px rgba(0,0,0,.14);
}
#s90 .shotcard .img{
  width:100%; height:100%;
  background-repeat:no-repeat; background-size:100% auto;
}

/* 스크린샷 위 강조 박스 */
#s90 .hl{
  position:absolute; border:3px solid #007613; border-radius:12px;
  box-shadow:0 0 0 4px rgba(0,118,19,.12); pointer-events:none;
}

/* 스크린샷 위 번호 배지 */
#s90 .badge{
  position:absolute; width:40px; height:40px; border-radius:50%;
  background:#007613; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:17px; font-weight:800; box-shadow:0 8px 18px rgba(0,118,19,.35); z-index:4;
}

/* 연결선 */
#s90 .wires{ position:absolute; inset:0; z-index:1; pointer-events:none; }

/* 우측 설명 리스트 */
#s90 .citem{
  position:absolute; left:1300px; width:560px; display:flex; align-items:flex-start; gap:18px;
}
#s90 .cnum{
  width:44px; height:44px; border-radius:50%; flex:none;
  background:#007613; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800; box-shadow:0 8px 18px rgba(0,118,19,.3);
}
#s90 .ctitle{ color:#171717; font-size:21px; font-weight:800; margin-bottom:6px; }
#s90 .cdesc{ color:#666; font-size:15px; font-weight:500; line-height:1.5; }

/* 클릭 유도 힌트 (마지막 단계에서는 숨김) */
#s90 .hint{
  position:absolute; left:70px; top:970px; color:#6B7C8A; font-size:16px; font-weight:700;
  display:flex; align-items:center; gap:6px; transition:opacity .3s ease; z-index:5;
}
#s90[data-step="3"] .hint{ opacity:0; pointer-events:none; }
#s90 .hint .arrow{ display:inline-block; animation:s90bounce 1.2s ease-in-out infinite; }
@keyframes s90bounce{ 0%,100%{ transform:translateX(0); } 50%{ transform:translateX(6px); } }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s90">
  <!-- 이 슬라이드의 최대 스텝 수(3)를 엔진에 알려주는 표시용 마커 -->
  <div class="frag" data-step="3" style="display:none"></div>

  <div class="abs hint">클릭하면 다음 화면으로 <span class="arrow">→</span></div>

  <!-- ========================= STEP 0 : 소비가이드 (상단) ========================= -->
  <div class="step step0">
    <div class="abs htitle">오늘의 소비 기준을 한눈에</div>
    <div class="abs hsub">회원의 급여 주기와 남은 예산을 기준으로<br>오늘 써도 되는 금액을 자동으로 계산합니다.</div>

    <div class="abs shotcard" style="height:720px;">
      <div class="img" style="background-image:url('assets/s90_spending_guide.png'); background-position:center top;"></div>
    </div>

    <div class="abs hl" style="left:167px; top:429px; width:683px; height:203px;"></div>
    <div class="abs hl" style="left:857px; top:429px; width:277px; height:203px;"></div>
    <div class="abs hl" style="left:167px; top:665px; width:557px; height:118px;"></div>
    <div class="abs hl" style="left:738px; top:665px; width:395px; height:259px;"></div>

    <div class="abs badge" style="left:151px; top:413px;">01</div>
    <div class="abs badge" style="left:841px; top:413px;">02</div>
    <div class="abs badge" style="left:151px; top:649px;">03</div>
    <div class="abs badge" style="left:722px; top:649px;">04</div>

    <svg class="abs wires" viewBox="0 0 1920 1080" width="1920" height="1080">
      <path d="M850,531  Q1150,430 1300,352" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
      <path d="M1134,531 Q1230,500 1300,487" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
      <path d="M724,724  Q1080,660 1300,622" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
      <path d="M1133,795 Q1230,780 1300,757" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    </svg>

    <div class="citem" style="top:330px;">
      <div class="cnum">01</div>
      <div>
        <div class="ctitle">오늘 사용 가능 금액</div>
        <div class="cdesc">하루 권장 소비액과 순사용액을 반영해<br>자동으로 계산합니다.</div>
      </div>
    </div>
    <div class="citem" style="top:465px;">
      <div class="cnum">02</div>
      <div>
        <div class="ctitle">이번 급여 주기 예산</div>
        <div class="cdesc">급여일 기준 주기 동안 남은 예산을<br>실시간으로 보여줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:600px;">
      <div class="cnum">03</div>
      <div>
        <div class="ctitle">잉여금 누적</div>
        <div class="cdesc">권장액보다 덜 쓴 금액을 급여 주기마다<br>자동으로 쌓아줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:735px;">
      <div class="cnum">04</div>
      <div>
        <div class="ctitle">카테고리별 소비</div>
        <div class="cdesc">실제 소비 내역을 카테고리로 나눠<br>시각화해서 보여줍니다.</div>
      </div>
    </div>
  </div>

  <!-- ========================= STEP 1 : 소비가이드 (하단) ========================= -->
  <div class="step step1">
    <div class="abs htitle">오늘 거래와 소비 흐름까지</div>
    <div class="abs hsub">카드내역을 자동으로 모아 정리하고,<br>최근 소비 흐름과 절약 포인트까지 알려줍니다.</div>

    <div class="abs shotcard" style="height:720px;">
      <div class="img" style="background-image:url('assets/s90_spending_guide.png'); background-position:center bottom;"></div>
    </div>

    <div class="abs hl" style="left:167px; top:230px; width:557px; height:324px;"></div>
    <div class="abs hl" style="left:738px; top:475px; width:395px; height:216px;"></div>
    <div class="abs hl" style="left:167px; top:568px; width:557px; height:345px;"></div>

    <div class="abs badge" style="left:151px; top:214px;">01</div>
    <div class="abs badge" style="left:722px; top:459px;">02</div>
    <div class="abs badge" style="left:151px; top:552px;">03</div>

    <svg class="abs wires" viewBox="0 0 1920 1080" width="1920" height="1080">
      <path d="M724,392  Q1080,380 1300,372" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
      <path d="M1133,583 Q1230,562 1300,542" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
      <path d="M724,740  Q1080,726 1300,712" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    </svg>

    <div class="citem" style="top:350px;">
      <div class="cnum">01</div>
      <div>
        <div class="ctitle">오늘 거래 자동 수집</div>
        <div class="cdesc">카드내역을 자동으로 동기화하고,<br>직접 입력도 바로 할 수 있습니다.</div>
      </div>
    </div>
    <div class="citem" style="top:520px;">
      <div class="cnum">02</div>
      <div>
        <div class="ctitle">이번 달 이렇게 아껴봐요</div>
        <div class="cdesc">지난 소비 습관을 바탕으로<br>절약할 수 있는 부분을 알려줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:690px;">
      <div class="cnum">03</div>
      <div>
        <div class="ctitle">최근 7일 소비 흐름</div>
        <div class="cdesc">날짜별 순사용액과 하루 권장액을<br>그래프로 비교해서 보여줍니다.</div>
      </div>
    </div>
  </div>

  <!-- ========================= STEP 2 : 고정지출 ========================= -->
  <div class="step step2">
    <div class="abs htitle">매달 반복되는 지출을 한곳에서</div>
    <div class="abs hsub">카드내역에서 반복 결제를 자동으로 찾아내고<br>결제일이 가까운 순서로 정리해 보여줍니다.</div>

    <div class="abs shotcard" style="height:820px;">
      <div class="img" style="background-image:url('assets/s90_fixed_expense.png'); background-position:center top;"></div>
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

    <svg class="abs wires" viewBox="0 0 1920 1080" width="1920" height="1080">
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
  </div>

  <!-- ========================= STEP 3 : 전체 소비내역 (transactions) ========================= -->
  <div class="step step3">
    <div class="abs htitle">소비 흐름을 그래프와 순위로</div>
    <div class="abs hsub">급여 주기 전체의 결제 흐름과 자주 이용한 곳을<br>한 번에 확인할 수 있습니다.</div>

    <div class="abs shotcard" style="height:850px;">
      <div class="img" style="background-image:url('assets/s90_transactions.png'); background-position:center top;"></div>
    </div>

    <div class="abs hl" style="left:771px; top:449px; width:291px; height:341px;"></div>
    <div class="abs hl" style="left:233px; top:580px; width:520px; height:211px;"></div>
    <div class="abs hl" style="left:233px; top:816px; width:563px; height:264px;"></div>

    <div class="abs badge" style="left:739px; top:417px;">01</div>
    <div class="abs badge" style="left:217px; top:564px;">02</div>
    <div class="abs badge" style="left:217px; top:800px;">03</div>

    <svg class="abs wires" viewBox="0 0 1920 1080" width="1920" height="1080">
      <path d="M1062,620 Q1180,600 1300,372" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
      <path d="M753,686  Q1080,600 1300,542" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
      <path d="M796,948  Q1080,830 1300,712" fill="none" stroke="#007613" stroke-width="2.5" opacity=".55"/>
    </svg>

    <div class="citem" style="top:350px;">
      <div class="cnum">01</div>
      <div>
        <div class="ctitle">많이 쓴 곳 TOP 5</div>
        <div class="cdesc">이번 주기에 가장 많이 쓴 가맹점을<br>순위로 보여줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:520px;">
      <div class="cnum">02</div>
      <div>
        <div class="ctitle">일별 소비 추이</div>
        <div class="cdesc">급여 주기 전체의 날짜별 소비 흐름을<br>그래프로 확인할 수 있습니다.</div>
      </div>
    </div>
    <div class="citem" style="top:690px;">
      <div class="cnum">03</div>
      <div>
        <div class="ctitle">결제내역 전체 목록</div>
        <div class="cdesc">날짜별로 묶어서 최신순으로,<br>페이지를 넘겨가며 확인합니다.</div>
      </div>
    </div>
  </div>
</section>
`);
