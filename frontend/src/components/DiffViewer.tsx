interface Props { diff: string }

export default function DiffViewer({ diff }: Props) {
  const lines = diff.split('\n')

  return (
    <div className="font-mono text-sm overflow-x-auto">
      {lines.map((line, i) => {
        const bg = line.startsWith('+') && !line.startsWith('+++')
          ? 'bg-green-900/40 text-green-300'
          : line.startsWith('-') && !line.startsWith('---')
          ? 'bg-red-900/40 text-red-300'
          : line.startsWith('@@')
          ? 'bg-blue-900/40 text-blue-300'
          : 'text-gray-400'
        return (
          <div key={i} className={`px-4 py-0.5 ${bg} whitespace-pre`}>
            {line || ' '}
          </div>
        )
      })}
    </div>
  )
}
