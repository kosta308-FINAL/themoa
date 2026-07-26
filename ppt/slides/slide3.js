/* ===================== Slide 3 ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s3{ background:transparent; }
#s3 .ptitle{ left:52px; top:40px; width:711px; color:#6B7C8A; font-size:45px; font-weight:800; }
/* 중앙 허브 */
#s3 .hub{ left:168px; top:335px; width:430px; height:430px; border-radius:50%;
  background:#fff; box-shadow:0 24px 55px rgba(0,0,0,.13);
  display:flex; align-items:center; justify-content:center; z-index:1;
  position:absolute; overflow:hidden; }
#s3 .hub .hublabel{ color:#112F8D; font-size:47px; font-weight:800; letter-spacing:1px;
  transition:opacity .3s; }
/* 허브 사진 영역(호버 시 표시) */
#s3 .hubphoto{ position:absolute; inset:0; border-radius:50%;
  background-color:#ffffff; background-position:center;
  background-size:contain; background-repeat:no-repeat;
  display:flex; align-items:flex-end; justify-content:center;
  opacity:0; transition:opacity .35s; }
#s3 .hubphoto .cap{ display:none; }
#s3 .hub.show-photo .hubphoto{ opacity:1; }
#s3 .hub.show-photo .hublabel{ opacity:0; }
/* 단계 노드 */
#s3 .node{ position:absolute; width:128px; height:128px; border-radius:50%;
  display:flex; align-items:center; justify-content:center;
  box-shadow:0 12px 22px rgba(0,0,0,.20); z-index:3;
  transition:transform .2s ease; }
#s3 .node.hot{ transform:scale(1.1); }
#s3 .node img{ display:block; }
#s3 .num{ position:absolute; width:148px; text-align:right; color:#545454;
  font-size:50px; font-weight:700; z-index:2; }
/* 단계 텍스트(제목+설명) — 호버 대상 */
#s3 .steptxt{ position:absolute; width:820px; z-index:2; cursor:pointer; }
#s3 .stitle{ color:#111; font-size:40px; font-weight:700; transition:color .2s; }
#s3 .sdesc{ color:#111; font-size:36px; font-weight:400; margin-top:14px; transition:color .2s; }
#s3 .steptxt.hot .stitle{ color:#007613; }
#s3 .steptxt.hot .sdesc{ color:#333; }
#s3 .n1{ left:691px; top:85px;  background:#007613; }
#s3 .n2{ left:756px; top:266px; background:#0097B2; }
#s3 .n4{ left:798px; top:462px; background:#C4DECE; }
#s3 .n5{ left:756px; top:658px; background:#E7FFEE; }
#s3 .n6{ left:691px; top:848px; background:#FEF1A8; }

/* 슬라이드3 진입: 각 행이 위에서 순서대로 도르륵 */
#s3.entering .dropitem{
  animation:dropIn .5s cubic-bezier(.2,.75,.3,1) backwards;
  animation-delay:calc(var(--i,0) * .13s);
}
@keyframes dropIn{
  0%  { opacity:0; transform:translateY(-52px); }
  100%{ opacity:1; transform:translateY(0); }
}

/* ============ Slide 3 → 4 : 모프 전환 ============ */
/* 흐름 그룹: 클릭하면 왼쪽으로 0.85배 축소 이동 */
#s3 .flow{ position:absolute; inset:0; transform-origin:0 0;
  transition:transform .65s cubic-bezier(.7,0,.2,1); }
#s3.morphed .flow{ transform:translate(-520.65px,156.3px) scale(0.85); }
/* 허브: 모프되면 사라짐 */
#s3 .hub{ transition:opacity .45s ease, transform .45s ease, box-shadow .3s; }
#s3.morphed .hub{ opacity:0; transform:scale(.6); pointer-events:none; }
/* 현재 소카테고리(주제선정) 강조 */
#s3 .flow [data-node]{ transition:opacity .5s; }
#s3.morphed .flow [data-node]:not([data-node="1"]){ opacity:.42; }
#s3.morphed .flow .steptxt[data-node="1"] .stitle{ color:#007613; }
#s3.morphed .flow .num[data-node="1"]{ color:#007613; }
#s3.morphed .flow .node.n1{ box-shadow:0 12px 26px rgba(0,118,19,.5); }

/* 서비스 카드: 모프 후 등장 */
#s3 .svccard{ position:absolute; inset:0; opacity:0; transform:translateX(48px);
  pointer-events:none; transition:opacity .55s .12s ease, transform .55s .12s ease; }
#s3.morphed .svccard{ opacity:1; transform:translateX(0); pointer-events:auto; }
#s3 .cardframe{ left:940px; top:311px; width:773px; height:518px; z-index:1;
  border:3px solid #000; border-radius:16px; overflow:hidden; background:#fff;
  box-shadow:0 18px 40px rgba(0,0,0,.16); }
#s3 .cardframe img{ width:100%; height:100%; display:block; }
#s3 .hero{ left:1175px; top:363px; width:281px; height:283px; z-index:2; object-fit:contain; }
#s3 .brand{ left:1146px; top:582px; width:338px; text-align:center; z-index:3;
  font-size:45px; font-weight:800; line-height:1; letter-spacing:-.5px;
  background:linear-gradient(90deg,#5BFF93 0%,#5AD0A6 50%,#379100 100%);
  -webkit-background-clip:text; background-clip:text; color:transparent; }
#s3 .subtitle{ left:1073px; top:654px; width:485px; text-align:center;
  color:#2C3E50; font-size:17px; font-weight:600; z-index:3; }
#s3 .target{ left:1133px; top:694px; width:364px; text-align:center;
  color:#6B7C8A; font-size:11px; font-weight:500; z-index:3; }
/* 클릭 가능한 칩(버튼 느낌) */
#s3 .chip{ position:absolute; width:159px; height:40px; background:#E8F5EE; border-radius:999px;
  display:flex; align-items:center; justify-content:center;
  color:#2C3E50; font-size:15px; font-weight:600; z-index:3;
  cursor:pointer; border:1px solid #cfe9db; box-shadow:0 2px 5px rgba(0,0,0,.08);
  transition:transform .15s, box-shadow .15s, background .15s; }
#s3 .chip:hover{ background:#d6f0e2; transform:translateY(-2px);
  box-shadow:0 6px 15px rgba(0,118,19,.24); }
#s3 .chip:active{ transform:translateY(0); box-shadow:0 2px 5px rgba(0,0,0,.12); }
#s3 .cursor{ left:1375px; top:751px; width:45px; height:65px; z-index:4;
  filter:drop-shadow(0 2px 3px rgba(0,0,0,.25)); pointer-events:none; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s3" data-morph>
  <div class="abs ptitle">설계 흐름도</div>

  <!-- 중앙 허브 (호버 시 사진) -->
  <div class="abs hub">
    <span class="hublabel">설계과정</span>
    <div class="hubphoto"><span class="cap"></span></div>
  </div>

  <!-- 흐름 그룹 (클릭 시 왼쪽으로 축소 모프) -->
  <div class="flow">
    <svg class="abs conn" viewBox="0 0 1920 1080" width="1920" height="1080"
         style="left:0;top:0;z-index:2;pointer-events:none">
      <path d="M755,149 C815,240 862,430 862,526 C862,622 815,820 755,912"
            fill="none" stroke="#BEC8DA" stroke-width="8" stroke-linecap="round"/>
    </svg>

    <div class="node n1 dropitem" data-node="1" style="--i:0"><img src="assets/s3_ic01.png" style="height:54px"></div>
    <div class="node n2 dropitem" data-node="2" style="--i:1"><img src="assets/s3_ic02.png" style="height:63px"></div>
    <div class="node n4 dropitem" data-node="3" style="--i:2"><img src="assets/s3_ic04.png" style="height:57px"></div>
    <div class="node n5 dropitem" data-node="4" style="--i:3"><img src="assets/s3_ic05.png" style="height:63px"></div>
    <div class="node n6 dropitem" data-node="5" style="--i:4"><img src="assets/s3_ic06.png" style="height:54px"></div>

    <div class="num dropitem" data-node="1" style="left:815px;top:114px;--i:0">01</div>
    <div class="num dropitem" data-node="2" style="left:883px;top:294px;--i:1">02</div>
    <div class="num dropitem" data-node="3" style="left:893px;top:498px;--i:2">03</div>
    <div class="num dropitem" data-node="4" style="left:875px;top:686px;--i:3">04</div>
    <div class="num dropitem" data-node="5" style="left:821px;top:872px;--i:4">05</div>

    <div class="steptxt dropitem" data-node="1" style="left:1014px;top:96px;--i:0">
      <div class="stitle">주제선정</div>
      <div class="sdesc">사회초년생 소비 문제 정의 및 서비스 방향 설정</div>
    </div>
    <div class="steptxt dropitem" data-node="2" style="left:1082px;top:276px;--i:1">
      <div class="stitle">기능설계</div>
      <div class="sdesc">파트별 역할 분담 확정, 기능 명세서 작성</div>
    </div>
    <div class="steptxt dropitem" data-node="3" style="left:1092px;top:480px;--i:2">
      <div class="stitle">테이블 설계</div>
      <div class="sdesc">ERD 설계 및 DB 환경 구축</div>
    </div>
    <div class="steptxt dropitem" data-node="4" style="left:1074px;top:668px;--i:3">
      <div class="stitle">흐름설계서</div>
      <div class="sdesc">와이어프레임, UI 목업, 사용자 흐름 정의</div>
    </div>
    <div class="steptxt dropitem" data-node="5" style="left:1020px;top:854px;--i:4">
      <div class="stitle">agent규칙/팀규칙 명시</div>
      <div class="sdesc">Agent 규칙 및 팀 협업 규칙 명시</div>
    </div>
  </div>

  <!-- 오른쪽 서비스 카드 (모프 후 등장) -->
  <div class="svccard">
    <div class="abs cardframe"><img src="assets/s4_card.png" alt="브라우저 목업"></div>
    <img class="abs hero" src="assets/s4_hero.png" alt="TheMoa 로고">
    <div class="abs brand">TheMoa</div>
    <div class="abs subtitle">맞춤형 청년 자산관리 플랫폼</div>
    <div class="abs target">Target: 사회초년생 · 청년</div>
    <div class="abs chip" style="left:1051px;top:743px">소비관리</div>
    <div class="abs chip" style="left:1239px;top:743px">정책 추천</div>
    <div class="abs chip" style="left:1427px;top:743px">금융상품 추천</div>
    <img class="abs cursor" src="assets/s4_icon.png" alt="">
  </div>
</section>
`);

// ---- 슬라이드3: 소제목 호버 → 강조 + 허브 사진 ----
(function(){
  const hub = document.querySelector('#s3 .hub');
  if(!hub) return;
  const cap = hub.querySelector('.cap');
  const photoEl = hub.querySelector('.hubphoto');
  // 단계별 사진 경로(추후 파일만 넣으면 자동 적용) + 캡션
  const photos = {1:'assets/s3_photo1.png',2:'assets/s3_photo2.png',3:'assets/s3_photo3.png',
                  4:'assets/s3_photo4.png',5:'assets/s3_photo5.png'};
  const caps   = {1:'주제선정',2:'기능설계',3:'테이블 설계',4:'흐름설계서',5:'agent규칙/팀규칙 명시'};
  const fits   = {2:'cover'};   // 표는 꽉 채우기, 나머지는 전체보기(contain)
  const groups = {};
  document.querySelectorAll('#s3 [data-node]').forEach(el=>{
    (groups[el.dataset.node] = groups[el.dataset.node] || []).push(el);
  });
  const allEls = Object.values(groups).flat();
  function activate(n){
    allEls.forEach(e=> e.classList.remove('hot'));
    (groups[n]||[]).forEach(e=> e.classList.add('hot'));
    photoEl.style.backgroundImage = 'url("'+photos[n]+'")';
    photoEl.style.backgroundSize = fits[n] || 'contain';
    cap.textContent = caps[n];
    hub.classList.add('show-photo');
  }
  function deactivate(){
    allEls.forEach(e=> e.classList.remove('hot'));
    hub.classList.remove('show-photo');
  }
  Object.keys(groups).forEach(n=>{
    groups[n].forEach(el=>{
      el.addEventListener('mouseenter', ()=> activate(n));
      el.addEventListener('mouseleave', deactivate);
    });
  });
  const hv = new URLSearchParams(location.search).get('hover');
  if(hv && groups[hv]) activate(hv);
})();
