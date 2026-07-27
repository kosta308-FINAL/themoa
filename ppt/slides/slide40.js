/* ===================== Slide 40 (맞춤형 금융상품 추천 · 조건 입력 · 6페이지 형식/보라색 + 호버 확대) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s40{ background:transparent; }

#s40 .htitle{ left:70px; top:50px; width:1150px; color:#171717; font-size:40px; font-weight:800; }
#s40 .hsub{ left:70px; top:112px; width:1150px; color:#333; font-size:20px; font-weight:600; line-height:1.5; }

#s40 .shotcard{
  left:70px; top:200px; width:1150px; height:804px; border-radius:18px;
  overflow:hidden; background:#fff; border:1px solid #ECE6FA;
  box-shadow:0 24px 55px rgba(0,0,0,.14);
}
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

#s40 .citem{ position:absolute; left:1300px; width:560px; display:flex; align-items:flex-start; gap:18px;
  transform-origin:left center; cursor:pointer; transition:transform .2s ease; }
#s40 .citem.big{ transform:scale(1.5); z-index:6; }
#s40 .cnum{ width:44px; height:44px; border-radius:50%; flex:none; background:#7C3AED; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:18px; font-weight:800;
  box-shadow:0 8px 18px rgba(124,58,237,.3); }
#s40 .ctitle{ color:#171717; font-size:23px; font-weight:800; margin-bottom:7px; width:fit-content; padding:0 4px; }
#s40 .cdesc{ color:#555; font-size:17px; font-weight:500; line-height:1.5; }
#s40 .citem.big .ctitle{ background:linear-gradient(transparent 55%, rgba(255,224,20,.65) 55%); }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s40">
  <div class="abs htitle">맞춤형 금융 상품 추천 &middot; 조건 입력</div>
  <div class="abs hsub">내 정보와 조건을 입력하면 가입 가능한 적금 중<br>딱 맞는 상품을 골라 추천합니다.</div>

  <div class="zoompop"><span class="zoomnum"></span></div>
  <svg class="abs zoomwire" viewBox="0 0 1920 1080" width="1920" height="1080" style="left:0;top:0;z-index:19;pointer-events:none">
    <path d="" fill="none" stroke="#7C3AED" stroke-width="3" opacity=".75"/>
  </svg>

  <div class="abs shotcard">
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
</section>
`);

/* ── 호버 확대 ── */
(function(){
  const sec = document.getElementById('s40');
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
