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
/* 현재 선택된 소카테고리 강조 (클릭으로 이동) */
#s3 .flow [data-node]{ transition:opacity .5s; }
#s3.morphed .flow [data-node]{ cursor:pointer; }
#s3.morphed .flow [data-node]:not(.sel){ opacity:.42; }
#s3.morphed .flow .steptxt.sel .stitle{ color:#007613; }
#s3.morphed .flow .num.sel{ color:#007613; }
#s3.morphed .flow .node.sel{ box-shadow:0 12px 26px rgba(0,118,19,.5); }

/* 서비스 카드: 모프 후 등장 */
#s3 .svccard{ position:absolute; inset:0; opacity:0; transform:translateX(48px);
  pointer-events:none; transition:opacity .55s .12s ease, transform .55s .12s ease; }
#s3.morphed .svccard{ opacity:1; transform:translateX(0); }
#s3 .svccard .chip{ pointer-events:auto; }   /* 컨테이너는 통과, 칩만 클릭 */
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
/* 소카테고리 클릭 시 오른쪽: 주제선정=카드, 나머지=준비중 자리표시 */
#s3 .svccard.off{ opacity:0 !important; pointer-events:none !important;
  transition:opacity .12s ease !important; }   /* 전환 시 빠르게 사라짐 */
#s3 .placeholder{ position:absolute; left:940px; top:311px; width:773px; height:518px;
  border:2px dashed #b8c4be; border-radius:16px; background:#fafcfb;
  display:none; flex-direction:column; align-items:center; justify-content:center;
  text-align:center; padding:0 40px; z-index:2; }
#s3.morphed .placeholder.show{ display:flex; }
#s3 .placeholder .pht{ color:#2D8A5E; font-size:54px; font-weight:800; margin-bottom:14px; }
#s3 .placeholder .phs{ color:#8a97a0; font-size:26px; }
/* 기능설계 클릭 시: 기능서 gif */
#s3 .featgif{ position:absolute; left:985px; top:360px; width:790px; display:none;
  border-radius:16px; overflow:hidden; background:#fff;
  border:3px solid #000; box-shadow:0 18px 40px rgba(0,0,0,.16); z-index:2; }
#s3.morphed .featgif.show{ display:block; }
#s3 .featgif img{ width:100%; display:block; }
/* 기능설계: "총 202개의 기능" 라벨 (gif 우상단) */
#s3 .feattag{ position:absolute; left:1452px; top:334px; width:305px; height:56px;
  background:#fff; border:2px solid #000; border-radius:4px;
  display:none; align-items:center; justify-content:center;
  color:#333; font-size:27px; font-weight:700; z-index:3; }
#s3.morphed .feattag.show{ display:flex; }
/* 흐름설계서: 유즈케이스 다이어그램 (클릭 시 확대) */
#s3 .flowimg{ position:absolute; left:950px; top:343px; width:920px; display:none;
  border-radius:16px; overflow:hidden; background:#fff; cursor:zoom-in;
  border:3px solid #000; box-shadow:0 18px 40px rgba(0,0,0,.16); z-index:2; }
#s3.morphed .flowimg.show{ display:block; }
#s3 .flowimg img{ width:100%; display:block; }
/* 테이블 설계: ERD (세로로 긴 이미지라 높이 기준으로 표시, 클릭 시 확대) */
#s3 .erdimg{ position:absolute; left:951px; top:145px; height:860px; display:none;
  border-radius:16px; overflow:hidden; background:#fff; cursor:zoom-in;
  border:3px solid #000; box-shadow:0 18px 40px rgba(0,0,0,.16); z-index:2; }
#s3.morphed .erdimg.show{ display:block; }
#s3 .erdimg img{ height:100%; width:auto; display:block; }
/* 확대 라이트박스 — 현재 페이지를 살짝 블러 처리한 배경 */
#s3 .lightbox{ position:absolute; inset:0; z-index:50; display:none;
  background:rgba(245,255,248,.35);
  backdrop-filter:blur(14px); -webkit-backdrop-filter:blur(14px);
  align-items:center; justify-content:center; cursor:zoom-out; }
#s3 .lightbox.show{ display:flex; }
#s3 .lightbox .lb-content img, #s3 .lightbox .lb-content video{
  max-width:1800px; max-height:1000px; object-fit:contain;
  border-radius:10px; border:1px solid rgba(0,0,0,.1);
  box-shadow:0 24px 70px rgba(0,0,0,.35); }
/* agent규칙/팀규칙: 프론트/백 탭 박스 */
#s3 .agentbox{ position:absolute; left:915px; top:300px; width:905px; display:none; z-index:2; }
#s3.morphed .agentbox.show{ display:block; }
#s3 .agentbox .a-head{ color:#112F8D; font-size:30px; font-weight:800; margin-bottom:10px; }
#s3 .agentbox .a-tree{ background:#1e2430; color:#d6e2ff; font-family:ui-monospace,Menlo,Consolas,monospace;
  font-size:16px; line-height:1.5; padding:12px 18px; border-radius:9px; white-space:pre; margin-bottom:16px; }
#s3 .agentbox .a-tabs{ display:flex; gap:8px; }
#s3 .agentbox .a-tab{ padding:9px 26px; font-size:19px; font-weight:700; cursor:pointer;
  background:#e8eef7; color:#6B7C8A; border-radius:11px 11px 0 0; border:2px solid transparent; border-bottom:none; }
#s3 .agentbox .a-tab.on{ background:#fff; color:#112F8D; border-color:#d0d8e4; }
#s3 .agentbox .a-detail{ background:#fff; border:2px solid #d0d8e4; border-radius:0 11px 11px 11px;
  padding:18px 22px; box-shadow:0 12px 30px rgba(0,0,0,.08); }
#s3 .agentbox .row{ margin-bottom:11px; font-size:25px; line-height:1.45; }
#s3 .agentbox .fname{ color:#2D8A5E; font-weight:800; }
#s3 .agentbox .fdesc{ color:#444; }
#s3 .agentbox .a-thumbs{ display:flex; gap:12px; margin-top:14px; }
#s3 .agentbox .a-thumbs img, #s3 .agentbox .a-thumbs video{
  height:150px; width:auto; max-width:275px; object-fit:cover; object-position:top left;
  border:2px solid #d0d8e4; border-radius:8px; cursor:zoom-in; background:#fff; }
#s3 .agentbox .a-back{ display:none; }
#s3 .agentbox.showback .a-front{ display:none; }
#s3 .agentbox.showback .a-back{ display:block; }
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

  <!-- 기능설계 클릭 시: 기능서 gif + "총 202개의 기능" 라벨 -->
  <div class="featgif"><img src="assets/feat_spec.gif" alt="기능설계 기능서"></div>
  <div class="feattag">총 202개의 기능</div>

  <!-- 흐름설계서 클릭 시: 유즈케이스 다이어그램 -->
  <div class="flowimg"><img src="assets/flow_usecase.png" alt="흐름설계서 유즈케이스"></div>

  <!-- 테이블 설계 클릭 시: ERD -->
  <div class="erdimg"><img src="assets/erd.png" alt="테이블 설계 ERD"></div>

  <!-- 확대 보기(다이어그램·스크린샷·영상 공용) -->
  <div class="lightbox"><div class="lb-content"></div></div>

  <!-- agent규칙/팀규칙: 프론트/백 탭 박스 -->
  <div class="agentbox">
    <div class="a-head">agent규칙/팀규칙 명시</div>
    <div class="a-tree">ROOT
├─ backend  → backendrule.md, 작업지시서.md, erd.md
└─ frontend → 화면흐름서.md, frontmustrule.md</div>
    <div class="a-tabs">
      <div class="a-tab on" data-side="front">프론트엔드</div>
      <div class="a-tab" data-side="back">백엔드</div>
    </div>
    <div class="a-detail">
      <div class="a-front">
        <div class="row"><span class="fname">frontmustrule.md</span> — <span class="fdesc">컴포넌트·Hook 분리 등 프론트엔드 구조·개발 규칙 정의</span></div>
        <div class="row"><span class="fname">화면흐름서.md</span> — <span class="fdesc">화면 이동 및 UI 흐름 정의 → 화면 구현 기준</span></div>
        <div class="a-thumbs">
          <img class="zoom" src="assets/agent_f_naming.png" alt="네이밍·화면추가 규칙">
          <img class="zoom" src="assets/agent_front.gif" alt="프론트 화면 흐름">
        </div>
      </div>
      <div class="a-back">
        <div class="row"><span class="fname">backendrule.md</span> — <span class="fdesc">네이밍·구조·record 등 백엔드 규칙 (AI Agent 필수 준수)</span></div>
        <div class="row"><span class="fname">작업지시서.md</span> — <span class="fdesc">섹션별 기능 구현 체크리스트·구현 방법</span></div>
        <div class="row"><span class="fname">erd.md</span> — <span class="fdesc">작업지시서 기반 Entity 구조 정의</span></div>
        <div class="a-thumbs">
          <img class="zoom" src="assets/agent_b_checklist.png" alt="Coding Agent 체크리스트">
          <img class="zoom" src="assets/agent_b_rule.png" alt="backendrule.md">
          <img class="zoom" src="assets/agent_b_erd.png" alt="ERD 스키마 정의">
        </div>
      </div>
    </div>
  </div>

  <!-- 그 외 소카테고리 클릭 시 자리표시 (내용은 나중에 채움) -->
  <div class="placeholder">
    <div class="pht"></div>
    <div class="phs">상세 내용 준비중</div>
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

// ---- 슬라이드3(모프 상태): 소카테고리 클릭 → 선택 이동 + 오른쪽 내용 전환 ----
(function(){
  const sec = document.getElementById('s3');
  const svccard = sec.querySelector('.svccard');
  const gif = sec.querySelector('.featgif');
  const tag = sec.querySelector('.feattag');
  const flowimg = sec.querySelector('.flowimg');
  const erdimg = sec.querySelector('.erdimg');
  const agentbox = sec.querySelector('.agentbox');
  const ph = sec.querySelector('.placeholder');
  const phName = ph.querySelector('.pht');
  const names = {1:'주제선정',2:'기능설계',3:'테이블 설계',4:'흐름설계서',5:'agent규칙/팀규칙 명시'};
  const groups = {};
  sec.querySelectorAll('.flow [data-node]').forEach(el=>{
    (groups[el.dataset.node] = groups[el.dataset.node] || []).push(el);
  });
  const allEls = Object.values(groups).flat();
  function selectItem(n){
    n = String(n);
    allEls.forEach(e=> e.classList.remove('sel'));
    (groups[n]||[]).forEach(e=> e.classList.add('sel'));
    // 오른쪽 내용: 1=서비스카드, 2=기능서 gif, 나머지=준비중 자리표시
    svccard.classList.toggle('off', n !== '1');
    gif.classList.toggle('show', n === '2');
    tag.classList.toggle('show', n === '2');
    erdimg.classList.toggle('show', n === '3');
    flowimg.classList.toggle('show', n === '4');
    agentbox.classList.toggle('show', n === '5');
    const isPh = (n !== '1' && n !== '2' && n !== '3' && n !== '4' && n !== '5');
    ph.classList.toggle('show', isPh);
    if(isPh) phName.textContent = names[n];
  }
  // 클릭: 모프 상태면 선택 이동(슬라이드 안 넘어가게), 아니면 그대로(=모프 진행)
  let pendingSel = null;
  allEls.forEach(el=>{
    el.addEventListener('click', function(e){
      if(sec.classList.contains('morphed')){
        e.stopPropagation();
        selectItem(el.dataset.node);
      } else {
        // 첫 화면(모프 전)에서 클릭한 항목으로 바로 이동하도록 예약
        pendingSel = el.dataset.node;
      }
    });
  });
  // 모프되면 예약된 항목(없으면 주제선정)으로
  new MutationObserver(function(){
    if(sec.classList.contains('morphed')){
      selectItem(pendingSel || '1');
      pendingSel = null;
    }
  }).observe(sec, { attributes:true, attributeFilter:['class'] });

  // 유즈케이스 다이어그램 클릭 → 확대 / 라이트박스 클릭 → 닫기
  const lightbox = sec.querySelector('.lightbox');
  const lbContent = lightbox.querySelector('.lb-content');
  function openLB(node){
    const clone = node.cloneNode(true);
    clone.removeAttribute('class');
    if(clone.tagName === 'VIDEO'){ clone.controls = true; clone.muted = false; clone.loop = true;
      setTimeout(function(){ clone.play().catch(function(){}); }, 60); }
    lbContent.innerHTML = '';
    lbContent.appendChild(clone);
    lightbox.classList.add('show');
  }
  lightbox.addEventListener('click', function(e){ e.stopPropagation(); lightbox.classList.remove('show'); lbContent.innerHTML = ''; });
  // 흐름설계서 다이어그램 확대
  flowimg.addEventListener('click', function(e){ e.stopPropagation(); openLB(flowimg.querySelector('img')); });
  // 테이블 설계(ERD) 확대
  erdimg.addEventListener('click', function(e){ e.stopPropagation(); openLB(erdimg.querySelector('img')); });
  // agent규칙 썸네일(스크린샷·영상) 확대
  sec.querySelectorAll('.agentbox .zoom, .agentbox .zoom-v').forEach(function(node){
    node.addEventListener('click', function(e){ e.stopPropagation(); openLB(node); });
  });

  // agent규칙: 프론트/백 탭 클릭
  sec.querySelectorAll('.agentbox .a-tab').forEach(function(t){
    t.addEventListener('click', function(e){
      e.stopPropagation();
      sec.querySelectorAll('.agentbox .a-tab').forEach(x=> x.classList.remove('on'));
      t.classList.add('on');
      agentbox.classList.toggle('showback', t.dataset.side === 'back');
    });
  });
})();
