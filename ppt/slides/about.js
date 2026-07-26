/* ===================== 소개 (4번째 슬라이드) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#sAbout{ background:#F8F8F8; }
#sAbout .ptitle{ left:52px; top:40px; width:711px; color:#6B7C8A; font-size:45px; font-weight:800; }
/* 로고 + 글자: 페이지 정중앙(x=960) 대칭 */
#sAbout .logo{ left:755px; top:143px; width:410px; height:412px; object-fit:contain; }
#sAbout .brand{ left:606px; top:436px; width:708px; text-align:center;
  color:#2D8A5E; font-size:85px; font-weight:800; line-height:1; letter-spacing:-1px; }
#sAbout .subtitle{ left:606px; top:534px; width:708px; text-align:center;
  color:#1A2332; font-size:28px; font-weight:500; }
#sAbout .target{ left:724px; top:588px; width:472px; text-align:center;
  color:#6B7C8A; font-size:16px; font-weight:500; }
#sAbout .divider{ left:606px; top:634px; width:708px; height:1px; background:#DCE3E0; }
/* 정보 리스트 (초록 사각 불릿) */
#sAbout .info{ position:absolute; left:820px; width:660px; color:#1A2332;
  font-size:21px; font-weight:500; line-height:1.3; }
#sAbout .info::before{ content:""; position:absolute; left:-29px; top:8px;
  width:10px; height:10px; background:#2D8A5E; }
/* 하단 칩 (클릭 가능 버튼) + 초록 탭 */
#sAbout .chip{ position:absolute; width:276px; height:70px; background:#E8F5EE;
  border-radius:16px; display:flex; align-items:center; justify-content:center;
  color:#2C3E50; font-size:38px; font-weight:600;
  cursor:pointer; border:1px solid #cfe9db; box-shadow:0 2px 6px rgba(0,0,0,.08);
  transition:transform .15s, box-shadow .15s, background .15s; }
#sAbout .chip:hover{ background:#d6f0e2; transform:translateY(-3px);
  box-shadow:0 8px 18px rgba(45,138,94,.28); }
#sAbout .chip:active{ transform:translateY(-1px); }
#sAbout .chiptab{ position:absolute; width:72px; height:9px; background:#2D8A5E; border-radius:5px; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="sAbout">
  <div class="abs ptitle">소개</div>
  <img class="abs logo" src="assets/about_logo.png" alt="TheMoa 로고">
  <div class="abs brand">TheMoa</div>
  <div class="abs subtitle">맞춤형 청년 자산관리 플랫폼</div>
  <div class="abs target">Target: 사회초년생 · 청년</div>
  <div class="abs divider"></div>

  <div class="info" style="top:659px">이름: TheMoa(더모아)</div>
  <div class="info" style="top:715px">주소: https://www.themoa.kro.kr</div>
  <div class="info" style="top:772px">타켓: 사회초년생 + 청년</div>

  <div class="abs chiptab" style="left:608px;top:865px"></div>
  <div class="abs chip"    style="left:508px;top:871px"  data-feature="소비관리">소비관리</div>
  <div class="abs chiptab" style="left:930px;top:865px"></div>
  <div class="abs chip"    style="left:828px;top:871px"  data-feature="정책추천">정책 추천</div>
  <div class="abs chiptab" style="left:1255px;top:866px"></div>
  <div class="abs chip"    style="left:1147px;top:873px" data-feature="금융상품추천">금융상품 추천</div>
</section>
`);

/* 칩 클릭 → (나중에) 각 기능 핵심소개 페이지로 이동. 아직 페이지가 없어 준비만 해둠 */
document.querySelectorAll('#sAbout .chip').forEach(function(chip){
  chip.addEventListener('click', function(e){
    e.stopPropagation();          // 클릭해도 슬라이드가 넘어가지 않게
    const target = chip.dataset.feature;
    // TODO: 핵심기능소개 페이지를 만들면 여기서 이동을 연결하세요.
    //  예) location.hash = '#' + target;  또는 해당 슬라이드로 goTo
    console.log('[TODO] ' + target + ' 핵심기능 소개 페이지로 이동');
  });
});
