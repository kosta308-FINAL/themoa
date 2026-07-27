/* ===================== 예·적금-고정지출 자동연동 (실제 화면 흐름) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-savlink{ background:transparent; color:#0F1B2A; }
#s-savlink::before{ content:""; position:absolute; right:-140px; top:-70px; width:890px; height:300px; border-radius:0 0 0 90%; background:linear-gradient(180deg,rgba(16,185,129,.12),rgba(16,185,129,.02)); transform:rotate(-7deg); }
#s-savlink::after{ content:""; position:absolute; right:-220px; bottom:170px; width:760px; height:250px; border-radius:50%; background:linear-gradient(90deg,rgba(16,185,129,.17),rgba(255,255,255,0)); transform:rotate(-18deg); }
#s-savlink .ptitle{ position:absolute; left:58px; top:50px; width:1300px; color:#0E1A2A; font-size:52px; font-weight:850; line-height:1.08; letter-spacing:0; }
#s-savlink .ptitle .accent{ color:#007A3D; }
#s-savlink .psub{ position:absolute; left:60px; top:126px; width:1380px; color:#152235; font-size:25px; font-weight:620; line-height:1.48; letter-spacing:0; }
#s-savlink .piggy{ position:absolute; right:206px; top:44px; width:245px; height:130px; opacity:.56; z-index:1; }
#s-savlink .piggy svg{ width:100%; height:100%; display:block; }
#s-savlink .flow{ position:absolute; left:70px; top:216px; width:1780px; display:grid; grid-template-columns:405px 170px 470px 170px 405px; column-gap:40px; align-items:start; z-index:3; }
#s-savlink .stepcard{ position:relative; height:558px; border-radius:24px; background:rgba(255,255,255,.95); border:1px solid rgba(203,215,209,.88); box-shadow:0 18px 40px rgba(26,50,39,.12); overflow:hidden; padding:29px 30px 24px; text-align:center; transform:translateY(18px); }
#s-savlink .stepcard.show{ transform:translateY(0); }
#s-savlink .stepcard .bar{ position:absolute; left:0; top:0; width:100%; height:8px; background:#059A50; }
#s-savlink .stepcard.c1 .bar{ background:#2F73EA; }
#s-savlink .stepcard .head{ height:40px; display:flex; align-items:center; justify-content:center; gap:13px; margin-bottom:17px; }
#s-savlink .stepcard .num{ width:38px; height:38px; border-radius:50%; background:#059A50; color:#fff; display:flex; align-items:center; justify-content:center; font-size:17px; font-weight:850; flex:none; }
#s-savlink .stepcard.c1 .num{ background:#2F73EA; }
#s-savlink .stepcard .stitle{ color:#111D2D; font-size:24px; font-weight:850; line-height:1.2; letter-spacing:0; }
#s-savlink .shotwrap{ height:342px; display:flex; align-items:center; justify-content:center; margin-bottom:18px; background:linear-gradient(180deg,#FFFFFF 0%,#F8FCFA 100%); border-radius:18px; border:1px solid #EEF3F0; overflow:hidden; }
#s-savlink .shotwrap img{ display:block; max-width:100%; max-height:100%; object-fit:contain; border-radius:12px; box-shadow:0 10px 22px rgba(18,39,27,.12); }
#s-savlink .shotwrap .shot-subscribe{ height:326px; width:auto; }
#s-savlink .shotwrap .shot-list{ width:420px; max-width:none; height:auto; }
#s-savlink .shotwrap .shot-detail{ height:326px; width:auto; }
#s-savlink .sdesc{ color:#1B2635; font-size:20px; font-weight:680; line-height:1.45; letter-spacing:0; }
#s-savlink .sdesc .green{ color:#007A3D; font-weight:850; }
#s-savlink .tag{ display:inline-flex; align-items:center; justify-content:center; min-height:33px; margin-top:11px; padding:6px 14px; border-radius:999px; background:#FFF2E8; color:#BB4D14; font-size:16px; font-weight:800; line-height:1.2; }
#s-savlink .callout{ display:inline-flex; align-items:center; justify-content:center; gap:12px; margin-top:10px; padding:13px 22px; border-radius:16px; background:#EAF8F0; color:#103423; font-size:20px; font-weight:760; line-height:1.45; }
#s-savlink .callout svg{ width:30px; height:30px; color:#129963; flex:none; }
#s-savlink .connector{ height:558px; display:flex; flex-direction:column; align-items:center; justify-content:center; transform:translateY(18px); }
#s-savlink .connector.show{ transform:translateY(0); }
#s-savlink .connector svg{ display:block; }
#s-savlink .connector .arrow-svg{ width:132px; height:36px; color:#23945A; margin-bottom:20px; }
#s-savlink .connector .badge{ width:72px; height:72px; border-radius:50%; display:flex; align-items:center; justify-content:center; background:#FFFFFF; color:#0B8B55; border:1px solid #DFEEE6; box-shadow:0 13px 26px rgba(27,83,55,.12); margin-bottom:14px; }
#s-savlink .connector .badge svg{ width:42px; height:42px; }
#s-savlink .connector .evname{ color:#007A3D; font-size:17px; font-weight:850; text-align:center; line-height:1.32; letter-spacing:0; }
#s-savlink .connector .move-text{ color:#007A3D; font-size:20px; font-weight:850; text-align:center; line-height:1.25; letter-spacing:0; }
#s-savlink .benefits{ position:absolute; left:56px; top:814px; width:1808px; height:158px; border-radius:24px; background:rgba(255,255,255,.94); border:1px solid rgba(211,224,216,.9); box-shadow:0 16px 34px rgba(24,54,39,.11); display:grid; grid-template-columns:repeat(4,1fr); overflow:hidden; z-index:3; transform:translateY(18px); }
#s-savlink .benefits.show{ transform:translateY(0); }
#s-savlink .benefit{ display:flex; align-items:center; gap:20px; padding:23px 28px; position:relative; }
#s-savlink .benefit + .benefit{ border-left:1px solid #DDE8E1; }
#s-savlink .benefit .bicon{ width:70px; height:70px; border-radius:50%; background:#EAF7F0; color:#087540; display:flex; align-items:center; justify-content:center; flex:none; box-shadow:inset 0 0 0 1px rgba(8,117,64,.08); }
#s-savlink .benefit .bicon svg{ width:42px; height:42px; display:block; }
#s-savlink .benefit .btitle{ color:#006B39; font-size:22px; font-weight:850; line-height:1.2; margin-bottom:7px; letter-spacing:0; }
#s-savlink .benefit .bdesc{ color:#263241; font-size:17px; font-weight:560; line-height:1.38; letter-spacing:0; }
#s-savlink .frag{ transform:translateY(18px); }
#s-savlink .frag.show{ transform:translateY(0); }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-savlink">
  <div class="ptitle"><span class="accent">예·적금</span>-고정지출 자동연동</div>
  <div class="psub">예·적금에 가입하면 납입액이 고정지출로 자동 반영됩니다.<br>두 도메인은 서로 직접 참조하지 않고, 이벤트로만 연결합니다.</div>

  <div class="piggy">
    <svg viewBox="0 0 260 140" aria-hidden="true">
      <ellipse cx="130" cy="114" rx="86" ry="12" fill="#0D7F49" opacity=".12"></ellipse>
      <path d="M60 68c8-27 36-44 77-40 30 3 55 18 65 40h18c7 0 12 5 12 12v20c0 7-5 12-12 12h-14c-7 13-20 23-37 29v-17H92v17c-18-7-30-18-36-33H38c-10 0-17-8-17-17s7-17 17-17h15c2-2 4-4 7-6z" fill="#17A86B" opacity=".45"></path>
      <circle cx="194" cy="72" r="5" fill="#087540" opacity=".55"></circle>
      <path d="M111 28h45" stroke="#087540" stroke-width="8" stroke-linecap="round" opacity=".36"></path>
      <circle cx="184" cy="22" r="20" fill="none" stroke="#18A96C" stroke-width="5" opacity=".45"></circle>
      <text x="174" y="30" fill="#087540" font-size="22" font-weight="850" opacity=".55">₩</text>
    </svg>
  </div>

  <div class="flow">
    <div class="stepcard c1 frag" data-step="1">
      <div class="bar"></div>
      <div class="head"><div class="num">01</div><div class="stitle">예·적금 가입</div></div>
      <div class="shotwrap"><img class="shot-subscribe" src="assets/savlink_subscribe.png" alt="예·적금 가입 화면"></div>
      <div class="sdesc">월 납입액 500,000원으로<br>SB톡톡 정기적금에 가입합니다.</div>
      <div class="tag">fixedExpense는 모름 · 이벤트만 발행</div>
    </div>

    <div class="connector frag" data-step="2">
      <svg class="arrow-svg" viewBox="0 0 132 36" aria-hidden="true">
        <path d="M5 18h101" fill="none" stroke="currentColor" stroke-width="6" stroke-linecap="round"></path>
        <path d="M94 6l28 12-28 12" fill="none" stroke="currentColor" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"></path>
      </svg>
      <div class="badge">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <path d="M35 4L15 35h16l-3 25 21-35H33l2-21z" fill="none" stroke="currentColor" stroke-width="5" stroke-linejoin="round"></path>
        </svg>
      </div>
      <div class="evname">SavingsSubscription<br>CreatedEvent</div>
    </div>

    <div class="stepcard frag" data-step="3">
      <div class="bar"></div>
      <div class="head"><div class="num">02</div><div class="stitle">고정지출 목록</div></div>
      <div class="shotwrap"><img class="shot-list" src="assets/savlink_list.png" alt="고정지출 목록 화면"></div>
      <div class="callout">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <circle cx="32" cy="32" r="25" fill="currentColor" opacity=".14"></circle>
          <path d="M20 32l8 8 17-19" fill="none" stroke="currentColor" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"></path>
        </svg>
        <span>저축 · 계좌이체 · <span class="green">매월 27일</span>로<br>자동 등록됩니다.</span>
      </div>
    </div>

    <div class="connector frag" data-step="4">
      <svg class="arrow-svg" viewBox="0 0 132 36" aria-hidden="true">
        <path d="M5 18h101" fill="none" stroke="currentColor" stroke-width="6" stroke-linecap="round"></path>
        <path d="M94 6l28 12-28 12" fill="none" stroke="currentColor" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"></path>
      </svg>
      <div class="badge">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <path d="M18 8h22l8 8v40H18V8z" fill="none" stroke="currentColor" stroke-width="5" stroke-linejoin="round"></path>
          <path d="M39 8v11h11M25 29h16M25 39h12" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"></path>
        </svg>
      </div>
      <div class="move-text">탭하면<br>상세로</div>
    </div>

    <div class="stepcard frag" data-step="5">
      <div class="bar"></div>
      <div class="head"><div class="num">03</div><div class="stitle">고정지출 상세</div></div>
      <div class="shotwrap"><img class="shot-detail" src="assets/savlink_detail.png" alt="고정지출 상세 화면"></div>
      <div class="sdesc">카테고리 저축 · 결제일 27일로<br>확인할 수 있습니다.</div>
    </div>
  </div>

  <div class="benefits frag" data-step="6">
    <div class="benefit">
      <div class="bicon">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <path d="M32 7l21 8v15c0 14-8.7 23.3-21 28C19.7 53.3 11 44 11 30V15l21-8z" fill="none" stroke="currentColor" stroke-width="5" stroke-linejoin="round"></path>
          <path d="M22 32l7 7 14-16" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"></path>
        </svg>
      </div>
      <div>
        <div class="btitle">안전한 자동연동</div>
        <div class="bdesc">먼저 자동감지·자동해지는 지원하지 않습니다.<br>가입 기록을 삭제하면 연동된 고정지출도 함께 취소됩니다.</div>
      </div>
    </div>
    <div class="benefit">
      <div class="bicon">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <rect x="16" y="28" width="32" height="24" rx="4" fill="none" stroke="currentColor" stroke-width="5"></rect>
          <path d="M23 28v-7a9 9 0 0 1 18 0v7" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round"></path>
          <circle cx="32" cy="40" r="3" fill="currentColor"></circle>
        </svg>
      </div>
      <div>
        <div class="btitle">모듈 분리 설계</div>
        <div class="bdesc">예·적금과 고정지출 도메인은<br>직접 참조하지 않고 독립적으로 동작합니다.</div>
      </div>
    </div>
    <div class="benefit">
      <div class="bicon">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <circle cx="18" cy="20" r="6" fill="none" stroke="currentColor" stroke-width="5"></circle>
          <circle cx="46" cy="20" r="6" fill="none" stroke="currentColor" stroke-width="5"></circle>
          <circle cx="32" cy="46" r="6" fill="none" stroke="currentColor" stroke-width="5"></circle>
          <path d="M23 24l6 12M41 24l-6 12M24 20h16" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round"></path>
        </svg>
      </div>
      <div>
        <div class="btitle">이벤트 기반 연동</div>
        <div class="bdesc">변경사항을 이벤트로 전달해<br>실시간으로 안전하게 반영합니다.</div>
      </div>
    </div>
    <div class="benefit">
      <div class="bicon">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <path d="M17 8h24l8 8v40H17V8z" fill="none" stroke="currentColor" stroke-width="5" stroke-linejoin="round"></path>
          <path d="M40 8v12h11M24 36l6 6 13-15" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"></path>
        </svg>
      </div>
      <div>
        <div class="btitle">데이터 일관성 보장</div>
        <div class="bdesc">가입 정보와 고정지출 상태를 맞춰<br>신뢰할 수 있는 데이터를 제공합니다.</div>
      </div>
    </div>
  </div>
</section>
`);
