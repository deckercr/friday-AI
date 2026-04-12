import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/authStore'

export function useWebSocket() {
  const clientRef = useRef<Client | null>(null)
  const { accessToken } = useAuthStore()

  useEffect(() => {
    if (!accessToken) return

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 3000,
    })
    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
      clientRef.current = null
    }
  }, [accessToken])

  const subscribe = useCallback((destination: string, callback: (body: string | Uint8Array) => void) => {
    const client = clientRef.current
    if (!client?.connected) return () => {}
    const sub = client.subscribe(destination, (frame) => {
      callback(frame.isBinaryBody ? frame.binaryBody : frame.body)
    })
    return () => sub.unsubscribe()
  }, [])

  const send = useCallback((destination: string, body: object) => {
    const client = clientRef.current
    if (!client?.connected) return
    client.publish({ destination, body: JSON.stringify(body) })
  }, [])

  return { subscribe, send }
}
