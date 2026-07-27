/* ===================== Slide 42 (금융상품 검색 · 6페이지 형식/보라색 + 호버 확대) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s42{ background:transparent; }

#s42 .htitle{ left:70px; top:50px; width:1150px; color:#171717; font-size:40px; font-weight:800; }
#s42 .hsub{ left:70px; top:112px; width:1150px; color:#333; font-size:20px; font-weight:600; line-height:1.5; }

#s42 .shotcard{
  left:70px; top:175px; width:1150px; height:884px; border-radius:18px;
  overflow:hidden; background:#fff; border:1px solid #ECE6FA;
  box-shadow:0 24px 55px rgba(0,0,0,.14);
}
#s42 .shotcard .img{ width:100%; height:100%; background-repeat:no-repeat; background-size:100% auto; }

#s42 .hl{ position:absolute; border:3px solid #7C3AED; border-radius:10px;
  box-shadow:0 0 0 4px rgba(124,58,237,.12); cursor:pointer; transition:opacity .3s ease; }
#s42.zooming .hl{ opacity:0; }
#s42.zooming .badge{ opacity:0; }
#s42 .zoompop{ position:absolute; opacity:0; pointer-events:none; z-index:20;
  background-repeat:no-repeat; border:3px solid #7C3AED; border-radius:12px;
  box-shadow:0 22px 55px rgba(0,0,0,.35); transition:opacity .2s ease; }
#s42 .zoompop.show{ opacity:1; }
#s42 .zoomnum{ position:absolute; top:-18px; left:-18px; width:46px; height:46px; border-radius:50%;
  background:#7C3AED; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800; box-shadow:0 8px 18px rgba(124,58,237,.4); }
#s42 .zoomwire{ opacity:0; transition:opacity .2s ease; }
#s42 .zoomwire.show{ opacity:1; }

#s42 .badge{ position:absolute; width:40px; height:40px; border-radius:50%;
  background:#7C3AED; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:17px; font-weight:800; box-shadow:0 8px 18px rgba(124,58,237,.35); z-index:4;
  transition:opacity .3s ease; }

#s42 .citem{ position:absolute; left:1300px; width:560px; display:flex; align-items:flex-start; gap:18px;
  transform-origin:left center; cursor:pointer; transition:transform .2s ease; }
#s42 .citem.big{ transform:scale(1.5); z-index:6; }
#s42 .cnum{ width:44px; height:44px; border-radius:50%; flex:none; background:#7C3AED; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:18px; font-weight:800;
  box-shadow:0 8px 18px rgba(124,58,237,.3); }
#s42 .ctitle{ color:#171717; font-size:23px; font-weight:800; margin-bottom:7px; width:fit-content; padding:0 4px; }
#s42 .cdesc{ color:#555; font-size:17px; font-weight:500; line-height:1.5; }
#s42 .citem.big .ctitle{ background:linear-gradient(transparent 55%, rgba(255,224,20,.65) 55%); }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s42">
  <div class="abs htitle">금융상품 검색</div>
  <div class="abs hsub">원하는 조건을 자연어로 입력하면<br>예금·적금·대출을 한 번에 찾아드립니다.</div>

  <div class="zoompop"><span class="zoomnum"></span></div>
  <svg class="abs zoomwire" viewBox="0 0 1920 1080" width="1920" height="1080" style="left:0;top:0;z-index:19;pointer-events:none">
    <path d="" fill="none" stroke="#7C3AED" stroke-width="3" opacity=".75"/>
  </svg>

  <div class="abs shotcard">
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
</section>
`);

/* ── 호버 확대 ── */
(function(){
  const sec = document.getElementById('s42');
  if(!sec) return;
  const pop = sec.querySelector('.zoompop');
  const zoomnum = pop.querySelector('.zoomnum');
  const zoomwire = sec.querySelector('.zoomwire');
  const zoompath = zoomwire.querySelector('path');
  const card = sec.querySelector('.shotcard');
  const imgEl = sec.querySelector('.shotcard .img');
  const bg = imgEl.style.backgroundImage;
  const CARD_L = card.offsetLeft, CARD_T = card.offsetTop, DISP_W = card.offsetWidth;
  const Z = 2.1;
  const hls = Array.from(sec.querySelectorAll('.hl'));
  const items = Array.from(sec.querySelectorAll('.citem'));
  hls.forEach(function(hl, i){
    const item = items[i];
    const on = function(){
      const rx = hl.offsetLeft - CARD_L;
      const ry = hl.offsetTop  - CARD_T;
      const rw = hl.offsetWidth, rh = hl.offsetHeight;
      const z = Math.min(Z, 1150/rw, 560/rh);
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
