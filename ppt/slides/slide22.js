/* ===================== Slide 22 ===================== */
/*
policy_recommend.png original: 1204 x 439
capture-wrap: left 80, top 254, width 1215, height 735
object-fit contain scale: 1.009136
rendered image: 1215 x 443
image content offset: left 80, top 400
*/
document.head.insertAdjacentHTML('beforeend', `
<style>
#s22{
  background:transparent;
}
#s22 .ptitle{
  left:52px; top:40px; width:760px;
  color:#6B7C8A; font-size:45px; font-weight:800; letter-spacing:-1px;
}
#s22 .lead{
  left:92px; top:128px; width:850px;
  color:#111; font-size:38px; font-weight:700; line-height:1.32; letter-spacing:-1.2px;
}
#s22 .capture-wrap{
  left:80px; top:254px; width:1215px; height:735px;
  background:#fff; border:1px solid rgba(28,92,58,.18);
  box-shadow:0 12px 30px rgba(34,70,48,.12);
  overflow:hidden;
}
#s22 .capture{
  width:100%; height:100%; object-fit:contain; display:block;
}
#s22.entering .capture-wrap{
  animation:s22CaptureIn .55s ease both;
}
#s22 .note-list{
  left:1358px; top:252px; width:430px;
}
#s22 .note{
  position:relative; margin-bottom:34px; padding-left:64px;
  color:#202522; font-size:26px; font-weight:700; line-height:1.34; letter-spacing:-.6px;
  transform:translateY(18px);
}
#s22 .note.frag.show{
  transform:translateY(0);
}
#s22 .num{
  position:absolute; left:0; top:2px; width:42px; height:42px;
  border:2px solid #00A651; color:#007613; border-radius:50%;
  display:flex; align-items:center; justify-content:center;
  font-size:18px; font-weight:800;
}
#s22 .small{
  display:block; margin-top:8px;
  color:#5B6760; font-size:18px; font-weight:500; line-height:1.35;
}
#s22 .mark{
  position:absolute; z-index:4; pointer-events:none;
  border:2px solid rgba(0,166,81,.86);
  background:rgba(0,166,81,.035);
  opacity:0; transform:scale(.99);
  transition:opacity .45s ease, transform .45s ease;
}
#s22 .mark.frag.show{
  opacity:1; transform:scale(1);
}
#s22 .mark-num{
  position:absolute; left:-13px; top:-13px; width:30px; height:30px;
  border-radius:50%; background:#00A651; color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-size:14px; font-weight:800;
}
#s22 .m1{ left:93px; top:479px; width:232px; height:38px; }
#s22 .m2{ left:120px; top:718px; width:492px; height:47px; }
#s22 .m3{ left:326px; top:479px; width:102px; height:38px; }
#s22 .m4{ left:592px; top:782px; width:78px; height:42px; }
@keyframes s22CaptureIn{
  0%{ opacity:0; transform:translateY(20px); }
  100%{ opacity:1; transform:translateY(0); }
}
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s22">
  <div class="abs ptitle">회원 맞춤형 정책 추천</div>
  <div class="abs lead">회원 정보에 저장된 지역, 나이, 취업 상태를 기준으로<br>조건에 맞는 정책을 우선 추천합니다.</div>

  <div class="abs capture-wrap">
    <img class="capture" src="assets/policy_recommend.png" alt="회원 맞춤형 정책 추천 화면">
  </div>

  <div class="abs mark m1 frag" data-step="1"><div class="mark-num">01</div></div>
  <div class="abs mark m2 frag" data-step="2"><div class="mark-num">02</div></div>
  <div class="abs mark m3 frag" data-step="3"><div class="mark-num">03</div></div>
  <div class="abs mark m4 frag" data-step="4"><div class="mark-num">04</div></div>

  <div class="abs note-list">
    <div class="note frag" data-step="1"><span class="num">01</span>회원 정보 기반 자동 추천<span class="small">지역, 나이, 취업 상태를 추천 기준으로 사용</span></div>
    <div class="note frag" data-step="2"><span class="num">02</span>조건별 추천 이유 제공<span class="small">왜 이 정책이 맞는지 화면에서 바로 확인</span></div>
    <div class="note frag" data-step="3"><span class="num">03</span>추천 조건 직접 수정<span class="small">필요하면 조건을 바꿔 다시 탐색</span></div>
    <div class="note frag" data-step="4"><span class="num">04</span>정책 상세 조회<span class="small">추천 카드에서 상세 화면으로 이동</span></div>
  </div>
</section>
`);
