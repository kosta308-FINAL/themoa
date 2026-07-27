/* ===================== Slide 24 (정책 상세 조회 · 6페이지 형식/파란색 + 호버 확대) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s24{ background:transparent; }

#s24 .htitle{ left:70px; top:50px; width:1150px; color:#171717; font-size:40px; font-weight:800; }
#s24 .hsub{ left:70px; top:112px; width:1150px; color:#333; font-size:20px; font-weight:600; line-height:1.5; }

#s24 .shotcard{
  left:260px; top:200px; width:786px; height:805px; border-radius:18px;
  overflow:hidden; background:#fff; border:1px solid #E3E9F5;
  box-shadow:0 24px 55px rgba(0,0,0,.14);
}
#s24 .shotcard .img{ width:100%; height:100%; background-repeat:no-repeat; background-size:100% auto; }

#s24 .hl{ position:absolute; border:3px solid #1667D6; border-radius:10px;
  box-shadow:0 0 0 4px rgba(22,103,214,.12); cursor:pointer; transition:opacity .3s ease; }
#s24.zooming .hl{ opacity:0; }
#s24.zooming .badge{ opacity:0; }
#s24 .zoompop{ position:absolute; opacity:0; pointer-events:none; z-index:20;
  background-repeat:no-repeat; border:3px solid #1667D6; border-radius:12px;
  box-shadow:0 22px 55px rgba(0,0,0,.35); transition:opacity .2s ease; }
#s24 .zoompop.show{ opacity:1; }
#s24 .zoomnum{ position:absolute; top:-18px; left:-18px; width:46px; height:46px; border-radius:50%;
  background:#1667D6; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800; box-shadow:0 8px 18px rgba(22,103,214,.4); }
#s24 .zoomwire{ opacity:0; transition:opacity .2s ease; }
#s24 .zoomwire.show{ opacity:1; }

#s24 .badge{ position:absolute; width:40px; height:40px; border-radius:50%;
  background:#1667D6; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:17px; font-weight:800; box-shadow:0 8px 18px rgba(22,103,214,.35); z-index:4;
  transition:opacity .3s ease; }

#s24 .citem{ position:absolute; left:1300px; width:560px; display:flex; align-items:flex-start; gap:18px;
  transform-origin:left center; cursor:pointer; transition:transform .2s ease; }
#s24 .citem.big{ transform:scale(1.5); z-index:6; }
#s24 .cnum{ width:44px; height:44px; border-radius:50%; flex:none; background:#1667D6; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:18px; font-weight:800;
  box-shadow:0 8px 18px rgba(22,103,214,.3); }
#s24 .ctitle{ color:#171717; font-size:23px; font-weight:800; margin-bottom:7px; width:fit-content; padding:0 4px; }
#s24 .cdesc{ color:#555; font-size:17px; font-weight:500; line-height:1.5; }
#s24 .citem.big .ctitle{ background:linear-gradient(transparent 55%, rgba(255,224,20,.65) 55%); }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s24">
  <div class="abs htitle">정책 상세 조회</div>
  <div class="abs hsub">정책의 핵심 정보와 신청 경로를<br>한 화면에서 확인할 수 있습니다.</div>

  <div class="zoompop"><span class="zoomnum"></span></div>
  <svg class="abs zoomwire" viewBox="0 0 1920 1080" width="1920" height="1080" style="left:0;top:0;z-index:19;pointer-events:none">
    <path d="" fill="none" stroke="#1667D6" stroke-width="3" opacity=".75"/>
  </svg>

  <div class="abs shotcard">
    <div class="img" style="background-image:url('assets/policy_detail.png'); background-position:center top;"></div>
  </div>

  <div class="abs hl" style="left:291px; top:320px; width:726px; height:115px;"></div>
  <div class="abs hl" style="left:292px; top:455px; width:224px; height:212px;"></div>
  <div class="abs hl" style="left:291px; top:679px; width:726px; height:55px;"></div>
  <div class="abs hl" style="left:291px; top:747px; width:186px; height:71px;"></div>
  <div class="abs hl" style="left:484px; top:747px; width:188px; height:71px;"></div>

  <div class="abs badge" style="left:275px; top:304px;">01</div>
  <div class="abs badge" style="left:276px; top:439px;">02</div>
  <div class="abs badge" style="left:275px; top:663px;">03</div>
  <div class="abs badge" style="left:275px; top:731px;">04</div>
  <div class="abs badge" style="left:468px; top:731px;">05</div>

  <div class="citem" style="top:220px;">
    <div class="cnum">01</div>
    <div><div class="ctitle">정책 기본 정보</div>
      <div class="cdesc">정책 식별값과 정책명을<br>한 화면에서 확인합니다.</div></div>
  </div>
  <div class="citem" style="top:360px;">
    <div class="cnum">02</div>
    <div><div class="ctitle">정책 분류 및 운영 정보</div>
      <div class="cdesc">기관, 분야, 상태, 지역 정보를<br>함께 표시합니다.</div></div>
  </div>
  <div class="citem" style="top:500px;">
    <div class="cnum">03</div>
    <div><div class="ctitle">정책 요약</div>
      <div class="cdesc">정책의 핵심 내용을<br>짧게 확인합니다.</div></div>
  </div>
  <div class="citem" style="top:640px;">
    <div class="cnum">04</div>
    <div><div class="ctitle">즐겨찾기 추가</div>
      <div class="cdesc">관심 정책으로 저장합니다.</div></div>
  </div>
  <div class="citem" style="top:780px;">
    <div class="cnum">05</div>
    <div><div class="ctitle">공식 링크</div>
      <div class="cdesc">공식 안내 페이지로 이동합니다.</div></div>
  </div>
</section>
`);

/* ── 호버 확대 ── */
(function(){
  const sec = document.getElementById('s24');
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
