import { Link } from 'react-router-dom'
import { useReviewStore } from '../store/reviewStore'
import { useAuth } from '../hooks/useAuth'

export default function NavBar() {
  const pendingDiff = useReviewStore(s => s.pendingDiff)
  const { logout } = useAuth()

  return (
    <nav className="bg-gray-800 border-b border-gray-700 px-4 py-3 flex items-center justify-between">
      <div className="flex items-center gap-6">
        <span className="text-white font-bold text-lg">Friday AI</span>
        <Link to="/chat" className="text-gray-300 hover:text-white text-sm">Chat</Link>
        <Link to="/review" className="text-gray-300 hover:text-white text-sm flex items-center gap-1">
          Review
          {pendingDiff && (
            <span className="bg-orange-500 text-white text-xs rounded-full px-1.5 py-0.5" aria-label="1 pending review">
              <span aria-hidden="true">1</span>
            </span>
          )}
        </Link>
      </div>
      <button onClick={logout} className="text-gray-400 hover:text-white text-sm">Sign out</button>
    </nav>
  )
}
