/* ===================== Slide 24 ===================== */
/*
policy_detail.png original: 458 x 469
capture-wrap: left 90, top 225, width 850, height 805
object-fit contain, object-position center top
scale: 1.716418
rendered image: 786.12 x 805
image content offset: left 121.94, top 225
*/
document.head.insertAdjacentHTML('beforeend', `
<style>
#s24{
  background:transparent;
}
#s24 .ptitle{
  left:52px; top:40px; width:760px;
  color:#6B7C8A; font-size:45px; font-weight:800; letter-spacing:-1px;
}
#s24 .lead{
  left:92px; top:128px; width:900px;
  color:#111; font-size:38px; font-weight:700; line-height:1.32; letter-spacing:-1.1px;
}
#s24 .capture-wrap{
  left:90px; top:225px; width:850px; height:805px;
  background:#fff; border:1px solid rgba(28,92,58,.18);
  box-shadow:0 12px 30px rgba(34,70,48,.12);
  overflow:hidden;
}
#s24 .capture{
  width:100%; height:100%; object-fit:contain; object-position:center top; display:block;
}
#s24.entering .capture-wrap{
  animation:s24CaptureIn .55s ease both;
}
#s24 .items{
  left:1060px; top:236px; width:720px;
}
#s24 .item{
  position:relative; margin-bottom:26px; padding-left:64px;
  color:#202522; font-size:25px; font-weight:700; line-height:1.32; letter-spacing:-.6px;
  transform:translateY(18px);
}
#s24 .item.frag.show{
  transform:translateY(0);
}
#s24 .item4{
  margin-top:8px;
}
#s24 .num{
  position:absolute; left:0; top:0; width:42px; height:42px;
  border:2px solid #00A651; color:#007613; border-radius:50%;
  display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800;
}
#s24 .sub{
  display:block; margin-top:7px;
  color:#5B6760; font-size:18px; font-weight:500; line-height:1.35;
}
#s24 .mark{
  position:absolute; z-index:4; pointer-events:none;
  border:2px solid rgba(0,166,81,.86);
  background:rgba(0,166,81,.035);
  opacity:0; transform:scale(.99);
  transition:opacity .45s ease, transform .45s ease;
}
#s24 .mark.frag.show{
  opacity:1; transform:scale(1);
}
#s24 .mark-num{
  position:absolute; left:-13px; top:-13px; width:30px; height:30px;
  border-radius:50%; background:#00A651; color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-size:14px; font-weight:800;
}
#s24 .m1{ left:153px; top:345px; width:726px; height:115px; }
#s24 .m2{ left:154px; top:480px; width:224px; height:212px; }
#s24 .m3{ left:153px; top:704px; width:726px; height:55px; }
#s24 .m4{ left:153px; top:772px; width:186px; height:71px; }
#s24 .m5{ left:346px; top:772px; width:188px; height:71px; }
@keyframes s24CaptureIn{
  0%{ opacity:0; transform:translateY(20px); }
  100%{ opacity:1; transform:translateY(0); }
}
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s24">
  <div class="abs ptitle">정책 상세 조회</div>
  <div class="abs lead">정책의 핵심 정보와 신청 경로를<br>한 화면에서 확인할 수 있습니다.</div>

  <div class="abs capture-wrap">
    <img class="capture" src="assets/policy_detail.png" alt="정책 상세 조회 화면">
  </div>

  <div class="abs mark m1 frag" data-step="1"><div class="mark-num">01</div></div>
  <div class="abs mark m2 frag" data-step="2"><div class="mark-num">02</div></div>
  <div class="abs mark m3 frag" data-step="3"><div class="mark-num">03</div></div>
  <div class="abs mark m4 frag" data-step="4"><div class="mark-num">04</div></div>
  <div class="abs mark m5 frag" data-step="5"><div class="mark-num">05</div></div>

  <div class="abs items">
    <div class="item frag" data-step="1"><span class="num">01</span>정책 기본 정보<span class="sub">정책 식별값과 정책명을 한 화면에서 확인</span></div>
    <div class="item frag" data-step="2"><span class="num">02</span>정책 분류 및 운영 정보<span class="sub">기관, 분야, 상태, 지역 정보 표시</span></div>
    <div class="item frag" data-step="3"><span class="num">03</span>정책 요약<span class="sub">정책의 핵심 내용을 짧게 확인</span></div>
    <div class="item item4 frag" data-step="4"><span class="num">04</span>즐겨찾기 추가<span class="sub">관심 정책으로 저장</span></div>
    <div class="item frag" data-step="5"><span class="num">05</span>공식 링크<span class="sub">공식 안내 페이지로 이동</span></div>
  </div>
</section>
`);
