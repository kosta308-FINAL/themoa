# Themore Front

React(Vite) 기반 프론트엔드 프로젝트입니다.

## 기술 스택

- **React 19** + **Vite**
- **react-router-dom** — 라우팅
- **Context API** — 전역 상태관리 (별도 라이브러리 없이 React 내장 기능 사용)
- **axios** — API 통신

## 디렉토리 구조

```
front/
├── public/                  # 정적 파일 (favicon 등, 빌드 시 그대로 복사됨)
├── src/
│   ├── api/                 # axios 인스턴스, 도메인별 API 요청 함수
│   │   └── axiosInstance.js
│   ├── assets/               # 이미지, 아이콘 등 코드에서 import해서 쓰는 리소스
│   ├── components/
│   │   ├── layout/          # Header, Footer 등 여러 페이지에서 공통으로 쓰는 레이아웃 컴포넌트
│   │   └── common/          # 버튼, 인풋 등 재사용 UI 컴포넌트
│   ├── constants/            # 라우트 경로, 공통 상수 등
│   ├── contexts/             # Context API 기반 전역 상태 (예: AuthContext)
│   ├── hooks/                 # 커스텀 훅
│   ├── pages/                 # 라우트 단위 페이지. 페이지별 폴더에 jsx/css를 함께 둠
│   │   └── LandingPage/
│   │       ├── LandingPage.jsx
│   │       └── LandingPage.css
│   ├── routes/                # react-router-dom 라우트 설정
│   │   └── AppRouter.jsx
│   ├── styles/                # 전역 스타일 (CSS 변수, 리셋 등)
│   │   └── globals.css
│   ├── utils/                  # 포맷터 등 순수 유틸 함수
│   ├── App.jsx                 # Router/Provider 조립부
│   └── main.jsx                # 엔트리 포인트
├── .env.example                # 환경변수 예시 (실제 .env는 git에 커밋하지 않음)
├── vite.config.js
└── package.json
```

## 폴더별 규칙

- **pages/**: 라우트에 매핑되는 화면 단위. 페이지 전용 컴포넌트/스타일은 해당 페이지 폴더 안에 둡니다. (`pages/이름/이름.jsx`, `이름.css`)
- **components/layout**: 여러 페이지에서 공통으로 쓰는 레이아웃 조각(Header, Footer, Sidebar 등).
- **components/common**: 특정 페이지에 종속되지 않는 범용 UI 컴포넌트.
- **api/**: axios 인스턴스와 도메인별 API 함수(`userApi.js`, `authApi.js` 등)를 이 폴더에 모읍니다. 컴포넌트에서 axios를 직접 import하지 않습니다.
- **contexts/**: Context API로 만든 전역 상태(로그인 정보 등). Provider와 커스텀 훅(`useAuth` 등)을 함께 정의합니다.
- **routes/**: `<Routes>`/`<Route>` 정의를 모아두는 곳. 페이지가 늘어나면 이 파일에 라우트만 추가합니다.

## 환경변수

`.env.example`을 복사해 `.env`를 만들고 값을 채워주세요.

```
cp .env.example .env
```

| 변수 | 설명 |
|---|---|
| `VITE_API_BASE_URL` | 백엔드 API 서버 주소 |

## 실행

```bash
npm install
npm run dev      # 개발 서버
npm run build    # 프로덕션 빌드
npm run lint      # ESLint 검사
```
