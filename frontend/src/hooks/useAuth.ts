import { useEffect } from 'react'
import { useAuthStore } from '../store/authStore'
import { refresh, logout as apiLogout } from '../api/auth'
import { jwtDecode } from 'jwt-decode'

interface JwtPayload { sub: string; exp: number }

export function useAuth() {
  const { setAuth, clearAuth, accessToken } = useAuthStore()

  // Silent refresh on mount
  useEffect(() => {
    if (!accessToken) {
      refresh()
        .then(({ accessToken: token }) => {
          const { sub } = jwtDecode<JwtPayload>(token)
          setAuth(token, sub)
        })
        .catch(() => { /* not logged in */ })
    }
  }, [])

  // Schedule refresh before expiry
  useEffect(() => {
    if (!accessToken) return
    const { exp } = jwtDecode<JwtPayload>(accessToken)
    const msUntilExpiry = exp * 1000 - Date.now() - 30_000 // 30s before expiry
    const timer = setTimeout(() => {
      refresh()
        .then(({ accessToken: token }) => {
          const { sub } = jwtDecode<JwtPayload>(token)
          setAuth(token, sub)
        })
        .catch(clearAuth)
    }, Math.max(msUntilExpiry, 0))
    return () => clearTimeout(timer)
  }, [accessToken])

  async function logout() {
    try {
      await apiLogout()
    } finally {
      clearAuth()
    }
  }

  return { accessToken, logout }
}
