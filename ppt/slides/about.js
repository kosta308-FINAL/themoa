/* ===================== 소개 (4번째 슬라이드) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#sAbout{ background:transparent; }
#sAbout .htitle{ left:52px; top:40px; width:711px; color:#6B7C8A; font-size:45px; font-weight:800; z-index:3; }
#sAbout .hero{ left:0; top:0; width:1920px; height:1080px; object-fit:fill; }
#sAbout .brandmark{ left:1180px; top:270px; width:560px; height:560px; object-fit:contain;
  filter:drop-shadow(0 20px 30px rgba(45,138,94,.18)); }

/* 하단 3개 소개 버튼: 동일 크기, 아이콘 위/제목 아래(좁은 간격) */
#sAbout .chip{
  position:absolute; top:830px; width:300px; height:118px;
  display:flex; flex-direction:column; align-items:center; justify-content:center; gap:6px;
  border-radius:24px;
  background:#fff; border:1px solid #E3EDE7; box-shadow:0 10px 26px rgba(30,60,45,.10);
  cursor:pointer;
  transition:transform .22s cubic-bezier(.2,.8,.3,1), box-shadow .22s ease, background .22s ease, border-color .22s ease;
}
#sAbout .chip:hover{
  transform:translateY(-6px) scale(1.02);
  background:linear-gradient(135deg,#2D8A5E,#43A873);
  border-color:transparent;
  box-shadow:0 22px 40px rgba(28,110,70,.32);
}
#sAbout .chip .icircle{
  width:44px; height:44px; border-radius:50%; background:#EAF6EF; flex:none;
  display:flex; align-items:center; justify-content:center;
  transition:background .22s ease;
}
#sAbout .chip:hover .icircle{ background:rgba(255,255,255,.22); }
#sAbout .chip .icircle svg{ width:22px; height:22px; stroke:#2D8A5E; color:#2D8A5E; transition:stroke .22s ease, color .22s ease; }
#sAbout .chip:hover .icircle svg{ stroke:#fff; color:#fff; }
#sAbout .chip .ctitle{
  color:#171717; font-size:22px; font-weight:800; letter-spacing:-.3px; white-space:nowrap;
  transition:color .22s ease;
}
#sAbout .chip:hover .ctitle{ color:#fff; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="sAbout" data-skip-to="14">
  <div class="abs htitle">소개</div>
  <img class="abs hero" src="assets/about_intro.png" alt="TheMoa 소개">
  <img class="abs brandmark" src="assets/themoa_logo.png" alt="TheMoa 로고">

  <div class="chip" style="left:154px" data-feature="소비관리">
    <div class="icircle"><svg viewBox="0 0 24 24" fill="none" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 7a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v2h1a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1h-1v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7Z"/>
      <path d="M16 12h2"/>
    </svg></div>
    <div class="ctitle">소비관리</div>
  </div>

  <div class="chip" style="left:502px" data-feature="정책추천">
    <div class="icircle"><svg viewBox="0 0 24 24" fill="none" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 10 12 4l9 6"/><path d="M5 10v9M10 10v9M14 10v9M19 10v9"/><path d="M3 19h18"/>
    </svg></div>
    <div class="ctitle">정책 추천</div>
  </div>

  <div class="chip" style="left:850px" data-feature="금융상품추천">
    <div class="icircle"><svg viewBox="0 0 24 24" fill="none" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M12.5 5.5c-3.5 0-6.5 2.2-6.5 5.6 0 1.5.5 2.6 1.3 3.4V17a1 1 0 0 0 1 1H10v-1.3c.6.1 1.3.2 2 .2s1.4-.1 2-.2V18a1 1 0 0 0 1 1h1.7a1 1 0 0 0 1-1v-2.5c1-.8 1.8-2 1.8-3.5v-.6h1.1c.5 0 .9-.4.9-.9 0-.9-.7-1.6-1.6-1.6h-.7C18.4 7 15.8 5.5 12.5 5.5Z"/>
      <path d="M8.7 7.2 7.3 5.8"/>
      <circle cx="15.3" cy="10" r=".55" fill="currentColor" stroke="none"/>
    </svg></div>
    <div class="ctitle">금융상품 추천</div>
  </div>
</section>
`);

/* 버튼 클릭 → 각 기능 핵심소개 페이지로 이동 */
const ABOUT_FEATURE_PAGE = {
  '소비관리': 6,     // key-features.js
  '정책추천': 7,     // slide22.js (정책 4장 중 첫 장, 7~10페이지)
  '금융상품추천': 11 // slide40.js (금융상품 3장 중 첫 장, 11~13페이지)
};
document.querySelectorAll('#sAbout .chip').forEach(function(chip){
  chip.addEventListener('click', function(e){
    e.stopPropagation();          // 클릭해도 슬라이드가 넘어가지 않게
    const page = ABOUT_FEATURE_PAGE[chip.dataset.feature];
    if(page) location.hash = '#' + page;
  });
});
