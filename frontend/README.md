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
