import { useState, useEffect, useRef } from 'react'
import NavBar from '../components/NavBar'
import ChatMessage from '../components/ChatMessage'
import ChatInput from '../components/ChatInput'
import AudioPlayer from '../components/AudioPlayer'
import { useWebSocket } from '../hooks/useWebSocket'
import { useReviewStore } from '../store/reviewStore'
import { getSessions, createSession, getMessages } from '../api/chat'
import type { Message, ChatSession } from '../types'

export default function ChatPage() {
  const [sessions, setSessions] = useState<ChatSession[]>([])
  const [activeSession, setActiveSession] = useState<ChatSession | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [streaming, setStreaming] = useState(false)
  const [muted, setMuted] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)
  const { subscribe, send } = useWebSocket()
  const { setPendingReview } = useReviewStore()

  useEffect(() => {
    getSessions().then(s => {
      setSessions(s)
      if (s.length > 0) loadSession(s[0])
    })
  }, [])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  async function loadSession(session: ChatSession) {
    setActiveSession(session)
    const msgs = await getMessages(session.id)
    setMessages(msgs)
  }

  useEffect(() => {
    if (!activeSession) return

    let assistantContent = ''
    const streamingId = `streaming-${crypto.randomUUID()}`

    const unsub = subscribe(`/topic/chat/${activeSession.id}`, (data) => {
      const token = data as string
      if (token === '[DONE]') {
        setStreaming(false)
        return
      }
      assistantContent += token
      setMessages(prev => {
        const last = prev[prev.length - 1]
        if (last?.role === 'assistant' && last.id === streamingId) {
          return [...prev.slice(0, -1), { ...last, content: assistantContent }]
        }
        return [...prev, {
          id: streamingId, sessionId: activeSession.id,
          role: 'assistant', content: assistantContent, createdAt: new Date().toISOString()
        }]
      })
    })

    const reviewUnsub = subscribe(`/topic/review/${activeSession.id}`, (data) => {
      const { diff } = JSON.parse(data as string)
      setPendingReview(activeSession.id, diff)
    })

    return () => {
      unsub()
      reviewUnsub()
      setStreaming(false)
    }
  }, [activeSession])

  function handleSend(content: string) {
    if (!activeSession || streaming) return
    setStreaming(true)
    setMessages(prev => [...prev, {
      id: crypto.randomUUID(), sessionId: activeSession.id,
      role: 'user', content, createdAt: new Date().toISOString()
    }])
    send('/app/chat.send', { sessionId: activeSession.id, content })
  }

  async function handleNewSession() {
    const session = await createSession('New chat')
    setSessions(prev => [session, ...prev])
    setActiveSession(session)
    setMessages([])
  }

  return (
    <div className="flex flex-col h-screen bg-gray-900">
      <NavBar />
      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar */}
        <div className="w-64 bg-gray-850 border-r border-gray-700 flex flex-col p-3 gap-2 overflow-y-auto">
          <button onClick={handleNewSession}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white rounded px-3 py-2 text-sm">
            + New Chat
          </button>
          {sessions.map(s => (
            <button key={s.id} onClick={() => loadSession(s)}
              className={`text-left rounded px-3 py-2 text-sm truncate ${
                activeSession?.id === s.id
                  ? 'bg-gray-700 text-white' : 'text-gray-400 hover:bg-gray-800'
              }`}>
              {s.title || 'Untitled'}
            </button>
          ))}
        </div>

        {/* Chat area */}
        <div className="flex flex-col flex-1 overflow-hidden">
          <div className="flex items-center justify-end px-4 py-2 border-b border-gray-700">
            <label className="text-gray-400 text-sm flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={muted} onChange={e => setMuted(e.target.checked)}
                className="rounded" />
              Mute TTS
            </label>
          </div>
          <div className="flex-1 overflow-y-auto p-4">
            {messages.map(m => <ChatMessage key={m.id} message={m} />)}
            <div ref={bottomRef} />
          </div>
          <ChatInput onSend={handleSend} disabled={streaming} />
        </div>
      </div>

      {activeSession && (
        <AudioPlayer sessionId={activeSession.id} subscribe={subscribe} muted={muted} />
      )}
    </div>
  )
}
