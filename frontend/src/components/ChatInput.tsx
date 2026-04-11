import { useState } from 'react'

interface Props { onSend: (content: string) => void; disabled?: boolean }

export default function ChatInput({ onSend, disabled }: Props) {
  const [value, setValue] = useState('')

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      if (value.trim()) {
        onSend(value.trim())
        setValue('')
      }
    }
  }

  return (
    <div className="border-t border-gray-700 p-4">
      <textarea
        value={value}
        onChange={e => setValue(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        placeholder="Message Friday... (Enter to send, Shift+Enter for newline)"
        rows={3}
        className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
      />
    </div>
  )
}
