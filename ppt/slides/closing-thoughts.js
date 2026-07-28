/* ===================== 마무리 소감 (팀원별 3분할) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-closing{ background:transparent; }
#s-closing .ptitle{ position:absolute; left:52px; top:40px; width:1500px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s-closing .psub{ position:absolute; left:52px; top:112px; width:1780px; color:#111; font-size:22px; font-weight:500; line-height:1.5; }

#s-closing .cols{ position:absolute; left:70px; top:220px; width:1780px; display:flex; gap:32px; }
#s-closing .ccol{ position:relative; overflow:hidden; flex:1; background:#fff; border-radius:24px; box-shadow:0 20px 46px rgba(0,0,0,.14);
  padding:40px 34px 36px; display:flex; flex-direction:column; min-height:700px; }
#s-closing .ccol::before{ content:''; position:absolute; top:0; left:0; width:100%; height:7px; }
#s-closing .p1::before{ background:linear-gradient(90deg,#2F6FED,#7FB0FF); }
#s-closing .p2::before{ background:linear-gradient(90deg,#007613,#4FBE5D); }
#s-closing .p3::before{ background:linear-gradient(90deg,#7C3AED,#B48CF7); }

#s-closing .cwatermark{ position:absolute; right:14px; top:-28px; font-size:190px; font-weight:900; line-height:1;
  font-family:Georgia,serif; opacity:.07; pointer-events:none; }
#s-closing .p1 .cwatermark{ color:#2F6FED; }
#s-closing .p2 .cwatermark{ color:#007613; }
#s-closing .p3 .cwatermark{ color:#7C3AED; }

#s-closing .chead{ position:relative; z-index:1; display:flex; align-items:center; gap:16px; }
#s-closing .cavatar{ flex:none; width:66px; height:66px; border-radius:50%; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:21px; font-weight:800;
  box-shadow:0 10px 20px -6px rgba(0,0,0,.35); }
#s-closing .p1 .cavatar{ background:linear-gradient(135deg,#2F6FED,#6398FF); }
#s-closing .p2 .cavatar{ background:linear-gradient(135deg,#007613,#2AA23C); }
#s-closing .p3 .cavatar{ background:linear-gradient(135deg,#7C3AED,#9C67F2); }
#s-closing .cname{ font-size:27px; font-weight:800; color:#171717; margin-bottom:6px; }
#s-closing .crole{ display:inline-block; padding:5px 13px; border-radius:999px; font-size:14px; font-weight:800; }
#s-closing .p1 .crole{ background:#EAF1FF; color:#2F6FED; }
#s-closing .p2 .crole{ background:#E8F5EA; color:#007613; }
#s-closing .p3 .crole{ background:#F1EBFC; color:#7C3AED; }

#s-closing .cdivider{ position:relative; z-index:1; height:1px; background:#EDF0F4; margin:24px 0; }

#s-closing .ctext{ position:relative; z-index:1; flex:1; font-size:20px; color:#333; font-weight:500; line-height:1.8;
  display:flex; flex-direction:column; justify-content:center; }

#s-closing .placeholder{ position:relative; z-index:1; flex:1; border:2px dashed #D8DEE5; border-radius:14px;
  display:flex; align-items:center; justify-content:center; text-align:center;
  color:#9AA5B1; font-size:18px; font-weight:700; line-height:1.7; padding:20px; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-closing">
  <div class="ptitle">마무리 소감</div>
  <div class="psub">짧은 시간 동안 함께 만든 프로젝트를 마치며, 각자의 소감을 남깁니다.</div>

  <div class="cols">
    <div class="ccol p1">
      <div class="cwatermark">&ldquo;</div>
      <div class="chead">
        <div class="cavatar">솔민</div>
        <div><div class="cname">김솔민</div><div class="crole">고정지출 · 소비가이드</div></div>
      </div>
      <div class="cdivider"></div>
      <div class="ctext">3주라면 길면 길고 짧으면 짧은 시간일 수도 있지만,<br>API를 분석해 ERD를 설계하거나 여러 예외 상황을<br>고려하는 복잡한 설계 과정을 경험해 볼 수 있어서<br>좋았습니다.<br><br>LLM을 활용한 RAG 결과의 퀄리티를 높이기 위해<br>어떤 부분들을 신경 써야 하는지 배울 수 있는<br>좋은 시간이었던 것 같습니다.</div>
    </div>

    <div class="ccol p2">
      <div class="cwatermark">&ldquo;</div>
      <div class="chead">
        <div class="cavatar">호연</div>
        <div><div class="cname">문호연</div><div class="crole">정책 추천</div></div>
      </div>
      <div class="cdivider"></div>
      <div class="ctext">외부 API에서 정보를 받아 RAG를 구현하는 과정에서,<br>통일되지 않은 데이터 형태를 정제하기 위한 기준을 세우고<br>직접 적용하며 성취감을 느낄 수 있었습니다.<br><br>단순해 보이는 결과를 만들기 위해서는<br>복잡한 데이터를 사용자가 이해하기 쉬운 형태로<br>정제하는 과정이 중요하다는 것을 깨달았습니다.<br><br>다음 프로젝트에서는 초반부터 더 명확한 기준을 세워<br>데이터 정제와 구현에 드는 시간을<br>더욱 효율적으로 단축하고 싶습니다.</div>
    </div>

    <div class="ccol p3">
      <div class="cwatermark">&ldquo;</div>
      <div class="chead">
        <div class="cavatar">수지</div>
        <div><div class="cname">임수지</div><div class="crole">금융상품</div></div>
      </div>
      <div class="cdivider"></div>
      <div class="ctext">이번 프로젝트를 하면서 정보를 많이 보여주는 것보다<br>필요한 것만 정확히, 신뢰감 있게 보여주는 게<br>훨씬 중요하다는 걸 느꼈습니다.<br><br>특히 소비, 금융, 정책이 따로 노는 문제가 아니라<br>하나로 이어진 흐름이라는 걸 발견한 게<br>가장 큰 소득이었고, 우대조건에 따라<br>만기금액이 바뀌는 계산 부분을 만들면서<br>작은 문구 하나가 서비스 신뢰도를 좌우한다는 것도<br>배웠습니다.</div>
    </div>
  </div>
</section>
`);
