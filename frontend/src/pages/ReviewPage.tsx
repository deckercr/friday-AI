import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import NavBar from '../components/NavBar'
import DiffViewer from '../components/DiffViewer'
import { useReviewStore } from '../store/reviewStore'
import axios from 'axios'
import { useAuthStore } from '../store/authStore'

export default function ReviewPage() {
  const { pendingDiff, sessionId, clearReview } = useReviewStore()
  const { accessToken } = useAuthStore()
  const [loading, setLoading] = useState(false)
  const [prUrl, setPrUrl] = useState<string | null>(null)
  const [branchName, setBranchName] = useState('friday/my-change')
  const [prTitle, setPrTitle] = useState('Friday AI: proposed changes')
  const navigate = useNavigate()

  async function handleApprove() {
    setLoading(true)
    try {
      const res = await axios.post('/api/review/approve',
        { branchName, title: prTitle, sessionId },
        { headers: { Authorization: `Bearer ${accessToken}` } }
      )
      setPrUrl(res.data.prUrl)
      clearReview()
    } catch {
      alert('Failed to create PR')
    } finally {
      setLoading(false)
    }
  }

  async function handleReject() {
    try {
      await axios.post('/api/review/reject', { sessionId },
        { headers: { Authorization: `Bearer ${accessToken}` } })
    } finally {
      clearReview()
      navigate('/chat')
    }
  }

  if (!pendingDiff && !prUrl) {
    return (
      <div className="flex flex-col h-screen bg-gray-900">
        <NavBar />
        <div className="flex-1 flex items-center justify-center text-gray-500">
          No pending changes to review.
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-screen bg-gray-900">
      <NavBar />
      <div className="flex-1 overflow-y-auto p-6">
        <h1 className="text-white text-2xl font-bold mb-4">Code Review</h1>

        {prUrl ? (
          <div className="bg-green-900/30 border border-green-700 rounded-lg p-6 text-center">
            <p className="text-green-300 text-lg mb-3">PR created successfully!</p>
            <a href={prUrl} target="_blank" rel="noopener noreferrer"
              className="text-blue-400 hover:text-blue-300 underline">
              {prUrl}
            </a>
          </div>
        ) : (
          <>
            <div className="bg-gray-800 rounded-lg overflow-hidden mb-6">
              <DiffViewer diff={pendingDiff!} />
            </div>

            <div className="grid grid-cols-2 gap-4 mb-6">
              <div>
                <label className="text-gray-400 text-sm block mb-1">Branch name</label>
                <input
                  value={branchName}
                  onChange={e => setBranchName(e.target.value)}
                  className="w-full bg-gray-800 text-white rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="text-gray-400 text-sm block mb-1">PR title</label>
                <input
                  value={prTitle}
                  onChange={e => setPrTitle(e.target.value)}
                  className="w-full bg-gray-800 text-white rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div className="flex gap-4">
              <button onClick={handleApprove} disabled={loading}
                className="bg-green-600 hover:bg-green-700 text-white px-6 py-2 rounded font-medium disabled:opacity-50">
                {loading ? 'Creating PR...' : 'Approve & Push PR'}
              </button>
              <button onClick={handleReject}
                className="bg-red-600 hover:bg-red-700 text-white px-6 py-2 rounded font-medium">
                Reject Changes
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
