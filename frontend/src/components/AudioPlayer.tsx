import { useEffect } from 'react'
import { useAudioQueue } from '../hooks/useAudioQueue'

interface Props {
  sessionId: string
  subscribe: (dest: string, cb: (data: string | Uint8Array) => void) => () => void
  muted: boolean
}

export default function AudioPlayer({ sessionId, subscribe, muted }: Props) {
  const { enqueue, clear } = useAudioQueue()

  useEffect(() => {
    clear()
    const unsub = subscribe(`/topic/audio/${sessionId}`, (data) => {
      if (!muted && data instanceof Uint8Array) {
        enqueue(data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength) as ArrayBuffer)
      }
    })
    return unsub
  }, [sessionId, muted])

  return null // invisible component
}
