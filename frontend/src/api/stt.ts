import { useAuthStore } from '../store/authStore'

const BASE = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * POST audio blob to the backend STT proxy.
 * Returns the transcription string.
 * @throws {Error} if the HTTP response is not OK
 */
export async function transcribe(blob: Blob): Promise<string> {
  const token = useAuthStore.getState().accessToken
  const form = new FormData()
  form.append('audio', blob, 'audio.webm')

  const res = await fetch(`${BASE}/api/stt/transcribe`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: form,
  })

  if (!res.ok) throw new Error(`STT request failed: ${res.status}`)
  const data = await res.json() as { text: string }
  return data.text
}
