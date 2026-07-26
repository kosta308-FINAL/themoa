/* ===================== Slide 23 ===================== */
/*
policy_search_result.png original: 1099 x 715
capture-wrap: left 92, top 326, width 1338, height 668
object-fit contain scale: 0.934266
rendered image: 1026.76 x 668
image content offset: left 247.62, top 326
*/
document.head.insertAdjacentHTML('beforeend', `
<style>
#s23{
  background:transparent;
}
#s23 .ptitle{
  left:52px; top:40px; width:760px;
  color:#6B7C8A; font-size:45px; font-weight:800; letter-spacing:-1px;
}
#s23 .question{
  left:92px; top:126px; width:1280px;
  color:#111; font-size:38px; font-weight:800; line-height:1.28; letter-spacing:-1.2px;
  transform:translateY(18px);
}
#s23 .question.frag.show{
  transform:translateY(0);
}
#s23 .flow{
  left:97px; top:230px; width:1040px; height:58px;
  display:flex; align-items:center; gap:22px;
  color:#2C3E50; font-size:24px; font-weight:700;
}
#s23 .flow-item{
  border:1px solid rgba(0,118,19,.28); background:rgba(255,255,255,.62);
  padding:13px 24px; border-radius:999px;
}
#s23 .arrow{
  color:#00A651; font-size:30px; font-weight:800;
}
#s23 .capture-wrap{
  left:92px; top:326px; width:1338px; height:668px;
  background:#fff; border:1px solid rgba(28,92,58,.18);
  box-shadow:0 12px 30px rgba(34,70,48,.12);
  overflow:hidden;
}
#s23 .capture{
  width:100%; height:100%; object-fit:contain; display:block;
}
#s23.entering .capture-wrap{
  animation:s23CaptureIn .55s ease both;
}
#s23 .labels{
  left:1484px; top:356px; width:340px;
  color:#202522;
}
#s23 .label{
  position:relative; margin-bottom:44px; padding-left:64px;
  font-size:25px; font-weight:700; line-height:1.35; letter-spacing:-.5px;
  transform:translateX(20px);
}
#s23 .label.frag.show{
  transform:translateX(0);
}
#s23 .label .num{
  position:absolute; left:0; top:0; width:42px; height:42px;
  border:2px solid #00A651; color:#007613; border-radius:50%;
  display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800;
}
#s23 .label .sub{
  display:block; margin-top:7px;
  color:#5B6760; font-size:18px; font-weight:500; line-height:1.35;
}
#s23 .final{
  left:1484px; top:742px; width:350px;
  color:#111; font-size:29px; font-weight:800; line-height:1.36; letter-spacing:-.8px;
  border-left:5px solid #00A651; padding-left:22px;
  transform:translateY(18px);
}
#s23 .final.frag.show{
  transform:translateY(0);
}
#s23 .mark{
  position:absolute; z-index:4; pointer-events:none;
  border:2px solid rgba(0,166,81,.86);
  background:rgba(0,166,81,.035);
  opacity:0; transform:scale(.99);
  transition:opacity .45s ease, transform .45s ease;
}
#s23 .mark.frag.show{
  opacity:1; transform:scale(1);
}
#s23 .mark-num{
  position:absolute; left:-13px; top:-13px; width:30px; height:30px;
  border-radius:50%; background:#00A651; color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-size:14px; font-weight:800;
}
#s23 .m1{ left:260px; top:341px; width:1005px; height:101px; }
#s23 .m2{ left:257px; top:590px; width:407px; height:42px; }
#s23 .m3{ left:247px; top:659px; width:1030px; height:166px; }
@keyframes s23CaptureIn{
  0%{ opacity:0; transform:translateY(20px); }
  100%{ opacity:1; transform:translateY(0); }
}
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s23">
  <div class="abs ptitle">자연어 정책 검색</div>
  <div class="abs question frag" data-step="1">수원에 사는 27살 취준생이 혜택 받을 수 있는 정책이 있을까?</div>

  <div class="abs flow">
    <div class="flow-item">자연어 질문</div>
    <div class="arrow">→</div>
    <div class="flow-item">조건 추출</div>
    <div class="arrow">→</div>
    <div class="flow-item">정책 검색 결과</div>
  </div>

  <div class="abs capture-wrap">
    <img class="capture" src="assets/policy_search_result.png" alt="자연어 정책 검색 결과 화면">
  </div>

  <div class="abs mark m1 frag" data-step="1"><div class="mark-num">01</div></div>
  <div class="abs mark m2 frag" data-step="2"><div class="mark-num">02</div></div>
  <div class="abs mark m3 frag" data-step="3"><div class="mark-num">03</div></div>

  <div class="abs labels">
    <div class="label frag" data-step="1"><span class="num">01</span>자연어 질문 입력<span class="sub">사용자가 입력한 문장을 그대로 검색</span></div>
    <div class="label frag" data-step="2"><span class="num">02</span>검색 조건 자동 추출<span class="sub">지역, 나이, 취업 상태, 검색 모드 구조화</span></div>
    <div class="label frag" data-step="3"><span class="num">03</span>정책 검색 결과 제공<span class="sub">조건에 맞는 정책 카드를 우선 표시</span></div>
  </div>
  <div class="abs final frag" data-step="3">자연어로 입력해도<br>검색에 필요한 조건을 자동으로 구조화합니다.</div>
</section>
`);
