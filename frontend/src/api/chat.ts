import axios from 'axios'
import type { ChatSession, Message } from '../types'
import { useAuthStore } from '../store/authStore'

const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL ?? '' })

function authHeader() {
  const token = useAuthStore.getState().accessToken
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function getSessions(): Promise<ChatSession[]> {
  const res = await api.get<ChatSession[]>('/api/sessions', { headers: authHeader() })
  return res.data
}

export async function createSession(title: string): Promise<ChatSession> {
  const res = await api.post<ChatSession>('/api/sessions', { title }, { headers: authHeader() })
  return res.data
}

export async function getMessages(sessionId: string): Promise<Message[]> {
  const res = await api.get<Message[]>(`/api/sessions/${encodeURIComponent(sessionId)}/messages`, { headers: authHeader() })
  return res.data
}
