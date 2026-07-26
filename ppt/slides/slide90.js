/* ===================== Slide 90 (주요 기능 · 소비가이드 / 고정지출) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s90{ background:transparent; }   /* 배경은 #deck에 고정된 공통 그라데이션을 그대로 사용 */
#s90 .ptitle{ left:52px; top:40px; width:900px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s90 .psub{ left:52px; top:112px; width:1100px; color:#111; font-size:26px; font-weight:500; }

#s90 .feature{ position:absolute; width:860px; top:210px; }
#s90 .feature.f1{ left:70px; }
#s90 .feature.f2{ left:990px; }

#s90 .feature .tag{
  display:inline-flex; align-items:center; height:40px; padding:0 18px;
  border-radius:999px; background:#E8F5EE; color:#007613;
  font-size:18px; font-weight:700; margin-bottom:14px;
}
#s90 .feature .ftitle{ color:#111; font-size:34px; font-weight:800; margin-bottom:10px; }
#s90 .feature .fdesc{ color:#333; font-size:20px; font-weight:400; line-height:1.5; margin-bottom:20px; }

/* 브라우저 목업 프레임 */
#s90 .browser{
  width:100%; height:560px; border-radius:16px; overflow:hidden;
  background:#fff; border:1px solid #E3E8E4;
  box-shadow:0 24px 55px rgba(0,0,0,.16);
}
#s90 .browser .chrome{
  height:38px; display:flex; align-items:center; gap:8px; padding:0 16px;
  background:#F1F3F1; border-bottom:1px solid #E3E8E4;
}
#s90 .browser .dot{ width:10px; height:10px; border-radius:50%; }
#s90 .browser .dot.r{ background:#FF5F57; }
#s90 .browser .dot.y{ background:#FEBC2E; }
#s90 .browser .dot.g{ background:#28C840; }
#s90 .browser .shot{ width:100%; height:calc(100% - 38px); overflow:hidden; }
/* 세로로 긴 스크린샷을 프레임 안에서 위→아래로 천천히 오토스크롤, 끝까지 내려가면 멈춤 */
#s90 .browser .shot .shotimg{
  width:100%; height:100%;
  background-repeat:no-repeat;
  background-size:100% auto;         /* 가로는 프레임에 맞추고 세로는 비율 유지 */
  background-position:center top;
  animation:s90scroll 18s ease-in-out 1 forwards;
}
@keyframes s90scroll{
  0%, 10%  { background-position:center top; }
  100%     { background-position:center bottom; }
}
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s90">
  <div class="abs ptitle">주요 기능</div>
  <div class="abs psub">오늘 얼마까지 써도 되는지, 매달 나가는 고정지출은 뭐가 있는지 — 한눈에 확인합니다</div>

  <div class="feature f1">
    <span class="tag">소비 가이드</span>
    <div class="ftitle">오늘의 소비 기준</div>
    <div class="fdesc">급여 주기와 남은 예산을 기준으로 오늘 써도 되는 금액을 계산해서 보여줘요.<br>카테고리별 소비 비중, 최근 7일 흐름까지 한 화면에서 확인할 수 있어요.</div>
    <div class="browser">
      <div class="chrome"><span class="dot r"></span><span class="dot y"></span><span class="dot g"></span></div>
      <div class="shot"><div class="shotimg" role="img" aria-label="소비가이드 화면" style="background-image:url('assets/s90_spending_guide.png')"></div></div>
    </div>
  </div>

  <div class="feature f2">
    <span class="tag">고정지출</span>
    <div class="ftitle">매달 반복되는 지출 관리</div>
    <div class="fdesc">카드내역에서 매달 반복되는 결제를 자동으로 찾아내고, 결제일이 가까운 순으로 정리해줘요.<br>급여 대비 고정지출 비중도 바로 확인할 수 있어요.</div>
    <div class="browser">
      <div class="chrome"><span class="dot r"></span><span class="dot y"></span><span class="dot g"></span></div>
      <div class="shot"><div class="shotimg" role="img" aria-label="고정지출 화면" style="background-image:url('assets/s90_fixed_expense.png')"></div></div>
    </div>
  </div>
</section>
`);
