/* ===================== 기획 배경 (문제정의 다음 슬라이드) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#sWhy{ background:transparent; }
#sWhy .blobdeco{ position:absolute; right:-220px; top:-220px; width:640px; height:640px; border-radius:50%;
  background:radial-gradient(circle at 30% 30%, rgba(67,168,115,.16), rgba(67,168,115,0) 70%); z-index:0; }
#sWhy .ptitle{ position:absolute; left:52px; top:40px; width:900px; color:#6B7C8A; font-size:45px; font-weight:800; z-index:1; }
#sWhy .psub{ position:absolute; left:52px; top:118px; width:1400px; color:#111; font-size:24px; font-weight:500; z-index:1; }

/* 페르소나 한줄 카드 */
#sWhy .strip{ position:absolute; left:52px; top:210px; width:1816px; height:140px; z-index:1;
  background:#fff; border-radius:20px; box-shadow:0 14px 30px rgba(30,60,45,.10);
  display:flex; align-items:center; gap:26px; padding:0 40px; }
#sWhy .savatar{ flex:none; width:78px; height:78px; border-radius:50%;
  background:linear-gradient(135deg,#6B7C8A,#8FA0AC); color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:26px; font-weight:800;
  box-shadow:0 10px 20px -6px rgba(0,0,0,.28); }
#sWhy .squote{ color:#171717; font-size:27px; font-weight:600; line-height:1.5; }

/* 문제 3카드 */
#sWhy .cards{ position:absolute; left:52px; top:390px; width:1816px; height:280px; z-index:1;
  display:flex; gap:40px; }
#sWhy .card{ flex:1; background:#fff; border-radius:24px; box-shadow:0 16px 34px rgba(30,60,45,.10);
  padding:40px 32px; display:flex; flex-direction:column; align-items:center; text-align:center; gap:16px; }
#sWhy .cicon{ width:64px; height:64px; border-radius:50%; background:#EEF1F3; flex:none;
  display:flex; align-items:center; justify-content:center; }
#sWhy .cicon svg{ width:30px; height:30px; stroke:#6B7C8A; color:#6B7C8A; }
#sWhy .ctitle{ font-size:23px; font-weight:800; color:#171717; letter-spacing:-.2px; }
#sWhy .cdesc{ font-size:17px; font-weight:500; color:#6B7280; line-height:1.5; }

/* 결론 배너 */
#sWhy .banner{ position:absolute; left:52px; top:720px; width:1816px; height:180px; z-index:1;
  background:linear-gradient(135deg,#2D8A5E,#43A873); border-radius:24px; color:#fff;
  display:flex; align-items:center; justify-content:center; text-align:center; padding:0 40px;
  font-size:28px; font-weight:700; white-space:nowrap; box-shadow:0 22px 44px rgba(45,138,94,.32); }
#sWhy .banner b{ font-weight:900; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="sWhy">
  <div class="blobdeco"></div>
  <div class="ptitle">기획 배경</div>
  <div class="psub">사회초년생의 첫 월급, 여기서 시작된 이야기</div>

  <div class="strip">
    <div class="savatar">23</div>
    <div class="squote">&ldquo;처음 받아본 월급, 얼마를 써야 할지 기준이 없어 그냥 썼습니다.&rdquo;</div>
  </div>

  <div class="cards">
    <div class="card">
      <div class="cicon"><svg viewBox="0 0 24 24" fill="none" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3 7a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v2h1a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1h-1v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7Z"/>
        <path d="M16 12h2"/>
      </svg></div>
      <div class="ctitle">얼마나 써야 할지 모르는 소비</div>
      <div class="cdesc">기준 없이 나가는 하루하루의 지출</div>
    </div>
    <div class="card">
      <div class="cicon"><svg viewBox="0 0 24 24" fill="none" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3 6l6 6 4-4 8 8"/><path d="M21 10v6h-6"/>
      </svg></div>
      <div class="ctitle">새는지도 몰랐던 고정지출</div>
      <div class="cdesc">구독료·월세, 얼마가 빠져나가는지 파악 불가</div>
    </div>
    <div class="card">
      <div class="cicon"><svg viewBox="0 0 24 24" fill="none" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M12.5 5.5c-3.5 0-6.5 2.2-6.5 5.6 0 1.5.5 2.6 1.3 3.4V17a1 1 0 0 0 1 1H10v-1.3c.6.1 1.3.2 2 .2s1.4-.1 2-.2V18a1 1 0 0 0 1 1h1.7a1 1 0 0 0 1-1v-2.5c1-.8 1.8-2 1.8-3.5v-.6h1.1c.5 0 .9-.4.9-.9 0-.9-.7-1.6-1.6-1.6h-.7C18.4 7 15.8 5.5 12.5 5.5Z"/>
        <path d="M8.7 7.2 7.3 5.8"/>
        <circle cx="15.3" cy="10" r=".55" fill="currentColor" stroke="none"/>
      </svg></div>
      <div class="ctitle">많이 벌어도 남지 않는 돈</div>
      <div class="cdesc">소득은 늘어도 저축은 제자리</div>
    </div>
  </div>

  <div class="banner">그래서 <b>소비 습관</b> 형성부터 <b>예·적금</b>, 놓치기 쉬운 <b>청년 정책</b>까지 한 곳에서 챙기는 서비스를 만들었습니다.</div>
</section>
`);
