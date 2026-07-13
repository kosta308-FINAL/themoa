# Themore Front

React(Vite) 기반 프론트엔드 프로젝트입니다.

## 기술 스택

- **React 19** + **Vite**
- **react-router-dom** — 라우팅
- **Context API** — 전역 상태관리 (별도 라이브러리 없이 React 내장 기능 사용)
- **axios** — API 통신

## 구조화 원칙

**기능(도메인) 단위 구조(feature-based)** 를 따릅니다. 파일을 어디에 둘지는 아래 3가지로 판단합니다.

1. **화면 하나 = `features/` 폴더 하나.** 라우트에 매핑되는 화면은 `features/도메인이름/`에 만들고, 그 화면에서만 쓰는 하위 컴포넌트·스타일은 같은 폴더 안에 함께 둡니다(`features/dashboard/components/`, `Dashboard.css`).
2. **두 개 이상의 feature가 쓰기 시작하면 그때 밖으로 승격.** 범용 UI는 `components/common/`, 레이아웃은 `components/layout/`으로 옮깁니다. 처음부터 공통 폴더에 만들지 않습니다.
3. **화면이 아닌 것은 역할별 폴더로.** API는 `api/`, 전역 상태는 `contexts/`, 훅은 `hooks/`, 순수 함수는 `utils/`, 상수는 `constants/`.

## 디렉토리 구조

```
front/
├── public/                  # 정적 파일 (favicon 등, 빌드 시 그대로 복사됨)
├── src/
│   ├── api/                 # axios 인스턴스, 도메인별 API 요청 함수
│   ├── assets/               # 이미지, 아이콘 등 코드에서 import해서 쓰는 리소스
│   ├── components/
│   │   ├── layout/          # 여러 feature가 공통으로 쓰는 레이아웃 컴포넌트
│   │   └── common/          # 특정 feature에 종속되지 않는 범용 UI 컴포넌트
│   ├── constants/            # 라우트 경로, 공통 상수 등
│   ├── contexts/             # Context API 기반 전역 상태 (예: AuthContext)
│   ├── features/              # 라우트(도메인) 단위 화면. 화면 하나 = 폴더 하나 (예: dashboard/, mypage/)
│   ├── hooks/                  # 커스텀 훅
│   ├── routes/                 # react-router-dom 라우트 설정
│   ├── styles/                  # 전역 스타일 (CSS 변수, 리셋 등)
│   ├── utils/                    # 포맷터 등 순수 유틸 함수
│   ├── App.jsx                   # Router/Provider 조립부
│   └── main.jsx                  # 엔트리 포인트
├── .env.example                # 환경변수 예시 (실제 .env는 git에 커밋하지 않음)
├── vite.config.js
└── package.json
```

폴더별 상세 규칙과 새 화면 추가 절차는 [structure.md](./structure.md)를 참고하세요.

## API 통신 규칙

**모든 서버 요청은 `src/api/axiosInstance.js`를 통해서만 보냅니다.** feature 컴포넌트에서 `axios`를 직접 import하거나 `fetch`를 쓰지 않습니다.

`axiosInstance`가 `baseURL`(`VITE_API_BASE_URL`), `withCredentials`, 요청 인터셉터의 `Authorization: Bearer` 헤더 주입을 담당하므로, 이걸 우회하면 인증 헤더 없이 요청이 나갑니다.

구조는 2단계입니다.

```
src/api/
├── axiosInstance.js   # 공통 설정 (baseURL, 인증 헤더, 인터셉터) — 여기만 axios를 직접 import
├── authApi.js         # 도메인별 요청 함수 — axiosInstance를 import해서 사용
└── spendingApi.js
```

도메인별 API 파일은 URL과 요청/응답 형태만 다루고, 컴포넌트는 그 함수만 호출합니다.

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
```

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
