import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // 백엔드는 CORS 설정 없이 동일 origin(배포 시 nginx 리버스 프록시) 전제라,
    // 개발에서도 /api 요청을 Spring(8080)으로 프록시해 같은 구조를 재현한다.
    // 8080 직접 호출은 JSON POST의 CORS preflight에서 막힌다(백엔드에 CORS 설정 없음).
    // 쿠키는 쟁점이 아니다 — localhost 간 포트 차이는 same-site라 SameSite=Strict여도 실린다.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
