import { useCallback, useState } from 'react'
import AuthContext from './AuthContext'
import { logout as requestLogout } from '../api/authApi'

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(() =>
    Boolean(localStorage.getItem('accessToken'))
  )

  /** 로그인·회원가입 성공 응답({ accessToken })으로 세션을 시작한다. */
  const login = useCallback(({ accessToken }) => {
    localStorage.setItem('accessToken', accessToken)
    setIsAuthenticated(true)
  }, [])

  /** 서버의 Refresh Token을 무효화하고 로컬 토큰을 지운다. 서버 호출이 실패해도 로컬 세션은 끝낸다. */
  const logout = useCallback(async () => {
    try {
      await requestLogout()
    } catch {
      // 이미 만료·폐기된 토큰이어도 로그아웃은 성공으로 취급한다
    } finally {
      localStorage.removeItem('accessToken')
      setIsAuthenticated(false)
    }
  }, [])

  return (
    <AuthContext.Provider value={{ isAuthenticated, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export default AuthProvider
