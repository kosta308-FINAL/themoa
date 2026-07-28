/* ===================== 그래서, 이렇게 만들었다 (기획 배경 다음 슬라이드) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#sSolution{ background:transparent; }
#sSolution .ptitle{ position:absolute; left:52px; top:40px; width:1100px; color:#6B7C8A; font-size:45px; font-weight:800; }
#sSolution .psub{ position:absolute; left:52px; top:118px; width:1500px; color:#111; font-size:24px; font-weight:500; }

#sSolution .cards{ position:absolute; left:52px; top:260px; width:1816px; height:640px; display:flex; gap:40px; }
#sSolution .card{ position:relative; overflow:hidden; flex:1; background:#fff; border-radius:24px;
  box-shadow:0 16px 34px rgba(30,60,45,.10); padding:44px 34px 38px;
  display:flex; flex-direction:column; align-items:center; text-align:center; gap:18px; }
#sSolution .card::before{ content:''; position:absolute; top:0; left:0; width:100%; height:7px; }
#sSolution .c1::before{ background:linear-gradient(90deg,#2F6FED,#7FB0FF); }
#sSolution .c2::before{ background:linear-gradient(90deg,#007613,#4FBE5D); }
#sSolution .c3::before{ background:linear-gradient(90deg,#7C3AED,#B48CF7); }

#sSolution .cicon{ width:76px; height:76px; border-radius:50%; margin-top:10px;
  display:flex; align-items:center; justify-content:center; }
#sSolution .c1 .cicon{ background:#EAF1FF; } #sSolution .c1 .cicon svg{ stroke:#2F6FED; color:#2F6FED; }
#sSolution .c2 .cicon{ background:#E8F5EA; } #sSolution .c2 .cicon svg{ stroke:#007613; color:#007613; }
#sSolution .c3 .cicon{ background:#F1EBFC; } #sSolution .c3 .cicon svg{ stroke:#7C3AED; color:#7C3AED; }
#sSolution .cicon svg{ width:34px; height:34px; }

#sSolution .ctitle{ font-size:27px; font-weight:800; color:#171717; letter-spacing:-.2px; margin-top:6px; }
#sSolution .cdesc{ font-size:19px; font-weight:500; color:#5B6470; line-height:1.75; word-break:keep-all; }

/* 설명 아래 빈 공간을 채우는 큰 일러스트 */
#sSolution .bigicon{ width:220px; height:220px; margin:auto auto 0; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="sSolution">
  <div class="ptitle">그래서, 이런 서비스를 만들었습니다</div>
  <div class="psub">문제를 하나씩 풀어가며, 세 가지 핵심 기능으로 답했습니다</div>

  <div class="cards">
    <div class="card c1">
      <div class="cicon"><svg viewBox="0 0 24 24" fill="none" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3 7a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v2h1a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1h-1v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7Z"/>
        <path d="M16 12h2"/>
      </svg></div>
      <div class="ctitle">소비 관리</div>
      <div class="cdesc">고정지출이 얼마나, 언제 빠져나가는지 파악하고,<br>수입 대비 소비 권장액을 역산해<br>무엇에 얼마를 썼는지 한눈에 보여드립니다</div>
      <svg class="bigicon" viewBox="0 0 100 100">
        <ellipse cx="50" cy="88" rx="34" ry="6" fill="#EAF1FF"/>
        <rect x="14" y="30" width="72" height="50" rx="12" fill="#2F6FED"/>
        <path d="M14 30 L50 8 L86 30" fill="none" stroke="#7FB0FF" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
        <rect x="20" y="42" width="60" height="30" rx="8" fill="#EAF1FF"/>
        <circle cx="66" cy="57" r="9" fill="#FFC94D"/>
        <rect x="20" y="42" width="22" height="30" rx="8" fill="#7FB0FF"/>
      </svg>
    </div>

    <div class="card c3">
      <div class="cicon"><svg viewBox="0 0 24 24" fill="none" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M12.5 5.5c-3.5 0-6.5 2.2-6.5 5.6 0 1.5.5 2.6 1.3 3.4V17a1 1 0 0 0 1 1H10v-1.3c.6.1 1.3.2 2 .2s1.4-.1 2-.2V18a1 1 0 0 0 1 1h1.7a1 1 0 0 0 1-1v-2.5c1-.8 1.8-2 1.8-3.5v-.6h1.1c.5 0 .9-.4.9-.9 0-.9-.7-1.6-1.6-1.6h-.7C18.4 7 15.8 5.5 12.5 5.5Z"/>
        <path d="M8.7 7.2 7.3 5.8"/>
        <circle cx="15.3" cy="10" r=".55" fill="currentColor" stroke="none"/>
      </svg></div>
      <div class="ctitle">금융상품 추천</div>
      <div class="cdesc">남는 잉여금을 그냥 두지 않고, 어울리는<br>예금·적금 상품으로 연결합니다</div>
      <svg class="bigicon" viewBox="0 0 100 100">
        <ellipse cx="50" cy="88" rx="34" ry="6" fill="#F1EBFC"/>
        <path d="M22 55c0-18 12-30 28-30 6 0 10 3 13 7 1.5 2 3.5 2 5 0 1.5-2 5-2 6 1l2 4c1 .5 2 1 4 1.2 4 .5 6 3 6 6.3 0 2-1.5 3.5-3.5 3.5-1.5 0-2 1.5-2 3v3c0 2.5-2 4.5-4.5 4.5H72v5c0 2-1.6 3.6-3.6 3.6h-6.8c-2 0-3.6-1.6-3.6-3.6v-5H36v5c0 2-1.6 3.6-3.6 3.6h-4c-2 0-3.6-1.6-3.6-3.6V63c-1.7-1.6-2.8-3.9-2.8-8Z" fill="#7C3AED"/>
        <circle cx="60" cy="41" r="2.6" fill="#fff"/>
        <path d="M30 47 22 40" stroke="#B48CF7" stroke-width="4" stroke-linecap="round"/>
        <circle cx="60" cy="16" r="10" fill="#FFC94D"/>
        <path d="M56 16 l3 3 6-6" fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </div>

    <div class="card c2">
      <div class="cicon"><svg viewBox="0 0 24 24" fill="none" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3 10 12 4l9 6"/><path d="M5 10v9M10 10v9M14 10v9M19 10v9"/><path d="M3 19h18"/>
      </svg></div>
      <div class="ctitle">정책 추천</div>
      <div class="cdesc">내 상황에 꼭 맞는 청년 정책을 자동으로 찾아<br>추천해드립니다</div>
      <svg class="bigicon" viewBox="0 0 100 100">
        <ellipse cx="50" cy="88" rx="36" ry="6" fill="#E8F5EA"/>
        <path d="M50 10 L88 32 L12 32 Z" fill="#4FBE5D"/>
        <rect x="16" y="36" width="8" height="38" rx="2" fill="#007613"/>
        <rect x="34" y="36" width="8" height="38" rx="2" fill="#007613"/>
        <rect x="58" y="36" width="8" height="38" rx="2" fill="#007613"/>
        <rect x="76" y="36" width="8" height="38" rx="2" fill="#007613"/>
        <rect x="10" y="76" width="80" height="10" rx="3" fill="#007613"/>
        <circle cx="50" cy="21" r="5" fill="#FFC94D"/>
      </svg>
    </div>
  </div>
</section>
`);
