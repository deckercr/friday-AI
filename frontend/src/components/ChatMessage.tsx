import ReactMarkdown from 'react-markdown'
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism'
import type { Message } from '../types'

interface Props { message: Message }

export default function ChatMessage({ message }: Props) {
  const isUser = message.role === 'user'

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`}>
      <div className={`max-w-3xl rounded-lg px-4 py-3 ${
        isUser ? 'bg-blue-600 text-white' : 'bg-gray-800 text-gray-100'
      }`}>
        {isUser ? (
          <p className="whitespace-pre-wrap">{message.content}</p>
        ) : (
          <ReactMarkdown
            components={{
              code({ className, children, ...props }) {
                const match = /language-(\w+)/.exec(className || '')
                return match ? (
                  <SyntaxHighlighter style={oneDark} language={match[1]} PreTag="div">
                    {String(children).replace(/\n$/, '')}
                  </SyntaxHighlighter>
                ) : (
                  <code className="bg-gray-700 rounded px-1 text-sm" {...props}>
                    {children}
                  </code>
                )
              }
            }}
          >
            {message.content}
          </ReactMarkdown>
        )}
      </div>
    </div>
  )
}
