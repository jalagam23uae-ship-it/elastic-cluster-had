/**
 * Real-time Event Viewer Component
 */

import React, { useState } from 'react';
import { Activity, X, Trash2, Download, Filter } from 'lucide-react';
import { useServiceEvents } from '../../hooks/useWebSocket';
import { formatDistanceToNow } from 'date-fns';
import type { WebSocketEvent } from '../../types/websocket';
import Modal from '../common/Modal';

interface EventViewerProps {
  serviceName: string | null;
  isOpen: boolean;
  onClose: () => void;
}

const EVENT_COLORS = {
  CREATED: 'bg-green-100 text-green-800',
  UPDATED: 'bg-blue-100 text-blue-800',
  DELETED: 'bg-red-100 text-red-800',
  SUBSCRIBED: 'bg-purple-100 text-purple-800',
  CUSTOM: 'bg-gray-100 text-gray-800',
};

export function EventViewer({ serviceName, isOpen, onClose }: EventViewerProps) {
  const { events, clearEvents } = useServiceEvents(serviceName, isOpen);
  const [filter, setFilter] = useState<string>('ALL');
  const [autoScroll, setAutoScroll] = useState(true);
  const scrollRef = React.useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when new events arrive
  React.useEffect(() => {
    if (autoScroll && scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [events, autoScroll]);

  const filteredEvents =
    filter === 'ALL' ? events : events.filter((e) => e.event === filter);

  const handleExport = () => {
    const dataStr = JSON.stringify(filteredEvents, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${serviceName}-events-${Date.now()}.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  if (!isOpen) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Real-time Events" size="xl">
      <div className="flex flex-col h-[600px]">
        {/* Header Controls */}
        <div className="flex items-center justify-between mb-4 pb-3 border-b">
          <div className="flex items-center gap-2">
            <Activity className="w-5 h-5 text-indigo-600" />
            <h3 className="text-lg font-semibold">
              {serviceName ? `Events for ${serviceName}` : 'Select a service'}
            </h3>
            <span className="bg-gray-100 px-2 py-1 rounded text-sm text-gray-600">
              {filteredEvents.length} events
            </span>
          </div>

          <div className="flex items-center gap-2">
            {/* Auto-scroll toggle */}
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="checkbox"
                checked={autoScroll}
                onChange={(e) => setAutoScroll(e.target.checked)}
                className="rounded"
              />
              Auto-scroll
            </label>

            {/* Export button */}
            <button
              onClick={handleExport}
              className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded transition-colors"
              title="Export events"
              disabled={events.length === 0}
            >
              <Download className="w-4 h-4" />
            </button>

            {/* Clear button */}
            <button
              onClick={clearEvents}
              className="p-2 text-gray-600 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
              title="Clear events"
              disabled={events.length === 0}
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Filter Buttons */}
        <div className="flex items-center gap-2 mb-4">
          <Filter className="w-4 h-4 text-gray-500" />
          {['ALL', 'CREATED', 'UPDATED', 'DELETED'].map((eventType) => (
            <button
              key={eventType}
              onClick={() => setFilter(eventType)}
              className={`px-3 py-1 rounded text-sm font-medium transition-colors ${
                filter === eventType
                  ? 'bg-indigo-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {eventType}
            </button>
          ))}
        </div>

        {/* Events List */}
        <div
          ref={scrollRef}
          className="flex-1 overflow-y-auto space-y-2 bg-gray-50 rounded-lg p-4"
        >
          {filteredEvents.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-gray-500">
              <Activity className="w-12 h-12 mb-2 opacity-50" />
              <p className="text-sm">No events yet</p>
              <p className="text-xs">Events will appear here in real-time</p>
            </div>
          ) : (
            filteredEvents.map((event, index) => (
              <EventItem key={index} event={event} />
            ))
          )}
        </div>
      </div>
    </Modal>
  );
}

function EventItem({ event }: { event: WebSocketEvent }) {
  const [expanded, setExpanded] = useState(false);
  const timeAgo = event.timestamp
    ? formatDistanceToNow(event.timestamp, { addSuffix: true })
    : 'just now';

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-3 hover:shadow-sm transition-shadow">
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          {/* Event Type Badge */}
          <div className="flex items-center gap-2 mb-2">
            <span
              className={`px-2 py-0.5 rounded text-xs font-semibold ${
                EVENT_COLORS[event.event] || EVENT_COLORS.CUSTOM
              }`}
            >
              {event.event}
            </span>
            <span className="text-xs text-gray-500">{timeAgo}</span>
          </div>

          {/* Event Details */}
          <div className="text-sm space-y-1">
            <div className="flex items-center gap-2">
              <span className="text-gray-600">Service:</span>
              <span className="font-medium">{event.service}</span>
            </div>

            {event.entityType && (
              <div className="flex items-center gap-2">
                <span className="text-gray-600">Type:</span>
                <span className="font-medium">{event.entityType}</span>
              </div>
            )}

            {event.entityId && (
              <div className="flex items-center gap-2">
                <span className="text-gray-600">ID:</span>
                <span className="font-mono text-xs bg-gray-100 px-2 py-0.5 rounded">
                  {event.entityId}
                </span>
              </div>
            )}

            {/* Event Data (Expandable) */}
            {event.data && (
              <div className="mt-2">
                <button
                  onClick={() => setExpanded(!expanded)}
                  className="text-xs text-indigo-600 hover:text-indigo-700 font-medium"
                >
                  {expanded ? 'Hide' : 'Show'} data
                </button>
                {expanded && (
                  <pre className="mt-2 p-2 bg-gray-50 rounded text-xs overflow-x-auto">
                    {JSON.stringify(event.data, null, 2)}
                  </pre>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default EventViewer;
