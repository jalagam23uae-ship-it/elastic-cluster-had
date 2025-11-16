import React, { useState } from 'react';
import { Send, RefreshCw, AlertCircle, CheckCircle } from 'lucide-react';

interface ActiveMQTesterProps {
  serviceName: string;
}

export function ActiveMQTester({ serviceName }: ActiveMQTesterProps) {
  const [operation, setOperation] = useState<'CREATE' | 'GET' | 'UPDATE' | 'DELETE' | 'LIST'>('CREATE');
  const [data, setData] = useState('{\n  "field1": "value1",\n  "field2": "value2"\n}');
  const [messageType, setMessageType] = useState<'queue' | 'topic'>('queue');
  const [result, setResult] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const sendMessage = async () => {
    setLoading(true);
    setError(null);
    setSuccess(false);

    try {
      let parsedData;
      try {
        parsedData = JSON.parse(data);
      } catch (e) {
        throw new Error('Invalid JSON in data field');
      }

      const message = {
        service: serviceName,
        operation,
        data: parsedData,
        timestamp: Date.now(),
        messageId: `msg-${Date.now()}`
      };

      // Send to backend endpoint that publishes to ActiveMQ
      const endpoint = messageType === 'queue'
        ? `/api/activemq/${serviceName}/queue`
        : `/api/activemq/${serviceName}/topic`;

      const response = await fetch(`http://localhost:8080${endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(message),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();
      setResult(JSON.stringify(result, null, 2));
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to send message');
    } finally {
      setLoading(false);
    }
  };

  const loadSampleData = (op: typeof operation) => {
    setOperation(op);
    switch (op) {
      case 'CREATE':
        setData('{\n  "name": "New Item",\n  "description": "Sample description"\n}');
        break;
      case 'GET':
        setData('{\n  "id": "1"\n}');
        break;
      case 'UPDATE':
        setData('{\n  "id": "1",\n  "name": "Updated Item"\n}');
        break;
      case 'DELETE':
        setData('{\n  "id": "1"\n}');
        break;
      case 'LIST':
        setData('{}');
        break;
    }
  };

  const queueName = `service.${serviceName}`;
  const topicName = `events.${serviceName}`;

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-gray-900 mb-2">ActiveMQ Message Tester</h3>
        <p className="text-sm text-gray-600">
          Send JMS messages to queues or topics for {serviceName} service
        </p>
      </div>

      {/* Message Type Selection */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Message Type
        </label>
        <div className="flex gap-4">
          <label className="flex items-center">
            <input
              type="radio"
              name="messageType"
              value="queue"
              checked={messageType === 'queue'}
              onChange={(e) => setMessageType(e.target.value as 'queue')}
              className="mr-2"
            />
            <span className="text-sm">
              Queue (Point-to-Point)
              <span className="text-gray-500 ml-2 font-mono text-xs">{queueName}</span>
            </span>
          </label>
          <label className="flex items-center">
            <input
              type="radio"
              name="messageType"
              value="topic"
              checked={messageType === 'topic'}
              onChange={(e) => setMessageType(e.target.value as 'topic')}
              className="mr-2"
            />
            <span className="text-sm">
              Topic (Pub/Sub)
              <span className="text-gray-500 ml-2 font-mono text-xs">{topicName}</span>
            </span>
          </label>
        </div>
      </div>

      {/* Operation Selection */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Operation
        </label>
        <div className="flex gap-2">
          {(['CREATE', 'GET', 'UPDATE', 'DELETE', 'LIST'] as const).map((op) => (
            <button
              key={op}
              onClick={() => loadSampleData(op)}
              className={`px-3 py-1 text-sm rounded ${
                operation === op
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 hover:bg-gray-200 text-gray-700'
              }`}
            >
              {op}
            </button>
          ))}
        </div>
      </div>

      {/* Data Editor */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Message Data (JSON)
        </label>
        <textarea
          value={data}
          onChange={(e) => setData(e.target.value)}
          className="w-full h-48 px-3 py-2 border border-gray-300 rounded-md font-mono text-sm focus:ring-blue-500 focus:border-blue-500"
          placeholder='{"field": "value"}'
        />
      </div>

      {/* Send Button */}
      <button
        onClick={sendMessage}
        disabled={loading}
        className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
      >
        <Send className="w-4 h-4" />
        {loading ? 'Sending...' : `Send to ${messageType === 'queue' ? 'Queue' : 'Topic'}`}
      </button>

      {/* Success Display */}
      {success && !error && (
        <div className="mt-4 p-4 bg-green-50 border border-green-200 rounded-md flex items-start gap-2">
          <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-green-800">Message Sent Successfully</p>
            <p className="text-sm text-green-700">
              Sent {operation} message to {messageType === 'queue' ? queueName : topicName}
            </p>
          </div>
        </div>
      )}

      {/* Error Display */}
      {error && (
        <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-md flex items-start gap-2">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-red-800">Error</p>
            <p className="text-sm text-red-700">{error}</p>
          </div>
        </div>
      )}

      {/* Result Display */}
      {result && (
        <div className="mt-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Response
          </label>
          <pre className="w-full p-4 bg-gray-50 border border-gray-200 rounded-md overflow-auto max-h-64 text-sm font-mono">
            {result}
          </pre>
        </div>
      )}

      {/* Info Box */}
      <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-md">
        <p className="text-sm text-yellow-800">
          <strong>Note:</strong> Messages are processed asynchronously.
          Subscribe to the topic or listen to the response queue to receive results.
        </p>
      </div>

      {/* Message Format Reference */}
      <details className="mt-4">
        <summary className="cursor-pointer text-sm font-medium text-gray-700 hover:text-gray-900">
          Message Format Reference
        </summary>
        <div className="mt-2 p-4 bg-gray-50 rounded border border-gray-200">
          <p className="text-sm text-gray-600 mb-2">Complete message structure:</p>
          <pre className="text-xs font-mono overflow-auto">
{`{
  "service": "${serviceName}",
  "operation": "${operation}",
  "data": ${data},
  "timestamp": 1700000000000,
  "messageId": "msg-123-456"
}`}
          </pre>
        </div>
      </details>
    </div>
  );
}
