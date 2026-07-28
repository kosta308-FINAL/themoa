/* ===================== 정책 지원 핵심기능 (회원 맞춤형 추천 → 자연어 검색 → 상세 조회 → 즐겨찾기, 클릭으로 전환) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s22{ background:transparent; }

/* 4단계(step0~3)를 한 슬라이드 안에서 클릭으로 전환 */
#s22 .step{ position:absolute; inset:0; opacity:0; pointer-events:none; transition:opacity .5s ease; }
#s22[data-step="0"] .step0,
#s22[data-step="1"] .step1,
#s22[data-step="2"] .step2,
#s22[data-step="3"] .step3{ opacity:1; pointer-events:auto; }

#s22 .htitle{ left:70px; top:50px; width:1200px; color:#171717; font-size:40px; font-weight:800; }
#s22 .hsub{ left:70px; top:112px; width:1200px; color:#333; font-size:20px; font-weight:600; line-height:1.5; }

/* 스크린샷 카드 (위치·크기는 단계별 inline style로 지정) */
#s22 .shotcard{ border-radius:18px; overflow:hidden; background:#fff; border:1px solid #E3E9F5;
  box-shadow:0 24px 55px rgba(0,0,0,.14); }
#s22 .shotcard .img{ width:100%; height:100%; background-repeat:no-repeat; background-size:100% auto; }

/* 순서 흐름(자연어 검색 단계 전용): 자연어 질문 → 조건 추출 → 정책 검색 결과 */
#s22 .flow{ left:70px; top:184px; width:1100px; display:flex; align-items:center; gap:18px;
  font-size:22px; font-weight:700; }
#s22 .flow .fchip{ border:1.5px solid rgba(22,103,214,.42); background:rgba(22,103,214,.07);
  padding:10px 24px; border-radius:999px; color:#12508F;
  transition:background .25s ease, color .25s ease, border-color .25s ease, box-shadow .25s ease; }
#s22 .flow .fchip.active{ background:#1667D6; color:#fff; border-color:#1667D6;
  box-shadow:0 8px 20px rgba(22,103,214,.38); }
#s22 .flow .arr{ color:#1667D6; font-size:26px; font-weight:800; }

/* 마지막 강조 문구 카드(자연어 검색 단계 전용) */
#s22 .final{ left:1300px; top:800px; width:544px; padding:24px 30px 26px; border-radius:20px;
  background:linear-gradient(135deg, rgba(22,103,214,.13), rgba(22,103,214,.02));
  border:1px solid rgba(22,103,214,.20); box-shadow:0 16px 38px rgba(22,103,214,.16); }
#s22 .final .flabel{ display:inline-flex; align-items:center; margin-bottom:13px;
  padding:5px 14px; border-radius:999px; background:#1667D6; color:#fff;
  font-size:13px; font-weight:800; letter-spacing:.5px; }
#s22 .final .ftext{ color:#173A66; font-size:23px; font-weight:800; line-height:1.48; letter-spacing:-.4px; }
#s22 .final .ftext b{ color:#1667D6; }

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

/* 클릭 유도 힌트 (마지막 단계에서는 숨김) */
#s22 .hint{
  position:absolute; right:70px; top:970px; color:#6B7C8A; font-size:16px; font-weight:700;
  display:flex; align-items:center; gap:6px; transition:opacity .3s ease; z-index:5;
}
#s22[data-step="3"] .hint{ opacity:0; pointer-events:none; }
#s22 .hint .arrow{ display:inline-block; animation:s22bounce 1.2s ease-in-out infinite; }
@keyframes s22bounce{ 0%,100%{ transform:translateX(0); } 50%{ transform:translateX(6px); } }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s22">
  <!-- 이 슬라이드의 최대 스텝 수(3)를 엔진에 알려주는 표시용 마커 -->
  <div class="frag" data-step="3" style="display:none"></div>

  <div class="abs hint">다음 화면으로 <span class="arrow">→</span></div>

  <!-- 호버 확대(공용): 팝업 + 번호 + 연결선 -->
  <div class="zoompop"><span class="zoomnum"></span></div>
  <svg class="abs zoomwire" viewBox="0 0 1920 1080" width="1920" height="1080" style="left:0;top:0;z-index:19;pointer-events:none">
    <path d="" fill="none" stroke="#1667D6" stroke-width="3" opacity=".75"/>
  </svg>

  <!-- ========================= STEP 0 : 회원 맞춤형 정책 추천 ========================= -->
  <div class="step step0">
    <div class="abs htitle">회원 맞춤형 정책 추천</div>
    <div class="abs hsub">회원 정보에 저장된 지역, 나이, 취업 상태를 기준으로<br>조건에 맞는 정책을 우선 추천합니다.</div>

    <div class="abs shotcard" style="left:70px; top:340px; width:1150px; height:419px;">
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
  </div>

  <!-- ========================= STEP 1 : 자연어 정책 검색 ========================= -->
  <div class="step step1">
    <div class="abs htitle">자연어 정책 검색</div>
    <div class="abs hsub">"수원에 사는 27살 취준생이 혜택 받을 정책 있을까?" 처럼<br>자연어로 물어보면 조건을 자동으로 추출해 검색합니다.</div>

    <div class="abs flow">
      <div class="fchip">자연어 질문</div>
      <div class="arr">&#8594;</div>
      <div class="fchip">조건 추출</div>
      <div class="arr">&#8594;</div>
      <div class="fchip">정책 검색 결과</div>
    </div>

    <div class="abs shotcard" style="left:70px; top:250px; width:1027px; height:668px;">
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
  </div>

  <!-- ========================= STEP 2 : 정책 상세 조회 ========================= -->
  <div class="step step2">
    <div class="abs htitle">정책 상세 조회</div>
    <div class="abs hsub">정책의 핵심 정보와 신청 경로를<br>한 화면에서 확인할 수 있습니다.</div>

    <div class="abs shotcard" style="left:260px; top:200px; width:786px; height:805px;">
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
  </div>

  <!-- ========================= STEP 3 : 즐겨찾기 정책 관리 ========================= -->
  <div class="step step3">
    <div class="abs htitle">즐겨찾기 정책 관리</div>
    <div class="abs hsub">관심 있는 정책을 저장하고,<br>마이페이지에서 다시 확인할 수 있습니다.</div>

    <div class="abs shotcard" style="left:70px; top:180px; width:1082px; height:748px;">
      <div class="img" style="background-image:url('assets/policy_scrap.png'); background-position:center top;"></div>
    </div>

    <div class="abs hl" style="left:94px; top:264px; width:124px; height:60px;"></div>
    <div class="abs hl" style="left:109px; top:368px; width:230px; height:84px;"></div>
    <div class="abs hl" style="left:110px; top:480px; width:69px; height:38px;"></div>
    <div class="abs hl" style="left:181px; top:480px; width:91px; height:38px;"></div>

    <div class="abs badge" style="left:78px; top:248px;">01</div>
    <div class="abs badge" style="left:93px; top:352px;">02</div>
    <div class="abs badge" style="left:94px; top:464px;">03</div>
    <div class="abs badge" style="left:165px; top:464px;">04</div>

    <div class="citem" style="top:280px;">
      <div class="cnum">01</div>
      <div><div class="ctitle">즐겨찾기 정책 모아보기</div>
        <div class="cdesc">관심 정책과 저장 개수를<br>한눈에 확인합니다.</div></div>
    </div>
    <div class="citem" style="top:440px;">
      <div class="cnum">02</div>
      <div><div class="ctitle">정책 상태 및 신청 기간 확인</div>
        <div class="cdesc">카드의 상태와 신청 기간을<br>바로 확인합니다.</div></div>
    </div>
    <div class="citem" style="top:600px;">
      <div class="cnum">03</div>
      <div><div class="ctitle">공식 링크 바로 이동</div>
        <div class="cdesc">저장한 정책의 원문으로<br>연결합니다.</div></div>
    </div>
    <div class="citem" style="top:760px;">
      <div class="cnum">04</div>
      <div><div class="ctitle">즐겨찾기 해제</div>
        <div class="cdesc">관심 대상에서 즉시 제거합니다.</div></div>
    </div>
  </div>
</section>
`);

/* ── 호버 확대: 4단계(step0~3) 모두 적용 ──
   강조박스(또는 설명)에 마우스 올리면 그 영역을 크게 팝업 + 번호 + 직선 연결선 + 소제목 형광펜 */
(function(){
  const sec = document.getElementById('s22');
  if(!sec) return;
  const pop = sec.querySelector('.zoompop');
  const zoomnum = pop.querySelector('.zoomnum');
  const zoomwire = sec.querySelector('.zoomwire');
  const zoompath = zoomwire.querySelector('path');
  const Z = 1.9;
  sec.querySelectorAll('.step').forEach(function(step){
    const card = step.querySelector('.shotcard');
    const imgEl = step.querySelector('.shotcard .img');
    if(!card || !imgEl) return;
    const bg = imgEl.style.backgroundImage;
    const CARD_L = card.offsetLeft, CARD_T = card.offsetTop, DISP_W = card.offsetWidth;
    const hls = Array.from(step.querySelectorAll('.hl'));
    const items = Array.from(step.querySelectorAll('.citem'));
    const chips = Array.from(step.querySelectorAll('.flow .fchip'));
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
  });
})();
