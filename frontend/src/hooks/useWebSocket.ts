/**
 * React Hook for WebSocket functionality
 */

import { useEffect, useState, useCallback } from 'react';
import { websocketClient } from '../api/websocketClient';
import type {
  WebSocketConnectionStatus,
  WebSocketEvent,
  WebSocketResponse,
  WebSocketStats,
} from '../types/websocket';

export function useWebSocket() {
  const [status, setStatus] = useState<WebSocketConnectionStatus>('DISCONNECTED');
  const [stats, setStats] = useState<WebSocketStats>({
    messagesSent: 0,
    messagesReceived: 0,
    eventsReceived: 0,
    connectionUptime: 0,
  });

  useEffect(() => {
    // Subscribe to status changes
    const unsubscribe = websocketClient.onStatusChange((newStatus) => {
      setStatus(newStatus);
      setStats(websocketClient.getStats());
    });

    // Initialize status
    setStatus(websocketClient.getStatus());

    // Cleanup
    return () => {
      unsubscribe();
    };
  }, []);

  const connect = useCallback(async () => {
    try {
      await websocketClient.connect();
    } catch (error) {
      console.error('[useWebSocket] Connection failed:', error);
      throw error;
    }
  }, []);

  const disconnect = useCallback(() => {
    websocketClient.disconnect();
  }, []);

  const sendMessage = useCallback(
    async (
      serviceName: string,
      operation: 'CREATE' | 'GET' | 'UPDATE' | 'DELETE' | 'LIST',
      data?: Record<string, any>,
      filter?: Record<string, any>
    ): Promise<WebSocketResponse> => {
      return websocketClient.sendMessage({
        service: serviceName,
        operation,
        messageId: `msg-${Date.now()}`,
        data,
        filter,
      });
    },
    []
  );

  return {
    status,
    stats,
    isConnected: status === 'CONNECTED',
    connect,
    disconnect,
    sendMessage,
    client: websocketClient,
  };
}

export function useServiceEvents(serviceName: string | null, enabled: boolean = true) {
  const [events, setEvents] = useState<WebSocketEvent[]>([]);
  const { isConnected } = useWebSocket();

  useEffect(() => {
    if (!serviceName || !enabled || !isConnected) {
      return;
    }

    const subscription = websocketClient.subscribeToService(serviceName, (event) => {
      setEvents((prev) => [...prev, event].slice(-100)); // Keep last 100 events
    });

    return () => {
      subscription.unsubscribe();
    };
  }, [serviceName, enabled, isConnected]);

  const clearEvents = useCallback(() => {
    setEvents([]);
  }, []);

  return { events, clearEvents };
}
