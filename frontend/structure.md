# 프로젝트 구조 가이드

새 화면이나 파일을 추가할 때 "이걸 어디에 둘지" 판단하는 기준 문서입니다. 이 프로젝트는 **기능(도메인) 단위 구조(feature-based)** 를 따릅니다 — 화면 하나가 `features/` 아래 폴더 하나에 대응하고, 그 화면 전용 컴포넌트/스타일은 같은 폴더 안에 둡니다.

## 디렉토리 구조

```
front/
├── public/                     # 정적 파일 (favicon 등, 빌드 시 그대로 복사됨)
├── src/
│   ├── api/                    # axios 인스턴스, 도메인별 API 요청 함수
│   │   └── axiosInstance.js
│   ├── assets/                 # 이미지, 아이콘 등 코드에서 import해서 쓰는 리소스
│   ├── components/
│   │   ├── layout/             # 여러 feature가 공통으로 쓰는 레이아웃 (Header, Footer, DashboardTopNav 등)
│   │   └── common/              # 특정 feature에 종속되지 않는 범용 UI 컴포넌트
│   ├── constants/                # 라우트 경로, 공통 상수 등
│   ├── contexts/                 # Context API 기반 전역 상태 (예: AuthContext)
│   ├── features/                 # 라우트(도메인) 단위 화면. 화면 하나 = 폴더 하나
│   │   ├── landing/
│   │   │   ├── LandingPage.jsx
│   │   │   └── LandingPage.css
│   │   ├── dashboard/
│   │   │   ├── Dashboard.jsx
│   │   │   ├── Dashboard.css
│   │   │   └── components/       # Dashboard 화면 전용 하위 컴포넌트
│   │   ├── products/
│   │   │   └── ProductsPage.jsx
│   │   ├── policy/
│   │   │   └── PolicyPage.jsx
│   │   ├── spending-guide/
│   │   │   └── SpendingGuidePage.jsx
│   │   ├── loan-compare/
│   │   │   └── LoanComparePage.jsx
│   │   └── mypage/
│   │       └── MyPage.jsx
│   ├── hooks/                     # 특정 feature에 종속되지 않는 커스텀 훅
│   ├── routes/                    # react-router-dom 라우트 설정
│   │   └── AppRouter.jsx
│   ├── styles/                     # 전역 스타일 (CSS 변수, 리셋 등)
│   │   └── globals.css
│   ├── utils/                       # 포맷터 등 순수 유틸 함수
│   ├── App.jsx                      # Router/Provider 조립부
│   └── main.jsx                     # 엔트리 포인트
├── .env.example
├── vite.config.js
└── package.json
```

## 폴더별 규칙

### `features/` — 화면(도메인) 단위
- 라우트에 매핑되는 화면 하나당 폴더 하나: `features/도메인이름/`
- 폴더명은 소문자, 두 단어 이상이면 kebab-case (`spending-guide`, `loan-compare`)
- 메인 페이지 컴포넌트는 `features/도메인이름/이름Page.jsx` (랜딩처럼 관례상 `Page` 접미사를 생략해도 무방)
- 페이지 전용 스타일은 같은 폴더에 `이름.css`로 둡니다
- 그 화면에서만 쓰는 하위 컴포넌트는 `features/도메인이름/components/`에 둡니다. **다른 feature에서 재사용할 일이 없는 컴포넌트만** 여기 둡니다

### `components/layout` — 여러 feature가 공통으로 쓰는 레이아웃
- 사이트 전역 레이아웃(Header, Footer)이나 `/dashboard/*` 계열 여러 feature가 함께 쓰는 레이아웃 조각(DashboardTopNav, DashboardFooter)

### `components/common` — 특정 feature에 종속되지 않는 범용 UI
- 두 개 이상의 feature에서 재사용되는 아이콘, 버튼, 인풋, 빈 상태(EmptyState) 등
- feature 안에서 만들다가 다른 화면에도 필요해지면 이곳으로 승격합니다

### `api/` — 모든 요청은 `axiosInstance`를 거친다
- **서버 요청은 예외 없이 `api/axiosInstance.js`로 보냅니다.** feature 컴포넌트에서 `axios`를 직접 import하거나 `fetch`를 쓰지 않습니다. `axiosInstance`가 `baseURL`(`VITE_API_BASE_URL`), `withCredentials`, 요청 인터셉터의 `Authorization: Bearer` 헤더 주입을 담당하므로 우회하면 인증 헤더 없이 요청이 나갑니다
- **`axios`를 직접 import하는 파일은 `axiosInstance.js` 하나뿐입니다.** baseURL·헤더·인터셉터 같은 공통 설정은 전부 이 파일에서만 바꿉니다
- 도메인별로 요청 함수를 파일로 나눕니다: `authApi.js`, `userApi.js`, `spendingApi.js` … (`도메인Api.js`, camelCase)
- 도메인 API 파일은 **URL과 요청/응답 형태만** 다룹니다. 화면 상태나 렌더링 로직을 넣지 않습니다

```js
// src/api/spendingApi.js
import axiosInstance from './axiosInstance'

export const getMonthlySpending = (yearMonth) =>
  axiosInstance.get('/api/spending/monthly', { params: { yearMonth } })

export const updateBudget = (payload) =>
  axiosInstance.put('/api/budget', payload)
```

```jsx
// src/features/spending-guide/SpendingGuidePage.jsx
import { getMonthlySpending } from '../../api/spendingApi'

const res = await getMonthlySpending('2026-07')
```

- 여러 화면이 같은 데이터를 부르거나 로딩/에러 상태가 반복되면, 호출을 감싼 커스텀 훅을 `hooks/`(공통) 또는 `features/도메인/hooks/`(그 화면 전용)에 만듭니다

### `contexts/`
- Context API로 만든 전역 상태(로그인 정보 등). Provider와 Context 정의를 함께 둡니다

### `hooks/`
- 특정 feature에 종속되지 않는 커스텀 훅 (`useAuth` 등)

### `routes/`
- `<Routes>`/`<Route>` 정의를 모아두는 곳. `features/`의 페이지 컴포넌트를 조합합니다. 화면이 늘어나면 이 파일에 라우트만 추가합니다

## 새 화면(feature) 추가 절차

1. `src/features/기능이름/` 폴더를 만듭니다 (소문자, kebab-case)
2. `기능이름/기능이름Page.jsx`에 메인 페이지 컴포넌트를 작성합니다. 필요하면 같은 폴더에 `.css`를 둡니다
3. 이 화면 전용 하위 컴포넌트는 `기능이름/components/`에 작성합니다
4. `routes/AppRouter.jsx`에 라우트를 추가합니다
5. 다른 화면에서도 재사용해야 하는 컴포넌트가 생기면 `components/common`(범용 UI) 또는 `components/layout`(레이아웃)으로 옮깁니다
6. API 호출이 필요하면 `api/도메인Api.js`에 `axiosInstance`를 쓰는 함수를 추가하고, feature 컴포넌트는 그 함수만 import해서 씁니다 (컴포넌트에서 `axios`/`fetch` 직접 사용 금지)

## 네이밍 규칙

| 대상 | 규칙 | 예시 |
|---|---|---|
| feature 폴더 | lowercase, 여러 단어는 kebab-case | `spending-guide` |
| 컴포넌트 파일 | PascalCase | `SummaryCards.jsx` |
| 훅 파일 | camelCase, `use` 접두사 | `useAuth.js` |
| API 파일 | camelCase, `Api` 접미사 | `spendingApi.js` |
| CSS 파일 | 대응하는 컴포넌트와 동일한 이름 | `Dashboard.jsx` ↔ `Dashboard.css` |
