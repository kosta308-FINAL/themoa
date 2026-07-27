/* ===================== Slide 23 (자연어 정책 검색 · 6페이지 형식/파란색 + 호버 확대) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s23{ background:transparent; }

#s23 .htitle{ left:70px; top:50px; width:1200px; color:#171717; font-size:40px; font-weight:800; }
#s23 .hsub{ left:70px; top:112px; width:1180px; color:#333; font-size:20px; font-weight:600; line-height:1.5; }

/* 순서 흐름: 자연어 질문 → 조건 추출 → 정책 검색 결과 */
#s23 .flow{ left:70px; top:184px; width:1100px; display:flex; align-items:center; gap:18px;
  font-size:22px; font-weight:700; }
#s23 .flow .chip{ border:1.5px solid rgba(22,103,214,.42); background:rgba(22,103,214,.07);
  padding:10px 24px; border-radius:999px; color:#12508F;
  transition:background .25s ease, color .25s ease, border-color .25s ease, box-shadow .25s ease; }
#s23 .flow .chip.active{ background:#1667D6; color:#fff; border-color:#1667D6;
  box-shadow:0 8px 20px rgba(22,103,214,.38); }
#s23 .flow .arr{ color:#1667D6; font-size:26px; font-weight:800; }

/* 마지막 강조 문구 (카드) */
#s23 .final{ left:1300px; top:800px; width:544px; padding:24px 30px 26px; border-radius:20px;
  background:linear-gradient(135deg, rgba(22,103,214,.13), rgba(22,103,214,.02));
  border:1px solid rgba(22,103,214,.20); box-shadow:0 16px 38px rgba(22,103,214,.16); }
#s23 .final .flabel{ display:inline-flex; align-items:center; margin-bottom:13px;
  padding:5px 14px; border-radius:999px; background:#1667D6; color:#fff;
  font-size:13px; font-weight:800; letter-spacing:.5px; }
#s23 .final .ftext{ color:#173A66; font-size:23px; font-weight:800; line-height:1.48; letter-spacing:-.4px; }
#s23 .final .ftext b{ color:#1667D6; }

#s23 .shotcard{
  left:70px; top:250px; width:1027px; height:668px; border-radius:18px;
  overflow:hidden; background:#fff; border:1px solid #E3E9F5;
  box-shadow:0 24px 55px rgba(0,0,0,.14);
}
#s23 .shotcard .img{ width:100%; height:100%; background-repeat:no-repeat; background-size:100% auto; }

#s23 .hl{ position:absolute; border:3px solid #1667D6; border-radius:10px;
  box-shadow:0 0 0 4px rgba(22,103,214,.12); cursor:pointer; transition:opacity .3s ease; }
#s23.zooming .hl{ opacity:0; }
#s23.zooming .badge{ opacity:0; }
#s23 .zoompop{ position:absolute; opacity:0; pointer-events:none; z-index:20;
  background-repeat:no-repeat; border:3px solid #1667D6; border-radius:12px;
  box-shadow:0 22px 55px rgba(0,0,0,.35); transition:opacity .2s ease; }
#s23 .zoompop.show{ opacity:1; }
#s23 .zoomnum{ position:absolute; top:-18px; left:-18px; width:46px; height:46px; border-radius:50%;
  background:#1667D6; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800; box-shadow:0 8px 18px rgba(22,103,214,.4); }
#s23 .zoomwire{ opacity:0; transition:opacity .2s ease; }
#s23 .zoomwire.show{ opacity:1; }

#s23 .badge{ position:absolute; width:40px; height:40px; border-radius:50%;
  background:#1667D6; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:17px; font-weight:800; box-shadow:0 8px 18px rgba(22,103,214,.35); z-index:4;
  transition:opacity .3s ease; }

#s23 .citem{ position:absolute; left:1300px; width:560px; display:flex; align-items:flex-start; gap:18px;
  transform-origin:left center; cursor:pointer; transition:transform .2s ease; }
#s23 .citem.big{ transform:scale(1.5); z-index:6; }
#s23 .cnum{ width:44px; height:44px; border-radius:50%; flex:none; background:#1667D6; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:18px; font-weight:800;
  box-shadow:0 8px 18px rgba(22,103,214,.3); }
#s23 .ctitle{ color:#171717; font-size:23px; font-weight:800; margin-bottom:7px; width:fit-content; padding:0 4px; }
#s23 .cdesc{ color:#555; font-size:17px; font-weight:500; line-height:1.5; }
#s23 .citem.big .ctitle{ background:linear-gradient(transparent 55%, rgba(255,224,20,.65) 55%); }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s23">
  <div class="abs htitle">자연어 정책 검색</div>
  <div class="abs hsub">"수원에 사는 27살 취준생이 혜택 받을 정책 있을까?" 처럼<br>자연어로 물어보면 조건을 자동으로 추출해 검색합니다.</div>

  <div class="abs flow">
    <div class="chip">자연어 질문</div>
    <div class="arr">&#8594;</div>
    <div class="chip">조건 추출</div>
    <div class="arr">&#8594;</div>
    <div class="chip">정책 검색 결과</div>
  </div>

  <div class="zoompop"><span class="zoomnum"></span></div>
  <svg class="abs zoomwire" viewBox="0 0 1920 1080" width="1920" height="1080" style="left:0;top:0;z-index:19;pointer-events:none">
    <path d="" fill="none" stroke="#1667D6" stroke-width="3" opacity=".75"/>
  </svg>

  <div class="abs shotcard">
    <div class="img" style="background-image:url('assets/policy_search_result.png'); background-position:center top;"></div>
  </div>

  <div class="abs hl" style="left:82px; top:265px; width:1005px; height:101px;"></div>
  <div class="abs hl" style="left:79px; top:514px; width:407px; height:42px;"></div>
  <div class="abs hl" style="left:69px; top:583px; width:1024px; height:166px;"></div>

  <div class="abs badge" style="left:66px; top:249px;">01</div>
  <div class="abs badge" style="left:63px; top:498px;">02</div>
  <div class="abs badge" style="left:53px; top:567px;">03</div>

  <div class="citem" style="top:300px;">
    <div class="cnum">01</div>
    <div><div class="ctitle">자연어 질문 입력</div>
      <div class="cdesc">사용자가 입력한 문장을<br>그대로 검색합니다.</div></div>
  </div>
  <div class="citem" style="top:480px;">
    <div class="cnum">02</div>
    <div><div class="ctitle">검색 조건 자동 추출</div>
      <div class="cdesc">지역, 나이, 취업 상태, 검색 모드를<br>자동으로 구조화합니다.</div></div>
  </div>
  <div class="citem" style="top:660px;">
    <div class="cnum">03</div>
    <div><div class="ctitle">정책 검색 결과 제공</div>
      <div class="cdesc">조건에 맞는 정책 카드를<br>우선 표시합니다.</div></div>
  </div>

  <div class="abs final">
    <span class="flabel">자동 구조화</span>
    <div class="ftext">자연어로 입력해도<br>검색에 필요한 <b>조건을 자동으로</b> 구조화합니다.</div>
  </div>
</section>
`);

/* ── 호버 확대 ── */
(function(){
  const sec = document.getElementById('s23');
  if(!sec) return;
  const pop = sec.querySelector('.zoompop');
  const zoomnum = pop.querySelector('.zoomnum');
  const zoomwire = sec.querySelector('.zoomwire');
  const zoompath = zoomwire.querySelector('path');
  const card = sec.querySelector('.shotcard');
  const imgEl = sec.querySelector('.shotcard .img');
  const bg = imgEl.style.backgroundImage;
  const CARD_L = card.offsetLeft, CARD_T = card.offsetTop, DISP_W = card.offsetWidth;
  const Z = 1.9;
  const hls = Array.from(sec.querySelectorAll('.hl'));
  const items = Array.from(sec.querySelectorAll('.citem'));
  const chips = Array.from(sec.querySelectorAll('.flow .chip'));
  hls.forEach(function(hl, i){
    const item = items[i];
    const on = function(){
      const rx = hl.offsetLeft - CARD_L;
      const ry = hl.offsetTop  - CARD_T;
      const rw = hl.offsetWidth, rh = hl.offsetHeight;
      const z = Math.min(Z, 1200/rw, 620/rh);
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
      if(chips[i]) chips[i].classList.add('active');
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
      if(chips[i]) chips[i].classList.remove('active');
    };
    [hl, item].forEach(function(t){ if(t){ t.addEventListener('mouseenter', on); t.addEventListener('mouseleave', off); } });
  });
})();
