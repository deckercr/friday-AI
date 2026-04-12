export interface User {
  username: string;
  role: string;
}

export interface AuthResponse {
  accessToken: string;
}

export interface ChatSession {
  id: string;
  title: string;
  createdAt: string;
}

export interface Message {
  id: string;
  sessionId: string;
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
}

export interface DiffFile {
  path: string;
  content: string;
}

export interface ReviewNotification {
  sessionId: string;
  diff: string;
}
