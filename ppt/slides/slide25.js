/* ===================== Slide 25 ===================== */
/*
policy_scrap.png original: 1189 x 822
capture-wrap: left 82, top 244, width 1326, height 748
object-fit contain, object-position center top
scale: 0.909976
rendered image: 1081.96 x 748
image content offset: left 204.02, top 244
*/
document.head.insertAdjacentHTML('beforeend', `
<style>
#s25{
  background:transparent;
}
#s25 .ptitle{
  left:52px; top:40px; width:760px;
  color:#6B7C8A; font-size:45px; font-weight:800; letter-spacing:-1px;
}
#s25 .lead{
  left:92px; top:128px; width:860px;
  color:#111; font-size:38px; font-weight:700; line-height:1.32; letter-spacing:-1.1px;
}
#s25 .capture-wrap{
  left:82px; top:244px; width:1326px; height:748px;
  background:#fff; border:1px solid rgba(28,92,58,.18);
  box-shadow:0 12px 30px rgba(34,70,48,.12);
  overflow:hidden;
}
#s25 .capture{
  width:100%; height:100%; object-fit:contain; object-position:center top; display:block;
}
#s25.entering .capture-wrap{
  animation:s25CaptureIn .55s ease both;
}
#s25 .points{
  left:1466px; top:270px; width:350px;
}
#s25 .point{
  position:relative; margin-bottom:30px; padding-left:64px;
  color:#202522; font-size:25px; font-weight:700; line-height:1.34; letter-spacing:-.6px;
  transform:translateY(18px);
}
#s25 .point.frag.show{
  transform:translateY(0);
}
#s25 .num{
  position:absolute; left:0; top:0; width:42px; height:42px;
  border:2px solid #00A651; color:#007613; border-radius:50%;
  display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800;
}
#s25 .sub{
  display:block; margin-top:7px;
  color:#5B6760; font-size:18px; font-weight:500; line-height:1.35;
}
#s25 .final{
  left:1466px; top:806px; width:350px;
  color:#111; font-size:29px; font-weight:800; line-height:1.36; letter-spacing:-.8px;
  border-left:5px solid #00A651; padding-left:22px;
  transform:translateY(18px);
}
#s25 .final.frag.show{
  transform:translateY(0);
}
#s25 .mark{
  position:absolute; z-index:4; pointer-events:none;
  border:2px solid rgba(0,166,81,.86);
  background:rgba(0,166,81,.035);
  opacity:0; transform:scale(.99);
  transition:opacity .45s ease, transform .45s ease;
}
#s25 .mark.frag.show{
  opacity:1; transform:scale(1);
}
#s25 .mark-num{
  position:absolute; left:-13px; top:-13px; width:30px; height:30px;
  border-radius:50%; background:#00A651; color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-size:14px; font-weight:800;
}
#s25 .m1{ left:228px; top:328px; width:124px; height:60px; }
#s25 .m2{ left:243px; top:432px; width:230px; height:84px; }
#s25 .m3{ left:244px; top:544px; width:69px; height:38px; }
#s25 .m4{ left:315px; top:544px; width:91px; height:38px; }
@keyframes s25CaptureIn{
  0%{ opacity:0; transform:translateY(20px); }
  100%{ opacity:1; transform:translateY(0); }
}
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s25">
  <div class="abs ptitle">즐겨찾기 정책 관리</div>
  <div class="abs lead">관심 있는 정책을 저장하고,<br>마이페이지에서 다시 확인할 수 있습니다.</div>

  <div class="abs capture-wrap">
    <img class="capture" src="assets/policy_scrap.png" alt="즐겨찾기 정책 관리 화면">
  </div>

  <div class="abs mark m1 frag" data-step="1"><div class="mark-num">01</div></div>
  <div class="abs mark m2 frag" data-step="2"><div class="mark-num">02</div></div>
  <div class="abs mark m3 frag" data-step="3"><div class="mark-num">03</div></div>
  <div class="abs mark m4 frag" data-step="4"><div class="mark-num">04</div></div>

  <div class="abs points">
    <div class="point frag" data-step="1"><span class="num">01</span>즐겨찾기 정책 모아보기<span class="sub">관심 정책과 저장 개수를 확인</span></div>
    <div class="point frag" data-step="2"><span class="num">02</span>정책 상태 및 신청 기간 확인<span class="sub">첫 번째 카드의 상태와 기간을 바로 확인</span></div>
    <div class="point frag" data-step="3"><span class="num">03</span>공식 링크 바로 이동<span class="sub">저장한 정책의 원문으로 연결</span></div>
    <div class="point frag" data-step="4"><span class="num">04</span>즐겨찾기 해제<span class="sub">관심 대상에서 즉시 제거</span></div>
  </div>
  <div class="abs final frag" data-step="4">검색에서 끝나지 않고,<br>관심 정책을 저장해 다시 활용할 수 있습니다.</div>
</section>
`);
