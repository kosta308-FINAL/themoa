/* ===================== 이벤트 기반 아키텍처 (CODEF 연동 & 도메인 분리) ===================== */
document.head.insertAdjacentHTML('beforeend', `
<style>
#s-evtarch{ background:transparent; }
#s-evtarch .ptitle{ position:absolute; left:52px; top:40px; width:1500px; color:#6B7C8A; font-size:45px; font-weight:800; }
#s-evtarch .psub{ position:absolute; left:52px; top:112px; width:1780px; color:#111; font-size:22px; font-weight:500; line-height:1.5; }

#s-evtarch .twocol{ position:absolute; left:70px; top:200px; width:1780px; display:flex; gap:40px; }
#s-evtarch .col{ flex:1; min-height:620px; display:flex; flex-direction:column; background:#fff; border-radius:20px; box-shadow:0 20px 46px rgba(0,0,0,.14);
  padding:34px 30px; }
#s-evtarch .col .chead{ flex:none; display:flex; align-items:center; gap:10px; margin-bottom:20px; }
#s-evtarch .col .chead .cdot{ flex:none; width:14px; height:14px; border-radius:50%; }
#s-evtarch .col.codef .chead .cdot{ background:#2F6FED; }
#s-evtarch .col.evt .chead .cdot{ background:#007613; }
#s-evtarch .col .chead .ctitle{ color:#171717; font-size:20px; font-weight:800; }

#s-evtarch .citems{ flex:1; display:flex; flex-direction:column; justify-content:space-between; }
#s-evtarch .sitem2{ display:flex; align-items:flex-start; gap:16px; }
#s-evtarch .sitem2 .snum2{ flex:none; width:34px; height:34px; border-radius:50%; color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:13px; font-weight:800; }
#s-evtarch .col.codef .snum2{ background:#2F6FED; }
#s-evtarch .col.evt .snum2{ background:#007613; }
#s-evtarch .sitem2 .stitle2{ color:#171717; font-size:18px; font-weight:800; margin-bottom:8px; }
#s-evtarch .sitem2 .sdesc2{ color:#555; font-size:16px; font-weight:500; line-height:1.65; }
#s-evtarch .sitem2 .scode{ display:inline-block; margin-top:8px; background:#F3F5F7; color:#333;
  font-size:13px; font-weight:700; padding:4px 10px; border-radius:8px; }

#s-evtarch .note{ position:absolute; left:70px; top:860px; width:1780px; background:#fff; border-radius:14px;
  box-shadow:0 10px 24px rgba(0,0,0,.08); padding:18px 28px; color:#444; font-size:16px; font-weight:600;
  display:flex; align-items:center; gap:12px; }
#s-evtarch .note .dot{ flex:none; width:10px; height:10px; border-radius:50%; background:#2D8A4E; }
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s-evtarch">
  <div class="ptitle">이벤트 기반 아키텍처</div>
  <div class="psub">실제 은행 마이데이터 연동과 도메인 간 결합도 관리, 두 곳에서 같은<br>이벤트 기반 패턴(발행 도메인은 구독자를 모른다)을 적용했습니다.</div>

  <div class="twocol">
    <div class="col codef">
      <div class="chead"><div class="cdot"></div><div class="ctitle">CODEF 마이데이터 연동</div></div>

      <div class="citems">
      <div class="sitem2">
        <div class="snum2">01</div>
        <div>
          <div class="stitle2">connectedId 발급</div>
          <div class="sdesc2">카드사 로그인 정보는 암호화해 SDK에 전달만 하고 저장하지 않습니다.<br>발급받은 연결 식별자(connectedId)만 DB에 남깁니다.</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">02</div>
        <div>
          <div class="stitle2">비동기 승인내역 백필</div>
          <div class="sdesc2">연동에 성공하면 이벤트를 발행하고, 별도 리스너가 커밋 이후<br>승인내역 수집을 비동기로 처리해 화면 응답이 지연되지 않습니다.</div>
          <div class="scode">@Async @TransactionalEventListener(AFTER_COMMIT)</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">03</div>
        <div>
          <div class="stitle2">실패코드 분기 + 쿨다운</div>
          <div class="sdesc2">CF-12801(비밀번호 오류) 같은 실패 코드별로 다르게 처리하고,<br>반복 실패 시에는 계정을 잠가 추가 시도를 막습니다.</div>
        </div>
      </div>
      </div>
    </div>

    <div class="col evt">
      <div class="chead"><div class="cdot"></div><div class="ctitle">예·적금 &harr; 고정지출 이벤트 연동</div></div>

      <div class="citems">
      <div class="sitem2">
        <div class="snum2">01</div>
        <div>
          <div class="stitle2">발행 도메인은 구독자를 모른다</div>
          <div class="sdesc2">예·적금 가입·해지 시 이벤트만 발행할 뿐,<br>고정지출 도메인이 있다는 사실 자체를 알지 못합니다.</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">02</div>
        <div>
          <div class="stitle2">비동기 구독 + 멱등 처리</div>
          <div class="sdesc2">고정지출 쪽 리스너가 이 이벤트를 구독해 저축 항목으로<br>자동 등록하고, 중복 등록을 막아 재실행에도 안전합니다.</div>
          <div class="scode">@Async @TransactionalEventListener(AFTER_COMMIT)</div>
        </div>
      </div>
      <div class="sitem2">
        <div class="snum2">03</div>
        <div>
          <div class="stitle2">단방향 의존 검증</div>
          <div class="sdesc2">고정지출 코드는 이벤트 클래스만 참조하고,<br>예·적금 코드에는 고정지출을 참조하는 코드가 한 줄도 없습니다.</div>
        </div>
      </div>
      </div>
    </div>
  </div>

  <div class="note"><div class="dot"></div>두 기능 모두 도메인 간 직접 참조 대신 이벤트로 연결해, 한쪽 도메인의 변경이 반대쪽 코드에 영향을 주지 않도록 설계했습니다.</div>
</section>
`);
