# PPT 작업 규칙

## 왜 파일을 나눴나

원래 `index.html` 하나에 슬라이드 전체가 다 들어있어서, 팀원끼리 다른 슬라이드를 고쳐도
같은 파일을 건드리게 되어 git 충돌이 자주 났음. 그래서 슬라이드별로 파일을 분리함.

```
ppt/
  index.html          슬라이드 목록 + 뼈대만 (거의 안 건드림)
  common.css           공통 레이아웃(덱/네비/전환 애니메이션)
  common.js             공통 엔진(다음/이전, 스텝 애니메이션, 화면 맞춤)
  assets/               이미지
  slides/
    slide1.js            슬라이드1 style+마크업 전부
    slide2.js            슬라이드2 style+마크업 전부
    slide3.js            슬라이드3 style+마크업+동작 전부
```

- **슬라이드별로 나눈 이유**: 각자 담당 슬라이드 파일(`slides/slideN.js`)만 고치면
  다른 사람 작업과 충돌하지 않음.
- **common.css / common.js를 따로 둔 이유**: 3개 슬라이드가 전부 같이 쓰는 엔진이라
  각 슬라이드 파일에 복붙하면 나중에 네비게이션 하나 고칠 때 파일을 다 고쳐야 함.
  대신 이 두 파일은 팀원이 거의 안 건드리는 파일이라 충돌 위험이 적음.
- **`fetch()` 안 쓰고 `<script src>`로 불러오는 이유**: `fetch()`는 브라우저가
  `file://`로 연 로컬 파일은 무조건 막아서 서버 없이는 안 열림. `<script src>` /
  `<link href>`는 그냥 리소스 로드라서 `index.html` 더블클릭만으로도 항상 동작함.
  **그래서 슬라이드 파일 로딩 방식은 절대 `fetch`/`XMLHttpRequest`로 바꾸지 말 것.**

## 어떻게 작업하면 되는지

### 1. 실행 방법
`index.html`을 그냥 더블클릭해서 열면 됨. 서버 필요 없음.

### 2. 내 슬라이드 고치기
`slides/slideN.js` 파일 하나만 열어서 수정. 파일 안에 style과 마크업이 다 들어있음.

```js
document.head.insertAdjacentHTML('beforeend', `
<style>
#s1 .뭔가 { ... }   // 반드시 #s1(자기 슬라이드 id)로 시작해서 다른 슬라이드와 안 겹치게
</style>
`);

document.getElementById('deck').insertAdjacentHTML('beforeend', `
<section class="slide" id="s1">
  ...마크업...
</section>
`);
```

- CSS 선택자는 항상 `#s1 .클래스`, `#s2 .클래스`처럼 **자기 슬라이드 id로 시작**할 것.
  다른 슬라이드 파일과 클래스 이름이 겹쳐도 이렇게 스코프를 걸어두면 서로 안 건드림.
- 백틱( ` ) 문자열 안에 코드가 들어있으니, 마크업 안에서 백틱이나 `${ }`는 쓰지 말 것
  (템플릿 리터럴이 깨짐).

### 3. 새 슬라이드 추가하기
1. `slides/slide4.js` 새로 만들고, 위 형식대로 `#s4` 스코프로 style + 마크업 작성.
2. `index.html`에서 `<script src="slides/slide3.js">` 아래에 한 줄만 추가:
   ```html
   <script src="slides/slide4.js"></script>
   <script src="common.js"></script>
   ```
   (`common.js`는 항상 제일 마지막이어야 함 — 모든 슬라이드가 삽입된 다음에
   엔진이 슬라이드 개수를 세기 때문.)

### 4. 공통 부분(common.css / common.js / index.html) 고치기
전체 네비게이션, 전환 애니메이션, 레이아웃처럼 슬라이드 전체에 영향 가는 걸 고칠 때만
수정. 다른 팀원 작업과 겹치기 쉬운 파일이니 고치기 전에 팀에 얘기하고 최신 상태로
pull 받은 뒤 작업할 것.

### 5. 자주 하는 실수
- `slides/slideN.js`를 브라우저로 직접 열면 안 됨 — 이건 `index.html`이 조립해서
  써야 하는 조각 파일이라 단독으로 열면 스타일도 없고 이미지 경로도 깨짐.
  항상 `index.html`을 열어서 확인할 것.
- 자기 슬라이드 CSS를 `#sN` 없이 클래스명만 쓰면 다른 슬라이드에도 스타일이 새어나감.
