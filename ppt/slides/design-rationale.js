/* ===================== 왜 이렇게 설계했는가 (비기능 요구사항 → 설계 결정 브릿지) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-whydesign{ background:transparent; }
#s-whydesign .ptitle{ position:absolute; left:52px; top:40px; width:1500px; color:#6B7C8A; font-size:45px; font-weight:800; }

#s-whydesign .threecol{ position:absolute; left:70px; top:160px; width:1780px; display:flex; gap:32px; }
#s-whydesign .col{ flex:1; min-height:650px; display:flex; flex-direction:column; background:#fff; border-radius:20px;
  box-shadow:0 20px 46px rgba(0,0,0,.14); padding:32px 30px; }

#s-whydesign .badge{ align-self:flex-start; padding:6px 14px; border-radius:20px; font-size:16px; font-weight:800; }
#s-whydesign .col.evt .badge{ background:rgba(47,111,237,.1); color:#2F6FED; }
#s-whydesign .col.sec .badge{ background:rgba(192,57,43,.1); color:#C0392B; }
#s-whydesign .col.infra .badge{ background:rgba(45,138,78,.1); color:#2D8A4E; }

#s-whydesign .stitle{ color:#171717; font-size:24px; font-weight:800; line-height:1.35; margin-top:16px; margin-bottom:14px; }
#s-whydesign .sdesc{ color:#555; font-size:20px; font-weight:500; line-height:1.65; flex:1; }

#s-whydesign .next{ margin-top:18px; display:flex; align-items:center; gap:8px; padding:14px 18px; border-radius:12px;
  font-size:18px; font-weight:800; }
#s-whydesign .col.evt .next{ background:rgba(47,111,237,.08); color:#2F6FED; }
#s-whydesign .col.sec .next{ background:rgba(192,57,43,.08); color:#C0392B; }
#s-whydesign .col.infra .next{ background:rgba(45,138,78,.08); color:#2D8A4E; }
#s-whydesign .next .arrow{ font-size:20px; }

#s-whydesign .note{ position:absolute; left:70px; top:870px; width:1780px; background:#fff; border-radius:14px;
  box-shadow:0 10px 24px rgba(0,0,0,.08); padding:20px 28px; color:#444; font-size:20px; font-weight:600;
  display:flex; align-items:center; gap:12px; }
#s-whydesign .note .dot{ flex:none; width:10px; height:10px; border-radius:50%; background:#2D8A4E; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-whydesign">
  <div class="ptitle">왜 이렇게 설계했는가</div>

  <div class="threecol">
    <div class="col evt">
      <div class="badge">요구사항 01</div>
      <div class="stitle">도메인이 계속 늘어나는 구조</div>
      <div class="sdesc">CODEF 마이데이터 연동, 예·적금, 고정지출처럼<br>서로 다른 시점에 생긴 도메인이 계속 늘어나는데,<br>직접 참조로 얽으면 한 쪽 변경이 반대쪽 코드까지<br>번지는 구조가 됩니다. 유지보수성·확장성과<br>API 응답 속도 개선이 필요했습니다.</div>
      <div class="next"><span class="arrow">&#9656;</span>이벤트 기반 아키텍처로 해결</div>
    </div>

    <div class="col sec">
      <div class="badge">요구사항 02</div>
      <div class="stitle">실제 금융·개인정보를 다루는 서비스</div>
      <div class="sdesc">카드사 로그인 정보, 계좌·소비 내역처럼<br>실제 금융·개인정보가 오가기 때문에,<br>탈취를 막는 데 그치지 않고 탈취돼도 피해를<br>최소화하는 방향이 필요했습니다.</div>
      <div class="next"><span class="arrow">&#9656;</span>인증 보안 설계로 해결</div>
    </div>

    <div class="col infra">
      <div class="badge">요구사항 03</div>
      <div class="stitle">서버·DB가 외부에 그대로 노출되는 구조</div>
      <div class="sdesc">실제 금융·개인정보를 다루는 서비스라,<br>서버나 DB가 외부에 그대로 노출되면<br>해킹·무단 접근의 표적이 되기 쉽습니다.<br>외부 접근 지점을 최소화할 필요가 있었습니다.</div>
      <div class="next"><span class="arrow">&#9656;</span>AWS 인프라 설계로 해결</div>
    </div>
  </div>

  <div class="note"><div class="dot"></div>세 결정 모두 출발점은 같습니다 — 데모가 아니라 실제로 동작하는 서비스를 만든다는 목표입니다.</div>
</section>
`);
