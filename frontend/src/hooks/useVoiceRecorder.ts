import { useRef, useState, useCallback } from 'react'

export type RecorderState = 'idle' | 'recording' | 'processing'

interface UseVoiceRecorder {
  state: RecorderState
  startRecording: () => Promise<void>
  stopRecording: () => Promise<Blob>
}

export function useVoiceRecorder(): UseVoiceRecorder {
  const [state, setState] = useState<RecorderState>('idle')
  const mediaRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<BlobPart[]>([])
  const resolveRef = useRef<((blob: Blob) => void) | null>(null)
  const rejectRef = useRef<((err: Error) => void) | null>(null)

  const startRecording = useCallback(async () => {
    if (mediaRef.current) return  // already recording
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const recorder = new MediaRecorder(stream, { mimeType: preferredMimeType() })
    chunksRef.current = []

    recorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunksRef.current.push(e.data)
    }

    recorder.onstop = () => {
      stream.getTracks().forEach(t => t.stop())
      const blob = new Blob(chunksRef.current, { type: recorder.mimeType })
      resolveRef.current?.(blob)
      resolveRef.current = null
      rejectRef.current = null
      setState('idle')
    }

    recorder.onerror = (event) => {
      stream.getTracks().forEach(t => t.stop())
      rejectRef.current?.(new Error(`Recording error: ${(event as MediaRecorderErrorEvent).error?.message ?? 'unknown'}`))
      resolveRef.current = null
      rejectRef.current = null
      setState('idle')
    }

    recorder.start()
    mediaRef.current = recorder
    setState('recording')
  }, [])

  const stopRecording = useCallback((): Promise<Blob> => {
    if (!mediaRef.current) {
      return Promise.reject(new Error('Not currently recording'))
    }
    return new Promise((resolve, reject) => {
      resolveRef.current = resolve
      rejectRef.current = reject
      setState('processing')
      mediaRef.current?.stop()
      mediaRef.current = null
    })
  }, [])

  return { state, startRecording, stopRecording }
}

/** Pick the best supported audio format. */
function preferredMimeType(): string {
  const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', 'audio/mp4']
  return candidates.find(t => MediaRecorder.isTypeSupported(t)) ?? ''
}
