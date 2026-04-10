import axios from 'axios'
import type { AuthResponse } from '../types'

const api = axios.create({ baseURL: '', withCredentials: true })

export async function login(username: string, password: string): Promise<AuthResponse> {
  const res = await api.post<AuthResponse>('/auth/login', { username, password })
  return res.data
}

export async function refresh(): Promise<AuthResponse> {
  const res = await api.post<AuthResponse>('/auth/refresh')
  return res.data
}

export async function logout(): Promise<void> {
  await api.post('/auth/logout')
}
