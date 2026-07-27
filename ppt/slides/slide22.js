/* ===================== Slide 22 (회원 맞춤형 정책 추천 · 6페이지 형식/파란색 + 호버 확대) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s22{ background:transparent; }

#s22 .htitle{ left:70px; top:50px; width:1150px; color:#171717; font-size:40px; font-weight:800; }
#s22 .hsub{ left:70px; top:112px; width:1150px; color:#333; font-size:20px; font-weight:600; line-height:1.5; }

/* 스크린샷 카드 */
#s22 .shotcard{
  left:70px; top:340px; width:1150px; height:419px; border-radius:18px;
  overflow:hidden; background:#fff; border:1px solid #E3E9F5;
  box-shadow:0 24px 55px rgba(0,0,0,.14);
}
#s22 .shotcard .img{ width:100%; height:100%; background-repeat:no-repeat; background-size:100% auto; }

/* 강조 박스 */
#s22 .hl{ position:absolute; border:3px solid #1667D6; border-radius:10px;
  box-shadow:0 0 0 4px rgba(22,103,214,.12); cursor:pointer; transition:opacity .3s ease; }
#s22.zooming .hl{ opacity:0; }
#s22.zooming .badge{ opacity:0; }
#s22 .zoompop{ position:absolute; opacity:0; pointer-events:none; z-index:20;
  background-repeat:no-repeat; border:3px solid #1667D6; border-radius:12px;
  box-shadow:0 22px 55px rgba(0,0,0,.35); transition:opacity .2s ease; }
#s22 .zoompop.show{ opacity:1; }
#s22 .zoomnum{ position:absolute; top:-18px; left:-18px; width:46px; height:46px; border-radius:50%;
  background:#1667D6; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800; box-shadow:0 8px 18px rgba(22,103,214,.4); }
#s22 .zoomwire{ opacity:0; transition:opacity .2s ease; }
#s22 .zoomwire.show{ opacity:1; }

/* 번호 배지 */
#s22 .badge{ position:absolute; width:40px; height:40px; border-radius:50%;
  background:#1667D6; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:17px; font-weight:800; box-shadow:0 8px 18px rgba(22,103,214,.35); z-index:4;
  transition:opacity .3s ease; }

/* 우측 설명 리스트 */
#s22 .citem{ position:absolute; left:1300px; width:560px; display:flex; align-items:flex-start; gap:18px;
  transform-origin:left center; cursor:pointer; transition:transform .2s ease; }
#s22 .citem.big{ transform:scale(1.5); z-index:6; }
#s22 .cnum{ width:44px; height:44px; border-radius:50%; flex:none; background:#1667D6; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:18px; font-weight:800;
  box-shadow:0 8px 18px rgba(22,103,214,.3); }
#s22 .ctitle{ color:#171717; font-size:23px; font-weight:800; margin-bottom:7px; width:fit-content; padding:0 4px; }
#s22 .cdesc{ color:#555; font-size:17px; font-weight:500; line-height:1.5; }
#s22 .citem.big .ctitle{ background:linear-gradient(transparent 55%, rgba(255,224,20,.65) 55%); }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s22">
  <div class="abs htitle">회원 맞춤형 정책 추천</div>
  <div class="abs hsub">회원 정보에 저장된 지역, 나이, 취업 상태를 기준으로<br>조건에 맞는 정책을 우선 추천합니다.</div>

  <div class="zoompop"><span class="zoomnum"></span></div>
  <svg class="abs zoomwire" viewBox="0 0 1920 1080" width="1920" height="1080" style="left:0;top:0;z-index:19;pointer-events:none">
    <path d="" fill="none" stroke="#1667D6" stroke-width="3" opacity=".75"/>
  </svg>

  <div class="abs shotcard">
    <div class="img" style="background-image:url('assets/policy_recommend.png'); background-position:center top;"></div>
  </div>

  <div class="abs hl" style="left:82px; top:415px; width:220px; height:36px;"></div>
  <div class="abs hl" style="left:108px; top:641px; width:466px; height:44px;"></div>
  <div class="abs hl" style="left:303px; top:415px; width:97px; height:36px;"></div>
  <div class="abs hl" style="left:555px; top:702px; width:74px; height:40px;"></div>

  <div class="abs badge" style="left:66px; top:399px;">01</div>
  <div class="abs badge" style="left:92px; top:625px;">02</div>
  <div class="abs badge" style="left:376px; top:399px;">03</div>
  <div class="abs badge" style="left:539px; top:686px;">04</div>

  <div class="citem" style="top:360px;">
    <div class="cnum">01</div>
    <div><div class="ctitle">회원 정보 기반 자동 추천</div>
      <div class="cdesc">지역, 나이, 취업 상태를<br>추천 기준으로 사용합니다.</div></div>
  </div>
  <div class="citem" style="top:500px;">
    <div class="cnum">02</div>
    <div><div class="ctitle">조건별 추천 이유 제공</div>
      <div class="cdesc">왜 이 정책이 맞는지<br>화면에서 바로 확인합니다.</div></div>
  </div>
  <div class="citem" style="top:640px;">
    <div class="cnum">03</div>
    <div><div class="ctitle">추천 조건 직접 수정</div>
      <div class="cdesc">필요하면 조건을 바꿔<br>다시 탐색할 수 있습니다.</div></div>
  </div>
  <div class="citem" style="top:780px;">
    <div class="cnum">04</div>
    <div><div class="ctitle">정책 상세 조회</div>
      <div class="cdesc">추천 카드에서<br>상세 화면으로 이동합니다.</div></div>
  </div>
</section>
`);

/* ── 호버 확대: 강조박스(또는 설명)에 마우스 올리면 그 영역을 크게 팝업 + 번호 + 직선 연결선 + 소제목 형광펜 ── */
(function(){
  const sec = document.getElementById('s22');
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
})();
