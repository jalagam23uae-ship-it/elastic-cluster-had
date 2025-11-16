/**
 * WebSocket Status Indicator Component
 */

import React from 'react';
import { Wifi, WifiOff, Loader2, AlertCircle } from 'lucide-react';
import { useWebSocket } from '../../hooks/useWebSocket';
import type { WebSocketConnectionStatus } from '../../types/websocket';

interface StatusConfig {
  icon: React.ReactNode;
  color: string;
  bgColor: string;
  text: string;
}

const STATUS_CONFIG: Record<WebSocketConnectionStatus, StatusConfig> = {
  CONNECTED: {
    icon: <Wifi className="w-4 h-4" />,
    color: 'text-green-600',
    bgColor: 'bg-green-100',
    text: 'Connected',
  },
  CONNECTING: {
    icon: <Loader2 className="w-4 h-4 animate-spin" />,
    color: 'text-blue-600',
    bgColor: 'bg-blue-100',
    text: 'Connecting...',
  },
  DISCONNECTING: {
    icon: <Loader2 className="w-4 h-4 animate-spin" />,
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    text: 'Disconnecting...',
  },
  DISCONNECTED: {
    icon: <WifiOff className="w-4 h-4" />,
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    text: 'Disconnected',
  },
  ERROR: {
    icon: <AlertCircle className="w-4 h-4" />,
    color: 'text-red-600',
    bgColor: 'bg-red-100',
    text: 'Error',
  },
};

interface WebSocketStatusProps {
  showText?: boolean;
  showStats?: boolean;
  className?: string;
}

export function WebSocketStatus({
  showText = true,
  showStats = false,
  className = '',
}: WebSocketStatusProps) {
  const { status, stats, connect, disconnect, isConnected } = useWebSocket();
  const config = STATUS_CONFIG[status];

  const handleToggle = async () => {
    if (isConnected) {
      disconnect();
    } else {
      try {
        await connect();
      } catch (error) {
        console.error('Failed to connect:', error);
      }
    }
  };

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      {/* Status Indicator */}
      <button
        onClick={handleToggle}
        className={`flex items-center gap-2 px-3 py-1.5 rounded-lg transition-colors ${config.bgColor} hover:opacity-80`}
        title={`WebSocket ${config.text}`}
      >
        <span className={config.color}>{config.icon}</span>
        {showText && <span className={`text-sm font-medium ${config.color}`}>{config.text}</span>}
      </button>

      {/* Stats (Optional) */}
      {showStats && isConnected && (
        <div className="text-xs text-gray-600 flex gap-3">
          <span title="Messages Sent">📤 {stats.messagesSent}</span>
          <span title="Messages Received">📥 {stats.messagesReceived}</span>
          <span title="Events Received">🔔 {stats.eventsReceived}</span>
        </div>
      )}
    </div>
  );
}

export default WebSocketStatus;
