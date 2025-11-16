/**
 * WebSocket Client Service using STOMP over WebSocket
 */

import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { API_CONFIG } from '../config/api';
import type {
  WebSocketMessage,
  WebSocketResponse,
  WebSocketEvent,
  WebSocketConnectionStatus,
  WebSocketSubscription,
  WebSocketStats,
} from '../types/websocket';

class WebSocketClient {
  private client: Client | null = null;
  private status: WebSocketConnectionStatus = 'DISCONNECTED';
  private subscriptions: Map<string, StompSubscription> = new Map();
  private messageCallbacks: Map<string, (response: WebSocketResponse) => void> = new Map();
  private stats: WebSocketStats = {
    messagesSent: 0,
    messagesReceived: 0,
    eventsReceived: 0,
    connectionUptime: 0,
  };
  private connectionTime: number = 0;
  private statusListeners: Set<(status: WebSocketConnectionStatus) => void> = new Set();

  /**
   * Initialize and connect to WebSocket
   */
  connect(): Promise<void> {
    if (this.status === 'CONNECTED' || this.status === 'CONNECTING') {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      try {
        this.status = 'CONNECTING';
        this.notifyStatusChange();

        this.client = new Client({
          brokerURL: API_CONFIG.WS_URL,
          connectHeaders: {},
          debug: (str) => {
            if (import.meta.env.DEV) {
              console.log('[WebSocket Debug]', str);
            }
          },
          reconnectDelay: 5000,
          heartbeatIncoming: 10000,
          heartbeatOutgoing: 10000,

          onConnect: () => {
            console.log('[WebSocket] Connected successfully');
            this.status = 'CONNECTED';
            this.connectionTime = Date.now();
            this.notifyStatusChange();

            // Subscribe to user-specific queue for responses
            this.subscribeToResponses();

            resolve();
          },

          onDisconnect: () => {
            console.log('[WebSocket] Disconnected');
            this.status = 'DISCONNECTED';
            this.notifyStatusChange();
          },

          onStompError: (frame) => {
            console.error('[WebSocket] STOMP error:', frame.headers['message'], frame.body);
            this.status = 'ERROR';
            this.stats.lastError = frame.headers['message'] || 'Unknown error';
            this.notifyStatusChange();
            reject(new Error(frame.headers['message'] || 'STOMP connection error'));
          },

          onWebSocketError: (event) => {
            console.error('[WebSocket] WebSocket error:', event);
            this.status = 'ERROR';
            this.stats.lastError = 'WebSocket connection error';
            this.notifyStatusChange();
            reject(event);
          },
        });

        this.client.activate();
      } catch (error) {
        console.error('[WebSocket] Connection failed:', error);
        this.status = 'ERROR';
        this.stats.lastError = error instanceof Error ? error.message : 'Unknown error';
        this.notifyStatusChange();
        reject(error);
      }
    });
  }

  /**
   * Disconnect from WebSocket
   */
  disconnect(): void {
    if (this.client && this.status !== 'DISCONNECTED') {
      this.status = 'DISCONNECTING';
      this.notifyStatusChange();

      // Unsubscribe from all topics
      this.subscriptions.forEach((sub) => sub.unsubscribe());
      this.subscriptions.clear();

      this.client.deactivate();
      this.client = null;
      this.status = 'DISCONNECTED';
      this.notifyStatusChange();
    }
  }

  /**
   * Subscribe to service events
   */
  subscribeToService(
    serviceName: string,
    callback: (event: WebSocketEvent) => void
  ): WebSocketSubscription {
    if (!this.client || this.status !== 'CONNECTED') {
      throw new Error('WebSocket not connected');
    }

    const destination = `/topic/service/${serviceName}`;
    const subscriptionId = `service-${serviceName}-${Date.now()}`;

    const stompSub = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const event: WebSocketEvent = JSON.parse(message.body);
        this.stats.eventsReceived++;
        callback(event);
      } catch (error) {
        console.error('[WebSocket] Failed to parse event:', error);
      }
    });

    this.subscriptions.set(subscriptionId, stompSub);

    return {
      id: subscriptionId,
      destination,
      callback,
      unsubscribe: () => {
        stompSub.unsubscribe();
        this.subscriptions.delete(subscriptionId);
      },
    };
  }

  /**
   * Subscribe to specific event types
   */
  subscribeToEventType(
    serviceName: string,
    eventType: 'created' | 'updated' | 'deleted',
    callback: (event: WebSocketEvent) => void
  ): WebSocketSubscription {
    if (!this.client || this.status !== 'CONNECTED') {
      throw new Error('WebSocket not connected');
    }

    const destination = `/topic/service/${serviceName}/${eventType}`;
    const subscriptionId = `${serviceName}-${eventType}-${Date.now()}`;

    const stompSub = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const event: WebSocketEvent = JSON.parse(message.body);
        this.stats.eventsReceived++;
        callback(event);
      } catch (error) {
        console.error('[WebSocket] Failed to parse event:', error);
      }
    });

    this.subscriptions.set(subscriptionId, stompSub);

    return {
      id: subscriptionId,
      destination,
      callback,
      unsubscribe: () => {
        stompSub.unsubscribe();
        this.subscriptions.delete(subscriptionId);
      },
    };
  }

  /**
   * Send operation message to service
   */
  sendMessage(message: WebSocketMessage): Promise<WebSocketResponse> {
    if (!this.client || this.status !== 'CONNECTED') {
      return Promise.reject(new Error('WebSocket not connected'));
    }

    const messageId = message.messageId || this.generateMessageId();
    const fullMessage: WebSocketMessage = {
      ...message,
      messageId,
      timestamp: Date.now(),
    };

    return new Promise((resolve, reject) => {
      // Store callback for response
      this.messageCallbacks.set(messageId, (response: WebSocketResponse) => {
        this.messageCallbacks.delete(messageId);
        if (response.success) {
          resolve(response);
        } else {
          reject(new Error(response.error || 'Operation failed'));
        }
      });

      // Set timeout
      setTimeout(() => {
        if (this.messageCallbacks.has(messageId)) {
          this.messageCallbacks.delete(messageId);
          reject(new Error('Message timeout'));
        }
      }, 30000); // 30 second timeout

      // Send message
      try {
        this.client!.publish({
          destination: `/app/service/${message.service}`,
          body: JSON.stringify(fullMessage),
        });
        this.stats.messagesSent++;
      } catch (error) {
        this.messageCallbacks.delete(messageId);
        reject(error);
      }
    });
  }

  /**
   * Convenience methods for CRUD operations
   */
  async create(serviceName: string, data: Record<string, any>): Promise<WebSocketResponse> {
    return this.sendMessage({
      service: serviceName,
      operation: 'CREATE',
      messageId: this.generateMessageId(),
      data,
    });
  }

  async get(serviceName: string, id: string): Promise<WebSocketResponse> {
    return this.sendMessage({
      service: serviceName,
      operation: 'GET',
      messageId: this.generateMessageId(),
      filter: { id },
    });
  }

  async list(serviceName: string): Promise<WebSocketResponse> {
    return this.sendMessage({
      service: serviceName,
      operation: 'LIST',
      messageId: this.generateMessageId(),
    });
  }

  async update(serviceName: string, data: Record<string, any>): Promise<WebSocketResponse> {
    return this.sendMessage({
      service: serviceName,
      operation: 'UPDATE',
      messageId: this.generateMessageId(),
      data,
    });
  }

  async delete(serviceName: string, id: string): Promise<WebSocketResponse> {
    return this.sendMessage({
      service: serviceName,
      operation: 'DELETE',
      messageId: this.generateMessageId(),
      filter: { id },
    });
  }

  /**
   * Subscribe to response queue
   */
  private subscribeToResponses(): void {
    if (!this.client) return;

    this.client.subscribe('/user/queue/reply', (message: IMessage) => {
      try {
        const response: WebSocketResponse = JSON.parse(message.body);
        this.stats.messagesReceived++;

        const callback = this.messageCallbacks.get(response.messageId);
        if (callback) {
          callback(response);
        }
      } catch (error) {
        console.error('[WebSocket] Failed to parse response:', error);
      }
    });
  }

  /**
   * Add status change listener
   */
  onStatusChange(listener: (status: WebSocketConnectionStatus) => void): () => void {
    this.statusListeners.add(listener);
    // Return unsubscribe function
    return () => {
      this.statusListeners.delete(listener);
    };
  }

  /**
   * Notify all status listeners
   */
  private notifyStatusChange(): void {
    this.statusListeners.forEach((listener) => listener(this.status));
  }

  /**
   * Get current connection status
   */
  getStatus(): WebSocketConnectionStatus {
    return this.status;
  }

  /**
   * Get statistics
   */
  getStats(): WebSocketStats {
    return {
      ...this.stats,
      connectionUptime: this.connectionTime ? Date.now() - this.connectionTime : 0,
    };
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.status === 'CONNECTED';
  }

  /**
   * Generate unique message ID
   */
  private generateMessageId(): string {
    return `msg-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}

// Export singleton instance
export const websocketClient = new WebSocketClient();

export default websocketClient;
