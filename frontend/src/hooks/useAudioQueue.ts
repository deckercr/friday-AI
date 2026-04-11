import { useRef, useCallback } from 'react'

export function useAudioQueue() {
  const contextRef = useRef<AudioContext | null>(null)
  const queue = useRef<ArrayBuffer[]>([])
  const playing = useRef(false)

  function getContext() {
    if (!contextRef.current) {
      contextRef.current = new AudioContext()
    }
    return contextRef.current
  }

  async function playNext() {
    if (playing.current || queue.current.length === 0) return
    playing.current = true
    const buf = queue.current.shift()!
    const ctx = getContext()
    const decoded = await ctx.decodeAudioData(buf)
    const source = ctx.createBufferSource()
    source.buffer = decoded
    source.connect(ctx.destination)
    source.onended = () => {
      playing.current = false
      playNext()
    }
    source.start()
  }

  const enqueue = useCallback((data: ArrayBuffer) => {
    queue.current.push(data)
    playNext()
  }, [])

  const clear = useCallback(() => {
    queue.current = []
  }, [])

  return { enqueue, clear }
}
