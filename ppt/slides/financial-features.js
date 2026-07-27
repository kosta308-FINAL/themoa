/* ===================== 금융상품 핵심기능 (조건 입력 → 추천 결과 → 상품 검색, 클릭으로 전환) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s40{ background:transparent; }

#s40 .step{ position:absolute; inset:0; opacity:0; pointer-events:none; transition:opacity .5s ease; }
#s40[data-step="0"] .step0,
#s40[data-step="1"] .step1,
#s40[data-step="2"] .step2{ opacity:1; pointer-events:auto; }

#s40 .htitle{ left:70px; top:50px; width:1150px; color:#171717; font-size:40px; font-weight:800; }
#s40 .hsub{ left:70px; top:112px; width:1150px; color:#333; font-size:20px; font-weight:600; line-height:1.5; }

#s40 .shotcard{ border-radius:18px; overflow:hidden; background:#fff; border:1px solid #ECE6FA;
  box-shadow:0 24px 55px rgba(0,0,0,.14); }
#s40 .shotcard .img{ width:100%; height:100%; background-repeat:no-repeat; background-size:100% auto; }

#s40 .hl{ position:absolute; border:3px solid #7C3AED; border-radius:10px;
  box-shadow:0 0 0 4px rgba(124,58,237,.12); cursor:pointer; transition:opacity .3s ease; }
#s40.zooming .hl{ opacity:0; }
#s40.zooming .badge{ opacity:0; }
#s40 .zoompop{ position:absolute; opacity:0; pointer-events:none; z-index:20;
  background-repeat:no-repeat; border:3px solid #7C3AED; border-radius:12px;
  box-shadow:0 22px 55px rgba(0,0,0,.35); transition:opacity .2s ease; }
#s40 .zoompop.show{ opacity:1; }
#s40 .zoomnum{ position:absolute; top:-18px; left:-18px; width:46px; height:46px; border-radius:50%;
  background:#7C3AED; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800; box-shadow:0 8px 18px rgba(124,58,237,.4); }
#s40 .zoomwire{ opacity:0; transition:opacity .2s ease; }
#s40 .zoomwire.show{ opacity:1; }

#s40 .badge{ position:absolute; width:40px; height:40px; border-radius:50%;
  background:#7C3AED; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:17px; font-weight:800; box-shadow:0 8px 18px rgba(124,58,237,.35); z-index:4;
  transition:opacity .3s ease; }

#s40 .citem{ position:absolute; left:1300px; width:560px; display:flex; align-items:flex-start; gap:16px;
  transform-origin:left center; cursor:pointer; transition:transform .2s ease; }
#s40 .citem.big{ transform:scale(1.45); z-index:6; }
#s40 .cnum{ width:42px; height:42px; border-radius:50%; flex:none; background:#7C3AED; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:17px; font-weight:800;
  box-shadow:0 8px 18px rgba(124,58,237,.3); }
#s40 .ctitle{ color:#171717; font-size:22px; font-weight:800; margin-bottom:6px; width:fit-content; padding:0 4px; }
#s40 .cdesc{ color:#555; font-size:16.5px; font-weight:500; line-height:1.48; }
#s40 .citem.big .ctitle{ background:linear-gradient(transparent 55%, rgba(255,224,20,.65) 55%); }

/* 클릭 유도 힌트 (마지막 단계에서는 숨김) */
#s40 .hint{
  position:absolute; right:70px; top:970px; color:#6B7C8A; font-size:16px; font-weight:700;
  display:flex; align-items:center; gap:6px; transition:opacity .3s ease; z-index:5;
}
#s40[data-step="2"] .hint{ opacity:0; pointer-events:none; }
#s40 .hint .arrow{ display:inline-block; animation:s40bounce 1.2s ease-in-out infinite; }
@keyframes s40bounce{ 0%,100%{ transform:translateX(0); } 50%{ transform:translateX(6px); } }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s40">
  <!-- 이 슬라이드의 최대 스텝 수(2)를 엔진에 알려주는 표시용 마커 -->
  <div class="frag" data-step="2" style="display:none"></div>

  <div class="abs hint">다음 화면으로 <span class="arrow">→</span></div>

  <!-- 호버 확대(공용): 팝업 + 번호 + 연결선 -->
  <div class="zoompop"><span class="zoomnum"></span></div>
  <svg class="abs zoomwire" viewBox="0 0 1920 1080" width="1920" height="1080" style="left:0;top:0;z-index:19;pointer-events:none">
    <path d="" fill="none" stroke="#7C3AED" stroke-width="3" opacity=".75"/>
  </svg>

  <!-- ========================= STEP 0 : 맞춤형 금융상품 추천 · 조건 입력 ========================= -->
  <div class="step step0">
    <div class="abs htitle">맞춤형 금융 상품 추천 &middot; 조건 입력</div>
    <div class="abs hsub">내 정보와 조건을 입력하면 가입 가능한 적금 중<br>딱 맞는 상품을 골라 추천합니다.</div>

    <div class="abs shotcard" style="left:70px; top:200px; width:1150px; height:804px;">
      <div class="img" style="background-image:url('assets/fin_recommend_input.png'); background-position:center top;"></div>
    </div>

    <div class="abs hl" style="left:97px; top:378px; width:261px; height:311px;"></div>
    <div class="abs hl" style="left:97px; top:688px; width:261px; height:218px;"></div>
    <div class="abs hl" style="left:975px; top:378px; width:219px; height:225px;"></div>

    <div class="abs badge" style="left:81px; top:362px;">01</div>
    <div class="abs badge" style="left:81px; top:672px;">02</div>
    <div class="abs badge" style="left:959px; top:362px;">03</div>

    <div class="citem" style="top:360px;">
      <div class="cnum">01</div>
      <div><div class="ctitle">정보 연동</div>
        <div class="cdesc">회원가입·소비 연동 시 등록된 정보가<br>자동으로 연동됩니다. (수정 가능)</div></div>
    </div>
    <div class="citem" style="top:560px;">
      <div class="cnum">02</div>
      <div><div class="ctitle">조건 입력</div>
        <div class="cdesc">투자성향, 목표금액, 기간 등을<br>직접 입력합니다.</div></div>
    </div>
    <div class="citem" style="top:760px;">
      <div class="cnum">03</div>
      <div><div class="ctitle">실시간 인기 상품</div>
        <div class="cdesc">상품별 북마크 수를 실시간으로<br>집계해 보여줍니다.</div></div>
    </div>
  </div>

  <!-- ========================= STEP 1 : 맞춤형 금융상품 추천 · 추천 결과 ========================= -->
  <div class="step step1">
    <div class="abs htitle">맞춤형 금융 상품 추천 &middot; 추천 결과</div>
    <div class="abs hsub">입력한 조건으로 점수를 매겨 Top5를 추천하고,<br>추천 이유부터 가입 경로까지 한 화면에서 보여줍니다.</div>

    <div class="abs shotcard" style="left:70px; top:200px; width:1150px; height:804px;">
      <div class="img" style="background-image:url('assets/fin_recommend_result.png'); background-position:center top;"></div>
    </div>

    <div class="abs hl" style="left:367px; top:372px; width:602px; height:43px;"></div>
    <div class="abs hl" style="left:367px; top:415px; width:602px; height:272px;"></div>
    <div class="abs hl" style="left:383px; top:509px; width:571px; height:29px;"></div>
    <div class="abs hl" style="left:447px; top:647px; width:129px; height:21px;"></div>
    <div class="abs hl" style="left:383px; top:891px; width:68px; height:27px;"></div>
    <div class="abs hl" style="left:970px; top:378px; width:219px; height:260px;"></div>

    <div class="abs badge" style="left:351px; top:356px;">01</div>
    <div class="abs badge" style="left:351px; top:399px;">02</div>
    <div class="abs badge" style="left:367px; top:493px;">03</div>
    <div class="abs badge" style="left:431px; top:631px;">04</div>
    <div class="abs badge" style="left:367px; top:875px;">05</div>
    <div class="abs badge" style="left:954px; top:362px;">06</div>

    <div class="citem" style="top:212px;">
      <div class="cnum">01</div>
      <div><div class="ctitle">목표 달성 여부</div>
        <div class="cdesc">입력한 목표금액과 기간이 적절한지<br>판단해 알려줍니다.</div></div>
    </div>
    <div class="citem" style="top:345px;">
      <div class="cnum">02</div>
      <div><div class="ctitle">맞춤 금융상품 추천</div>
        <div class="cdesc">가입대상 원문을 파싱, 하드·소프트 필터로<br>점수를 매겨 Top5를 추천합니다.</div></div>
    </div>
    <div class="citem" style="top:478px;">
      <div class="cnum">03</div>
      <div><div class="ctitle">AI 설명</div>
        <div class="cdesc">AI가 추천 이유를<br>이해하기 쉽게 설명합니다.</div></div>
    </div>
    <div class="citem" style="top:611px;">
      <div class="cnum">04</div>
      <div><div class="ctitle">은행 사이트 이동</div>
        <div class="cdesc">해당 상품을 판매하는<br>은행 사이트로 바로 이동합니다.</div></div>
    </div>
    <div class="citem" style="top:744px;">
      <div class="cnum">05</div>
      <div><div class="ctitle">가입 등록</div>
        <div class="cdesc">우대조건·이자율을 계산하고 '소비내역·<br>등록 예적금'으로 연동합니다.</div></div>
    </div>
    <div class="citem" style="top:877px;">
      <div class="cnum">06</div>
      <div><div class="ctitle">예상 만기금 확인</div>
        <div class="cdesc">예상 만기금을 한눈에 비교합니다.</div></div>
    </div>
  </div>

  <!-- ========================= STEP 2 : 금융상품 검색 ========================= -->
  <div class="step step2">
    <div class="abs htitle">금융상품 검색</div>
    <div class="abs hsub">원하는 조건을 자연어로 입력하면<br>예금·적금·대출을 한 번에 찾아드립니다.</div>

    <div class="abs shotcard" style="left:70px; top:175px; width:1150px; height:884px;">
      <div class="img" style="background-image:url('assets/fin_search.png'); background-position:center top;"></div>
    </div>

    <div class="abs hl" style="left:120px; top:474px; width:850px; height:50px;"></div>
    <div class="abs hl" style="left:100px; top:610px; width:528px; height:92px;"></div>
    <div class="abs hl" style="left:997px; top:490px; width:148px; height:106px;"></div>
    <div class="abs hl" style="left:1147px; top:616px; width:38px; height:37px;"></div>

    <div class="abs badge" style="left:104px; top:458px;">01</div>
    <div class="abs badge" style="left:84px; top:594px;">02</div>
    <div class="abs badge" style="left:981px; top:474px;">03</div>
    <div class="abs badge" style="left:1131px; top:600px;">04</div>

    <div class="citem" style="top:300px;">
      <div class="cnum">01</div>
      <div><div class="ctitle">자연어 검색</div>
        <div class="cdesc">원하는 조건을 자연어 문장으로<br>그대로 입력합니다.</div></div>
    </div>
    <div class="citem" style="top:470px;">
      <div class="cnum">02</div>
      <div><div class="ctitle">상품 추천</div>
        <div class="cdesc">입력한 자연어를 기반으로<br>맞는 상품을 추천합니다.</div></div>
    </div>
    <div class="citem" style="top:640px;">
      <div class="cnum">03</div>
      <div><div class="ctitle">조건 정렬</div>
        <div class="cdesc">관련도·금리·가입기간 등<br>원하는 순으로 정렬합니다.</div></div>
    </div>
    <div class="citem" style="top:810px;">
      <div class="cnum">04</div>
      <div><div class="ctitle">북마크</div>
        <div class="cdesc">마음에 드는 상품을<br>북마크로 수집합니다.</div></div>
    </div>
  </div>
</section>
`);

/* ── 호버 확대: 3단계(step0~2) 모두 적용 ── */
(function(){
  const sec = document.getElementById('s40');
  if(!sec) return;
  const pop = sec.querySelector('.zoompop');
  const zoomnum = pop.querySelector('.zoomnum');
  const zoomwire = sec.querySelector('.zoomwire');
  const zoompath = zoomwire.querySelector('path');
  sec.querySelectorAll('.step').forEach(function(step, stepIdx){
    const card = step.querySelector('.shotcard');
    const imgEl = step.querySelector('.shotcard .img');
    if(!card || !imgEl) return;
    const bg = imgEl.style.backgroundImage;
    const CARD_L = card.offsetLeft, CARD_T = card.offsetTop, DISP_W = card.offsetWidth;
    const Z = stepIdx === 0 ? 1.9 : 2.1;
    const hls = Array.from(step.querySelectorAll('.hl'));
    const items = Array.from(step.querySelectorAll('.citem'));
    hls.forEach(function(hl, i){
      const item = items[i];
      const on = function(){
        const rx = hl.offsetLeft - CARD_L;
        const ry = hl.offsetTop  - CARD_T;
        const rw = hl.offsetWidth, rh = hl.offsetHeight;
        const z = Math.min(Z, (stepIdx===0?1200:1150)/rw, (stepIdx===0?620:560)/rh);
        const popW = rw*z, popH = rh*z;
        let popL = hl.offsetLeft - rw*(z-1)/2;
        let popT = hl.offsetTop  - rh*(z-1)/2;
        popL = Math.max(20, Math.min(popL, 1920 - popW - 20));
        popT = Math.max(20, Math.min(popT, 1080 - popH - 20));
        pop.style.left = popL+'px'; pop.style.top = popT+'px';
        pop.style.width = popW+'px'; pop.style.height = popH+'px';
        pop.style.backgroundImage    = bg;
        pop.style.backgroundSize     = (DISP_W*z)+'px auto';
        pop.style.backgroundPosition = (-rx*z)+'px '+(-ry*z)+'px';
        pop.classList.add('show');
        sec.classList.add('zooming');
        if(item) item.classList.add('big');
        const popR = popL+popW, popCY = popT+popH/2;
        if(item){
          zoomnum.textContent = item.querySelector('.cnum').textContent;
          const ex = item.offsetLeft, ey = item.offsetTop + 26;
          zoompath.setAttribute('d', 'M'+popR+','+popCY+' L'+ex+','+ey);
          zoomwire.classList.add('show');
        }
      };
      const off = function(){
        pop.classList.remove('show');
        zoomwire.classList.remove('show');
        sec.classList.remove('zooming');
        if(item) item.classList.remove('big');
      };
      [hl, item].forEach(function(t){ if(t){ t.addEventListener('mouseenter', on); t.addEventListener('mouseleave', off); } });
    });
  });
})();
