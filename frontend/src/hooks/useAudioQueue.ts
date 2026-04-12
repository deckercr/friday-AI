import { useRef, useCallback } from 'react'

export function useAudioQueue() {
  const contextRef = useRef<AudioContext | null>(null)
  const queue = useRef<ArrayBuffer[]>([])
  const playing = useRef(false)
  const currentSource = useRef<AudioBufferSourceNode | null>(null)
  const clearGen = useRef(0)

  function getContext() {
    if (!contextRef.current) {
      contextRef.current = new AudioContext()
    }
    return contextRef.current
  }

  async function playNext(gen: number) {
    if (playing.current || queue.current.length === 0) return
    playing.current = true
    const buf = queue.current.shift()!
    const ctx = getContext()
    let decoded: AudioBuffer
    try {
      decoded = await ctx.decodeAudioData(buf)
    } catch {
      // Bad chunk — drop it and continue draining the queue.
      playing.current = false
      playNext(gen)
      return
    }
    // A clear() may have fired while decodeAudioData was awaited — bail out.
    if (gen !== clearGen.current) {
      playing.current = false
      return
    }
    const source = ctx.createBufferSource()
    currentSource.current = source
    source.buffer = decoded
    source.connect(ctx.destination)
    source.onended = () => {
      playing.current = false
      currentSource.current = null
      playNext(gen)
    }
    source.start()
  }

  const enqueue = useCallback((data: ArrayBuffer) => {
    queue.current.push(data)
    playNext(clearGen.current)
  }, [])

  const clear = useCallback(() => {
    clearGen.current += 1
    queue.current = []
    if (currentSource.current) {
      try { currentSource.current.stop() } catch { /* already stopped */ }
      currentSource.current = null
    }
    playing.current = false
  }, [])

  return { enqueue, clear }
}
