import { create } from 'zustand'

interface AuthState {
  accessToken: string | null
  username: string | null
  setAuth: (token: string, username: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  username: null,
  setAuth: (accessToken, username) => set({ accessToken, username }),
  clearAuth: () => set({ accessToken: null, username: null }),
}))
