/* ===================== Slide 90 (내 기능 소개 · 소비가이드 → 고정지출 → 전체 소비내역 → 카테고리 상세, 클릭으로 전환) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s90{ background:transparent; }   /* 배경은 #deck에 고정된 공통 그라데이션을 그대로 사용 */

/* 4단계(step0~3)를 한 슬라이드 안에서 클릭으로 전환. 현재 data-step 값과 일치하는 것만 보임 */
#s90 .step{ position:absolute; inset:0; opacity:0; pointer-events:none; transition:opacity .5s ease; }
#s90[data-step="0"] .step0,
#s90[data-step="1"] .step1,
#s90[data-step="2"] .step2,
#s90[data-step="3"] .step3,
#s90[data-step="4"] .step4{ opacity:1; pointer-events:auto; }

#s90 .htitle{ left:70px; top:50px; width:1000px; color:#171717; font-size:40px; font-weight:800; }
#s90 .hsub{ left:70px; top:112px; width:1050px; color:#333; font-size:20px; font-weight:600; line-height:1.5; }

/* 스크린샷 카드 */
#s90 .shotcard{
  left:70px; top:230px; width:1150px; border-radius:18px;
  overflow:hidden; background:#fff; border:1px solid #E7ECE8;
  box-shadow:0 24px 55px rgba(0,0,0,.14);
}
#s90 .shotcard .img{
  width:100%; height:100%;
  background-repeat:no-repeat; background-size:100% auto;
}

/* 스크린샷 위 강조 박스 */
#s90 .hl{
  position:absolute; border:3px solid #007613; border-radius:12px;
  box-shadow:0 0 0 4px rgba(0,118,19,.12); cursor:pointer; transition:opacity .3s ease;
}
/* ── 호버 확대(zoom) ── 강조박스/설명에 마우스 올리면 그 영역을 원본 해상도로 크게 팝업 */
#s90.zooming .hl{ opacity:0; }
#s90.zooming .badge{ opacity:0; }
#s90 .zoompop{ position:absolute; opacity:0; pointer-events:none; z-index:20;
  background-repeat:no-repeat; border:3px solid #007613; border-radius:12px;
  box-shadow:0 22px 55px rgba(0,0,0,.35); transition:opacity .2s ease; }
#s90 .zoompop.show{ opacity:1; }
#s90 .zoomnum{ position:absolute; top:-18px; left:-18px;
  width:46px; height:46px; border-radius:50%; background:#007613; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:18px; font-weight:800;
  box-shadow:0 8px 18px rgba(0,118,19,.4); }
#s90 .zoomwire{ opacity:0; transition:opacity .2s ease; }
#s90 .zoomwire.show{ opacity:1; }

/* 스크린샷 위 번호 배지 */
#s90 .badge{
  position:absolute; width:40px; height:40px; border-radius:50%;
  background:#007613; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:17px; font-weight:800; box-shadow:0 8px 18px rgba(0,118,19,.35); z-index:4;
  transition:opacity .3s ease;
}

/* 우측 설명 리스트 */
#s90 .citem{
  position:absolute; left:1300px; width:560px; display:flex; align-items:flex-start; gap:18px;
  transform-origin:left center; cursor:pointer; transition:transform .2s ease;
}
#s90 .citem.big{ transform:scale(1.5); z-index:6; }
#s90 .cnum{
  width:44px; height:44px; border-radius:50%; flex:none;
  background:#007613; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800; box-shadow:0 8px 18px rgba(0,118,19,.3);
}
#s90 .ctitle{ color:#171717; font-size:23px; font-weight:800; margin-bottom:7px; width:fit-content; padding:0 4px; }
#s90 .cdesc{ color:#555; font-size:17px; font-weight:500; line-height:1.5; }
/* 호버한 항목의 소제목에만 노란 형광펜 */
#s90 .citem.big .ctitle{ background:linear-gradient(transparent 55%, rgba(255,224,20,.65) 55%); }

/* 클릭 유도 힌트 (마지막 단계에서는 숨김) */
#s90 .hint{
  position:absolute; right:70px; top:970px; color:#6B7C8A; font-size:16px; font-weight:700;
  display:flex; align-items:center; gap:6px; transition:opacity .3s ease; z-index:5;
}
#s90[data-step="4"] .hint{ opacity:0; pointer-events:none; }
#s90 .hint .arrow{ display:inline-block; animation:s90bounce 1.2s ease-in-out infinite; }
@keyframes s90bounce{ 0%,100%{ transform:translateX(0); } 50%{ transform:translateX(6px); } }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s90">
  <!-- 이 슬라이드의 최대 스텝 수(4)를 엔진에 알려주는 표시용 마커 -->
  <div class="frag" data-step="4" style="display:none"></div>

  <div class="abs hint">다음 화면으로 <span class="arrow">→</span></div>

  <!-- 호버 확대(공용): 팝업 + 번호 + 연결선 -->
  <div class="zoompop"><span class="zoomnum"></span></div>
  <svg class="abs zoomwire" viewBox="0 0 1920 1080" width="1920" height="1080" style="left:0;top:0;z-index:19;pointer-events:none">
    <path d="" fill="none" stroke="#007613" stroke-width="3" opacity=".75"/>
  </svg>

  <!-- ========================= STEP 0 : 소비가이드 (상단) ========================= -->
  <div class="step step0">
    <div class="abs htitle">오늘의 소비 기준을 한눈에</div>
    <div class="abs hsub">회원의 급여 주기와 남은 예산을 기준으로<br>오늘 써도 되는 금액을 자동으로 계산합니다.</div>

    <div class="abs shotcard" style="height:720px;">
      <div class="img" style="background-image:url('assets/s90_spending_guide.png'); background-position:center top;"></div>
    </div>

    <div class="abs hl" style="left:167px; top:429px; width:683px; height:203px;"></div>
    <div class="abs hl" style="left:857px; top:429px; width:277px; height:203px;"></div>
    <div class="abs hl" style="left:167px; top:665px; width:557px; height:118px;"></div>
    <div class="abs hl" style="left:738px; top:665px; width:395px; height:259px;"></div>

    <div class="abs badge" style="left:151px; top:413px;">01</div>
    <div class="abs badge" style="left:841px; top:413px;">02</div>
    <div class="abs badge" style="left:151px; top:649px;">03</div>
    <div class="abs badge" style="left:722px; top:649px;">04</div>

    <div class="citem" style="top:330px;">
      <div class="cnum">01</div>
      <div>
        <div class="ctitle">오늘 사용 가능 금액</div>
        <div class="cdesc">하루 권장 소비액과 순사용액을 반영해<br>자동으로 계산합니다.</div>
      </div>
    </div>
    <div class="citem" style="top:465px;">
      <div class="cnum">02</div>
      <div>
        <div class="ctitle">이번 급여 주기 예산</div>
        <div class="cdesc">급여일 기준 주기 동안 남은 예산을<br>실시간으로 보여줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:600px;">
      <div class="cnum">03</div>
      <div>
        <div class="ctitle">잉여금 누적</div>
        <div class="cdesc">권장액보다 덜 쓴 금액을 급여 주기마다<br>자동으로 쌓아줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:735px;">
      <div class="cnum">04</div>
      <div>
        <div class="ctitle">카테고리별 소비</div>
        <div class="cdesc">실제 소비 내역을 카테고리로 나눠<br>시각화해서 보여줍니다.</div>
      </div>
    </div>
  </div>

  <!-- ========================= STEP 1 : 소비가이드 (하단) ========================= -->
  <div class="step step1">
    <div class="abs htitle">오늘 거래와 소비 흐름까지</div>
    <div class="abs hsub">카드내역을 자동으로 모아 정리하고,<br>최근 소비 흐름과 절약 포인트까지 알려줍니다.</div>

    <div class="abs shotcard" style="height:720px;">
      <div class="img" style="background-image:url('assets/s90_spending_guide.png'); background-position:center bottom;"></div>
    </div>

    <div class="abs hl" style="left:167px; top:230px; width:557px; height:324px;"></div>
    <div class="abs hl" style="left:738px; top:475px; width:395px; height:216px;"></div>
    <div class="abs hl" style="left:167px; top:568px; width:557px; height:345px;"></div>

    <div class="abs badge" style="left:151px; top:214px;">01</div>
    <div class="abs badge" style="left:722px; top:459px;">02</div>
    <div class="abs badge" style="left:151px; top:552px;">03</div>

    <div class="citem" style="top:350px;">
      <div class="cnum">01</div>
      <div>
        <div class="ctitle">오늘 거래 자동 수집</div>
        <div class="cdesc">카드내역을 자동으로 동기화하고,<br>직접 입력도 바로 할 수 있습니다.</div>
      </div>
    </div>
    <div class="citem" style="top:520px;">
      <div class="cnum">02</div>
      <div>
        <div class="ctitle">이번 달 이렇게 아껴봐요</div>
        <div class="cdesc">지난 소비 습관을 바탕으로<br>절약할 수 있는 부분을 알려줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:690px;">
      <div class="cnum">03</div>
      <div>
        <div class="ctitle">최근 7일 소비 흐름</div>
        <div class="cdesc">날짜별 순사용액과 하루 권장액을<br>그래프로 비교해서 보여줍니다.</div>
      </div>
    </div>
  </div>

  <!-- ========================= STEP 2 : 고정지출 ========================= -->
  <div class="step step2">
    <div class="abs htitle">매달 반복되는 지출을 한곳에서</div>
    <div class="abs hsub">카드내역에서 반복 결제를 자동으로 찾아내고<br>결제일이 가까운 순서로 정리해 보여줍니다.</div>

    <div class="abs shotcard" style="height:820px;">
      <div class="img" style="background-image:url('assets/s90_fixed_expense.png'); background-position:center top;"></div>
    </div>

    <div class="abs hl" style="left:167px; top:367px; width:967px; height:165px;"></div>
    <div class="abs hl" style="left:167px; top:562px; width:661px; height:135px;"></div>
    <div class="abs hl" style="left:846px; top:562px; width:288px; height:193px;"></div>
    <div class="abs hl" style="left:167px; top:722px; width:661px; height:255px;"></div>
    <div class="abs hl" style="left:846px; top:845px; width:288px; height:201px;"></div>

    <div class="abs badge" style="left:151px; top:351px;">01</div>
    <div class="abs badge" style="left:151px; top:546px;">02</div>
    <div class="abs badge" style="left:818px; top:530px;">03</div>
    <div class="abs badge" style="left:151px; top:706px;">04</div>
    <div class="abs badge" style="left:818px; top:813px;">05</div>

    <div class="citem" style="top:300px;">
      <div class="cnum">01</div>
      <div>
        <div class="ctitle">고정지출 자동 합산</div>
        <div class="cdesc">등록된 항목을 합산해 급여 대비<br>비중까지 계산합니다.</div>
      </div>
    </div>
    <div class="citem" style="top:430px;">
      <div class="cnum">02</div>
      <div>
        <div class="ctitle">반복 결제 자동 감지</div>
        <div class="cdesc">카드내역에서 매달 반복되는 결제를<br>찾아 등록을 제안합니다.</div>
      </div>
    </div>
    <div class="citem" style="top:560px;">
      <div class="cnum">03</div>
      <div>
        <div class="ctitle">다가오는 결제 미리 확인</div>
        <div class="cdesc">다음 결제 예정일을 리스트로<br>미리 보여줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:690px;">
      <div class="cnum">04</div>
      <div>
        <div class="ctitle">결제일 가까운 순 정렬</div>
        <div class="cdesc">카드 결제·계좌이체를 구분해<br>결제 상태를 표시합니다.</div>
      </div>
    </div>
    <div class="citem" style="top:820px;">
      <div class="cnum">05</div>
      <div>
        <div class="ctitle">연간으로 환산해 보기</div>
        <div class="cdesc">한 달 금액이 1년으로 보면<br>얼마인지 바로 알려줍니다.</div>
      </div>
    </div>
  </div>

  <!-- ========================= STEP 3 : 전체 소비내역 (transactions) ========================= -->
  <div class="step step3">
    <div class="abs htitle">소비 흐름을 그래프와 순위로</div>
    <div class="abs hsub">급여 주기 전체의 결제 흐름과 자주 이용한 곳을<br>한 번에 확인할 수 있습니다.</div>

    <div class="abs shotcard" style="left:180px; top:175px; width:760px; height:900px;">
      <div class="img" style="background-image:url('assets/s90_transactions.png'); background-position:center top;"></div>
    </div>

    <div class="abs hl" style="left:643px; top:320px; width:192px; height:225px;"></div>
    <div class="abs hl" style="left:288px; top:406px; width:344px; height:139px;"></div>
    <div class="abs hl" style="left:288px; top:562px; width:372px; height:174px;"></div>

    <div class="abs badge" style="left:611px; top:288px;">01</div>
    <div class="abs badge" style="left:256px; top:374px;">02</div>
    <div class="abs badge" style="left:256px; top:530px;">03</div>

    <div class="citem" style="top:350px;">
      <div class="cnum">01</div>
      <div>
        <div class="ctitle">많이 쓴 곳 TOP 5</div>
        <div class="cdesc">이번 주기에 가장 많이 쓴 가맹점을<br>순위로 보여줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:520px;">
      <div class="cnum">02</div>
      <div>
        <div class="ctitle">일별 소비 추이</div>
        <div class="cdesc">급여 주기 전체의 날짜별 소비 흐름을<br>그래프로 확인할 수 있습니다.</div>
      </div>
    </div>
    <div class="citem" style="top:690px;">
      <div class="cnum">03</div>
      <div>
        <div class="ctitle">결제내역 전체 목록</div>
        <div class="cdesc">날짜별로 묶어서 최신순으로,<br>페이지를 넘겨가며 확인합니다.</div>
      </div>
    </div>
  </div>

  <!-- ========================= STEP 4 : 카테고리 소비 상세 (category-detail) ========================= -->
  <div class="step step4">
    <div class="abs htitle">카테고리 하나만 파고들기</div>
    <div class="abs hsub">카테고리를 선택하면 지난 주기 대비 변화부터<br>결제내역, 소비 시점까지 한 화면에서 보여줍니다.</div>

    <div class="abs shotcard" style="left:180px; top:245px; width:760px; height:750px;">
      <div class="img" style="background-image:url('assets/s90_category_detail.png'); background-position:center top;"></div>
    </div>

    <div class="abs hl" style="left:262px; top:340px; width:314px; height:237px;"></div>
    <div class="abs hl" style="left:585px; top:340px; width:266px; height:237px;"></div>
    <div class="abs hl" style="left:262px; top:587px; width:589px; height:26px;"></div>
    <div class="abs hl" style="left:262px; top:621px; width:392px; height:186px;"></div>
    <div class="abs hl" style="left:662px; top:621px; width:188px; height:186px;"></div>

    <div class="abs badge" style="left:246px; top:320px;">01</div>
    <div class="abs badge" style="left:569px; top:320px;">02</div>
    <div class="abs badge" style="left:236px; top:586px;">03</div>
    <div class="abs badge" style="left:246px; top:671px;">04</div>
    <div class="abs badge" style="left:643px; top:592px;">05</div>

    <div class="citem" style="top:240px;">
      <div class="cnum">01</div>
      <div>
        <div class="ctitle">카테고리별 지난 주기 대비 변화</div>
        <div class="cdesc">이번 주기 소비 규모를 이전 주기와 비교해<br>증감액까지 함께 보여줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:372px;">
      <div class="cnum">02</div>
      <div>
        <div class="ctitle">카테고리별 결제내역</div>
        <div class="cdesc">선택한 카테고리의 결제 건을 날짜별로<br>모아서 보여줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:502px;">
      <div class="cnum">03</div>
      <div>
        <div class="ctitle">카테고리 전환</div>
        <div class="cdesc">탭 하나로 다른 카테고리의 상세 분석으로<br>바로 넘어갑니다.</div>
      </div>
    </div>
    <div class="citem" style="top:632px;">
      <div class="cnum">04</div>
      <div>
        <div class="ctitle">급여주기별 소비 흐름</div>
        <div class="cdesc">선택한 카테고리가 급여주기마다 어떻게<br>변해왔는지 그래프로 보여줍니다.</div>
      </div>
    </div>
    <div class="citem" style="top:762px;">
      <div class="cnum">05</div>
      <div>
        <div class="ctitle">급여주기 내 소비 시점</div>
        <div class="cdesc">초반·중반·후반, 평일·주말 중 언제<br>많이 썼는지 비교해서 보여줍니다.</div>
      </div>
    </div>
  </div>
</section>
`);

/* ── 호버 확대: 5단계(step0~4) 모두 적용 ──
   강조박스(또는 설명)에 마우스 올리면 그 영역을 원본 해상도로 크게 팝업 + 번호 + 직선 연결선 + 소제목 노란 형광펜 */
(function(){
  const sec = document.getElementById('s90');
  if(!sec) return;
  const pop = sec.querySelector('.zoompop');
  const zoomnum = pop.querySelector('.zoomnum');
  const zoomwire = sec.querySelector('.zoomwire');
  const zoompath = zoomwire.querySelector('path');
  const NATW = 1600, Z = 1.55;
  // 스크린샷 원본 세로 픽셀(파일명으로 구분) — center bottom 보정 계산에 사용
  const NATH = { spending_guide:1821, fixed_expense:1821, transactions:2004, category_detail:1737 };
  sec.querySelectorAll('.step').forEach(function(step){
    const card  = step.querySelector('.shotcard');
    const imgEl = step.querySelector('.shotcard .img');
    if(!card || !imgEl) return;
    const bg = imgEl.style.backgroundImage;
    // 카드(스크린샷) 실제 배치를 스텝마다 읽어옴 — 스텝별로 크기/위치가 달라도 정확
    const CARD_L = card.offsetLeft, CARD_T = card.offsetTop;
    const DISP_W = card.offsetWidth, CARD_H = card.offsetHeight;
    let natH = 1821;
    Object.keys(NATH).forEach(function(k){ if(bg.indexOf(k) >= 0) natH = NATH[k]; });
    const imgDispH = natH * (DISP_W / NATW);
    // center bottom(아래로 스크롤된 화면)은 숨은 위쪽만큼 보정
    const bottom = /bottom/.test(imgEl.style.backgroundPosition || '');
    const topHidden = bottom ? (imgDispH - CARD_H) : 0;
    const hls = Array.from(step.querySelectorAll('.hl'));
    const items = Array.from(step.querySelectorAll('.citem'));
    hls.forEach(function(hl, i){
      const item = items[i];
      const on = function(){
        const rx = hl.offsetLeft - CARD_L;
        const ry = hl.offsetTop  - CARD_T;
        const rw = hl.offsetWidth, rh = hl.offsetHeight;
        const iy = ry + topHidden;
        const z = Math.min(Z, 1200/rw, 800/rh);   // 폭 넓으면 배율 축소(오른쪽 설명 안 가림)
        const popW = rw*z, popH = rh*z;
        let popL = hl.offsetLeft - rw*(z-1)/2;
        let popT = hl.offsetTop  - rh*(z-1)/2;
        popL = Math.max(20, Math.min(popL, 1920 - popW - 20));
        popT = Math.max(20, Math.min(popT, 1080 - popH - 20));
        pop.style.left = popL+'px'; pop.style.top = popT+'px';
        pop.style.width = popW+'px'; pop.style.height = popH+'px';
        pop.style.backgroundImage    = bg;
        pop.style.backgroundSize     = (DISP_W*z)+'px auto';
        pop.style.backgroundPosition = (-rx*z)+'px '+(-iy*z)+'px';
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
