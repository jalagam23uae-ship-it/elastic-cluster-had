/**
 * WebSocket Types for STOMP Messaging
 */

export interface WebSocketMessage {
  service: string;
  operation: 'CREATE' | 'GET' | 'UPDATE' | 'DELETE' | 'LIST';
  messageId: string;
  data?: Record<string, any>;
  filter?: Record<string, any>;
  timestamp?: number;
  topic?: string;
}

export interface WebSocketResponse {
  messageId: string;
  success: boolean;
  data?: any;
  error?: string;
  statusCode?: number;
  service?: string;
  operation?: string;
  timestamp?: number;
}

export interface WebSocketEvent {
  event: 'CREATED' | 'UPDATED' | 'DELETED' | 'SUBSCRIBED' | 'CUSTOM';
  service: string;
  entityType?: string;
  entityId?: string;
  data?: any;
  timestamp?: number;
  triggeredBy?: string;
  metadata?: any;
}

export type WebSocketConnectionStatus =
  | 'CONNECTING'
  | 'CONNECTED'
  | 'DISCONNECTING'
  | 'DISCONNECTED'
  | 'ERROR';

export interface WebSocketConfig {
  brokerURL: string;
  reconnectDelay?: number;
  heartbeatIncoming?: number;
  heartbeatOutgoing?: number;
  debug?: boolean;
}

export interface WebSocketSubscription {
  id: string;
  destination: string;
  callback: (event: WebSocketEvent) => void;
  unsubscribe: () => void;
}

export interface WebSocketStats {
  messagesSent: number;
  messagesReceived: number;
  eventsReceived: number;
  connectionUptime: number;
  lastError?: string;
}
