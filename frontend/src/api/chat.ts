import axios from 'axios'
import type { ChatSession, Message } from '../types'
import { useAuthStore } from '../store/authStore'

function authHeader() {
  const token = useAuthStore.getState().accessToken
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function getSessions(): Promise<ChatSession[]> {
  const res = await axios.get<ChatSession[]>('/api/sessions', { headers: authHeader() })
  return res.data
}

export async function createSession(title: string): Promise<ChatSession> {
  const res = await axios.post<ChatSession>('/api/sessions', { title }, { headers: authHeader() })
  return res.data
}

export async function getMessages(sessionId: string): Promise<Message[]> {
  const res = await axios.get<Message[]>(`/api/sessions/${sessionId}/messages`, { headers: authHeader() })
  return res.data
}
