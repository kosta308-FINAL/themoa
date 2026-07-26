/* ===================== Slide 93 (팀 소개) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s93{
  background:transparent;   /* 배경은 #deck에 고정된 공통 그라데이션을 그대로 사용 */
  --ink:#16241C; --muted:#5E6B62; --line:#E1E8E2;
  --accent:#0B7A3D; --accent-soft:#E4F1E7; --gold:#B8892E; --gold-soft:#F5EEDE;
}
#s93 .ptitle{ left:70px; top:54px; width:1200px; color:var(--ink); font-size:44px; font-weight:800; }
#s93 .psub{ left:70px; top:124px; width:1500px; color:var(--muted); font-size:20px; font-weight:500; }

/* 공통 작업 바 */
#s93 .common{
  left:70px; top:172px; width:1780px; height:60px; border-radius:14px;
  background:#fff; display:flex; align-items:center; gap:16px; padding:0 26px;
  box-shadow:0 10px 26px rgba(20,40,25,.08);
}
#s93 .common .ctag{
  background:var(--accent-soft); color:var(--accent); font-size:15px; font-weight:800;
  padding:7px 14px; border-radius:8px; flex:none;
}
#s93 .common .cvalue{ color:var(--ink); font-size:18px; font-weight:700; }
#s93 .common .cnote{ color:var(--muted); font-size:14px; font-weight:600; margin-left:auto; }

/* 팀원 카드 */
#s93 .members{ left:70px; top:256px; width:1780px; display:flex; gap:32px; }
#s93 .member{
  flex:1; min-height:650px; background:#fff; border-radius:20px;
  box-shadow:0 18px 40px rgba(20,40,25,.10);
  display:flex; flex-direction:column; overflow:hidden;
}
#s93 .member.m1{ --pc:#2F6FED; }
#s93 .member.m2{ --pc:#6D5DD3; }
#s93 .member.m3{ --pc:#0B7A3D; }
#s93 .member.m4{ --pc:#8B958E; }

/* 사진 — 카드 상단을 꽉 채우는 비중있는 영역 */
#s93 .photo{
  position:relative; width:100%; height:340px; flex:none;
  background:
    radial-gradient(120% 140% at 50% 15%, color-mix(in srgb, var(--pc) 20%, #fff) 0%, color-mix(in srgb, var(--pc) 10%, #fff) 100%);
  display:flex; align-items:center; justify-content:center;
}
#s93 .photo .phead{ width:72px; height:72px; border-radius:50%; background:var(--pc); opacity:.38; }
#s93 .photo .pbody{ width:132px; height:64px; border-radius:66px 66px 0 0; background:var(--pc); opacity:.38; margin-top:8px; position:absolute; bottom:34px; left:50%; transform:translateX(-50%); }
#s93 .photo .ptag{
  position:absolute; right:16px; bottom:16px; background:rgba(255,255,255,.85);
  color:var(--muted); font-size:13px; font-weight:700; padding:6px 12px; border-radius:999px;
  backdrop-filter:blur(2px);
}
#s93 .photo.has-photo{
  background-size:cover; background-position:center top; background-repeat:no-repeat;
}

/* 본문 */
#s93 .body{ padding:24px 26px 26px; display:flex; flex-direction:column; flex:1; }

#s93 .mname{ display:flex; align-items:center; gap:9px; color:var(--ink); font-size:29px; font-weight:800; }
#s93 .mname .dot{ width:11px; height:11px; border-radius:50%; background:var(--pc); flex:none; }
#s93 .mcount{ color:var(--muted); font-size:14px; font-weight:700; margin-top:5px; font-variant-numeric:tabular-nums; }

#s93 .roletags{ display:flex; flex-wrap:wrap; gap:8px; margin-top:14px; }
#s93 .roletag{
  background:color-mix(in srgb, var(--pc) 10%, #fff); color:var(--ink);
  font-size:14px; font-weight:700; padding:6px 12px; border-radius:8px;
}
#s93 .roletags.empty .roletag{ background:#F1F3F1; color:#A6AFA9; font-style:italic; font-weight:600; }

/* AI가 본 성격 — 하단 고정 */
#s93 .persona{ margin-top:auto; }
#s93 .hr{ height:1px; background:var(--line); margin:20px 0 16px; }
#s93 .plabel{ color:var(--gold); font-size:13px; font-weight:800; letter-spacing:.3px; margin-bottom:10px; }
#s93 .pbox{
  border-radius:10px; padding:16px 18px; font-size:15px; font-weight:600; line-height:1.55;
}
#s93 .pbox.filled{
  background:var(--gold-soft); color:var(--ink); border-left:4px solid var(--gold);
}
#s93 .pbox.empty{
  border:2px dashed #D8DED9; color:#AEB8B1; font-weight:700; text-align:center;
}
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s93">
  <div class="abs ptitle">팀 소개</div>
  <div class="abs psub">TheMoa를 함께 만든 4명의 팀원입니다.</div>

  <div class="abs common">
    <div class="ctag">공통 작업</div>
    <div class="cvalue">기능 설계 · ERD 설계</div>
    <div class="cnote">4명 모두 참여</div>
  </div>

  <div class="abs members">
    <div class="member m1">
      <div class="photo">
        <div class="phead"></div><div class="pbody"></div>
        <div class="ptag">사진 추가</div>
      </div>
      <div class="body">
        <div class="mname"><span class="dot"></span>임수지</div>
        <div class="mcount">담당 1개</div>
        <div class="roletags">
          <div class="roletag">금융상품</div>
        </div>
        <div class="persona">
          <div class="hr"></div>
          <div class="plabel">AI가 본 성격</div>
          <div class="pbox empty">여기에 성격을 채워보세요</div>
        </div>
      </div>
    </div>

    <div class="member m2">
      <div class="photo has-photo" style="background-image:url('assets/team_munhoyeon.png')"></div>
      <div class="body">
        <div class="mname"><span class="dot"></span>문호연</div>
        <div class="mcount">담당 1개</div>
        <div class="roletags">
          <div class="roletag">정책</div>
        </div>
        <div class="persona">
          <div class="hr"></div>
          <div class="plabel">AI가 본 성격</div>
          <div class="pbox filled">AI를 조교급으로 갈아넣는 집착형 실전파</div>
        </div>
      </div>
    </div>

    <div class="member m3">
      <div class="photo has-photo" style="background-image:url('assets/solmin.png')"></div>
      <div class="body">
        <div class="mname"><span class="dot"></span>김솔민</div>
        <div class="mcount">담당 4개</div>
        <div class="roletags">
          <div class="roletag">고정지출</div>
          <div class="roletag">소비가이드</div>
          <div class="roletag">관리자</div>
          <div class="roletag">고객센터</div>
        </div>
        <div class="persona">
          <div class="hr"></div>
          <div class="plabel">AI가 본 성격</div>
          <div class="pbox filled">규칙은 깐깐하게 세우지만, 정작 본인은 후드티 입고<br>여유롭게 커피 마시며 코딩하는 타입</div>
        </div>
      </div>
    </div>

    <div class="member m4">
      <div class="photo">
        <div class="phead"></div><div class="pbody"></div>
        <div class="ptag">사진 추가</div>
      </div>
      <div class="body">
        <div class="mname"><span class="dot"></span>박영균</div>
        <div class="mcount">담당 0개</div>
        <div class="roletags empty">
          <div class="roletag">맡은 게 zero가 되어버림...</div>
        </div>
        <div class="persona">
          <div class="hr"></div>
          <div class="plabel">AI가 본 성격</div>
          <div class="pbox empty">여기에 성격을 채워보세요</div>
        </div>
      </div>
    </div>
  </div>
</section>
`);
