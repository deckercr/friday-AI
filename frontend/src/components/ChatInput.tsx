import { useState } from 'react'
import { transcribe } from '../api/stt'
import { useVoiceRecorder } from '../hooks/useVoiceRecorder'

interface Props {
  onSend: (content: string) => void
  disabled?: boolean
}

export default function ChatInput({ onSend, disabled }: Props) {
  const [value, setValue] = useState('')
  const { state: recState, startRecording, stopRecording } = useVoiceRecorder()
  const [sttError, setSttError] = useState<string | null>(null)

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      submitText(value)
    }
  }

  function submitText(text: string) {
    const trimmed = text.trim()
    if (!trimmed) return
    onSend(trimmed)
    setValue('')
  }

  async function handleMicClick() {
    setSttError(null)
    if (recState === 'recording') {
      try {
        const blob = await stopRecording()
        const text = await transcribe(blob)
        if (text) {
          onSend(text)
          setValue('')
        }
      } catch (err) {
        setSttError('Transcription failed. Please try again.')
        console.error('STT error:', err)
      }
    } else {
      try {
        await startRecording()
      } catch {
        setSttError('Microphone access denied.')
      }
    }
  }

  const isRecording = recState === 'recording'
  const isProcessing = recState === 'processing'
  const micDisabled = disabled || isProcessing

  return (
    <div className="border-t border-gray-700 p-4">
      <textarea
        value={value}
        onChange={e => setValue(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled || isRecording || isProcessing}
        placeholder={
          isRecording ? 'Listening...' :
          isProcessing ? 'Transcribing...' :
          'Message Friday... (Enter to send, Shift+Enter for newline)'
        }
        rows={3}
        className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
      />

      {sttError && (
        <p className="text-red-400 text-xs mt-1">{sttError}</p>
      )}

      {/* Toolbar row */}
      <div className="flex justify-between items-center mt-2">

        {/* Left: mic toggle */}
        {isRecording ? (
          <div className="flex items-center gap-2">
            <button
              onClick={handleMicClick}
              title="Stop recording"
              className="w-8 h-8 rounded-full bg-red-500 hover:bg-red-600 text-white flex items-center justify-center text-sm font-bold shrink-0"
            >
              ■
            </button>
            <WaveformBars />
          </div>
        ) : (
          <button
            onClick={handleMicClick}
            disabled={micDisabled}
            title={isProcessing ? 'Transcribing...' : 'Start voice input'}
            className="flex items-center gap-2 px-3 py-1.5 rounded-md bg-gray-700 hover:bg-gray-600 text-gray-300 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span>🎙</span>
            <span>{isProcessing ? 'Transcribing...' : 'Voice input'}</span>
          </button>
        )}

        {/* Right: send */}
        <button
          onClick={() => submitText(value)}
          disabled={disabled || !value.trim() || isRecording || isProcessing}
          className="px-4 py-1.5 rounded-md bg-blue-600 hover:bg-blue-700 text-white text-sm disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Send
        </button>
      </div>
    </div>
  )
}

/** Animated waveform bars shown during recording. */
function WaveformBars() {
  const heights = [12, 20, 8, 16, 10, 18]
  return (
    <div className="flex items-center gap-0.5">
      {heights.map((h, i) => (
        <div
          key={i}
          style={{
            width: 3,
            height: h,
            animationDelay: `${i * 80}ms`,
            animationDuration: '600ms',
          }}
          className="bg-red-400 rounded-sm animate-pulse"
        />
      ))}
    </div>
  )
}
