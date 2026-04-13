import { useAuthStore } from '../store/authStore'

const BASE = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * POST audio blob to the backend STT proxy.
 * Returns the transcription string.
 * @throws {Error} if the HTTP response is not OK
 */
function filenameForBlob(blob: Blob): string {
  const ext: Record<string, string> = {
    'audio/webm': 'audio.webm',
    'audio/ogg': 'audio.ogg',
    'audio/mp4': 'audio.mp4',
    'audio/wav': 'audio.wav',
  }
  const base = blob.type.split(';')[0].trim()
  return ext[base] ?? 'audio.webm'
}

export async function transcribe(blob: Blob): Promise<string> {
  const token = useAuthStore.getState().accessToken
  const form = new FormData()
  form.append('audio', blob, filenameForBlob(blob))

  const res = await fetch(`${BASE}/api/stt/transcribe`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: form,
  })

  if (!res.ok) throw new Error(`STT request failed: ${res.status}`)
  const data = await res.json() as { text: string }
  return data.text
}
