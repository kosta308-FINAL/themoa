/* ===================== 예·적금-고정지출 자동연동 (실제 화면 흐름) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-savlink{ background:transparent; }
#s-savlink .ptitle{ position:absolute; left:52px; top:40px; width:1500px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s-savlink .psub{ position:absolute; left:52px; top:112px; width:1780px; color:#111; font-size:22px; font-weight:500; line-height:1.5; }

#s-savlink .steps{ position:absolute; left:70px; top:200px; width:1780px; display:flex; align-items:flex-start; justify-content:center; gap:14px; }

#s-savlink .stepcard{ background:#fff; border-radius:20px; box-shadow:0 20px 46px rgba(0,0,0,.14);
  padding:24px 28px 26px; position:relative; overflow:hidden; text-align:center; flex:none; }
#s-savlink .stepcard .bar{ position:absolute; left:0; top:0; width:100%; height:8px; background:#2D8A4E; }
#s-savlink .stepcard.c1 .bar{ background:#2F6FED; }

#s-savlink .stepcard .head{ display:flex; align-items:center; justify-content:center; gap:10px; margin-bottom:14px; }
#s-savlink .stepcard .num{ width:32px; height:32px; border-radius:50%; flex:none; background:#2D8A4E; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:14px; font-weight:800; }
#s-savlink .stepcard.c1 .num{ background:#2F6FED; }
#s-savlink .stepcard .stitle{ color:#171717; font-size:22px; font-weight:800; }

#s-savlink .stepcard .shotwrap{ display:flex; align-items:center; justify-content:center; margin-bottom:16px; }
#s-savlink .stepcard .shotwrap img{ display:block; border-radius:10px; box-shadow:0 8px 20px rgba(0,0,0,.10); }

#s-savlink .stepcard .sdesc{ color:#555; font-size:16px; font-weight:500; line-height:1.5; }

#s-savlink .tag{ display:inline-block; background:#F3F5F7; color:#555; font-size:13px; font-weight:700;
  padding:6px 12px; border-radius:999px; margin-top:12px; }
#s-savlink .tag.warn{ background:#FFF3EC; color:#B5541F; }

#s-savlink .arrowcol{ flex:none; display:flex; flex-direction:column; align-items:center; justify-content:flex-start;
  padding-top:210px; }
#s-savlink .arrowcol .arrow{ font-size:32px; font-weight:800; color:#A9BDB0; }
#s-savlink .arrowcol .evname{ margin-top:8px; color:#2D8A4E; font-size:13px; font-weight:800; text-align:center; width:150px; }

#s-savlink .note{ position:absolute; left:70px; top:958px; width:1780px; background:#fff; border-radius:14px;
  box-shadow:0 10px 24px rgba(0,0,0,.08); padding:18px 28px; color:#444; font-size:17px; font-weight:600;
  display:flex; align-items:center; gap:12px; }
#s-savlink .note .dot{ flex:none; width:10px; height:10px; border-radius:50%; background:#2D8A4E; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-savlink">
  <div class="ptitle">예·적금-고정지출 자동연동</div>
  <div class="psub">예·적금에 가입하면 그 납입액이 고정지출로 자동 반영됩니다.<br>두 도메인은 서로 직접 참조하지 않고, 이벤트로만 연결합니다.</div>

  <div class="steps">
    <div class="stepcard c1" style="width:400px;">
      <div class="bar"></div>
      <div class="head"><div class="num">01</div><div class="stitle">예·적금 가입</div></div>
      <div class="shotwrap"><img src="assets/savlink_subscribe.png" style="height:460px;"></div>
      <div class="sdesc">월 납입액 500,000원으로<br>SB톡톡 정기적금에 가입합니다.</div>
      <div class="tag warn">fixedExpense는 모름 · 이벤트만 발행</div>
    </div>

    <div class="arrowcol" style="width:150px;">
      <div class="arrow">&#8594;</div>
      <div class="evname">SavingsSubscription<br>CreatedEvent</div>
    </div>

    <div class="stepcard" style="width:480px;">
      <div class="bar"></div>
      <div class="head"><div class="num">02</div><div class="stitle">고정지출 목록</div></div>
      <div class="shotwrap"><img src="assets/savlink_list.png" style="width:420px;"></div>
      <div class="sdesc">저축 · 계좌이체 · 매월 27일로<br>자동 등록됩니다.</div>
    </div>

    <div class="arrowcol" style="width:150px;">
      <div class="arrow">&#8594;</div>
      <div class="evname">탭하면<br>상세로</div>
    </div>

    <div class="stepcard" style="width:400px;">
      <div class="bar"></div>
      <div class="head"><div class="num">03</div><div class="stitle">고정지출 상세</div></div>
      <div class="shotwrap"><img src="assets/savlink_detail.png" style="height:460px;"></div>
      <div class="sdesc">카테고리 저축 · 결제일 27일로<br>확인할 수 있습니다.</div>
    </div>
  </div>

  <div class="note"><div class="dot"></div>만기 자동감지·자동해지는 지원하지 않습니다. 가입 기록을 삭제하면 연동된 고정지출도 함께 취소됩니다.</div>
</section>
`);
