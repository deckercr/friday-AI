import { create } from 'zustand'

interface ReviewState {
  pendingDiff: string | null
  sessionId: string | null
  setPendingReview: (sessionId: string, diff: string) => void
  clearReview: () => void
}

export const useReviewStore = create<ReviewState>((set) => ({
  pendingDiff: null,
  sessionId: null,
  setPendingReview: (sessionId, diff) => set({ sessionId, pendingDiff: diff }),
  clearReview: () => set({ pendingDiff: null, sessionId: null }),
}))
