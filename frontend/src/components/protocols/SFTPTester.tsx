import React, { useState, useEffect } from 'react';
import { Upload, Download, FolderOpen, RefreshCw, FileText, AlertCircle, CheckCircle } from 'lucide-react';

interface SFTPTesterProps {
  serviceName: string;
}

interface SFTPStats {
  requestsPending: number;
  responsesGenerated: number;
  errors: number;
  archived: number;
}

export function SFTPTester({ serviceName }: SFTPTesterProps) {
  const [operation, setOperation] = useState<'CREATE' | 'GET' | 'UPDATE' | 'DELETE' | 'LIST'>('CREATE');
  const [data, setData] = useState('{\n  "name": "Sample Item",\n  "field1": "value1"\n}');
  const [requestId, setRequestId] = useState('');
  const [stats, setStats] = useState<SFTPStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [responses, setResponses] = useState<any[]>([]);

  const sftpRoot = `/sftp/${serviceName}`;

  useEffect(() => {
    loadStats();
  }, [serviceName]);

  const loadStats = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/sftp/${serviceName}/stats`);
      if (response.ok) {
        const data = await response.json();
        setStats(data);
      }
    } catch (err) {
      console.error('Failed to load SFTP stats', err);
    }
  };

  const createRequestFile = async () => {
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

      const newRequestId = requestId || `req-${Date.now()}`;
      const requestFile = {
        service: serviceName,
        operation,
        data: parsedData,
        timestamp: Date.now(),
        requestId: newRequestId
      };

      // Send to backend endpoint that creates SFTP request file
      const response = await fetch(`http://localhost:8080/api/sftp/${serviceName}/request`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestFile),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();
      setSuccess(true);
      setRequestId(newRequestId);
      await loadStats();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create request file');
    } finally {
      setLoading(false);
    }
  };

  const processRequests = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`http://localhost:8080/api/sftp/${serviceName}/process`, {
        method: 'POST',
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      await loadStats();
      await loadResponses();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to process requests');
    } finally {
      setLoading(false);
    }
  };

  const loadResponses = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/sftp/${serviceName}/responses`);
      if (response.ok) {
        const data = await response.json();
        setResponses(data);
      }
    } catch (err) {
      console.error('Failed to load responses', err);
    }
  };

  const loadSampleData = (op: typeof operation) => {
    setOperation(op);
    switch (op) {
      case 'CREATE':
        setData('{\n  "name": "New Item",\n  "description": "Created via SFTP"\n}');
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

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-gray-900 mb-2">SFTP File Tester</h3>
        <p className="text-sm text-gray-600">
          Create request files and monitor responses for {serviceName} service
        </p>
        <p className="text-xs text-gray-500 mt-1 font-mono">
          Directory: {sftpRoot}
        </p>
      </div>

      {/* Statistics */}
      {stats && (
        <div className="mb-4 grid grid-cols-4 gap-4">
          <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
            <div className="text-xs text-yellow-700 mb-1">Pending</div>
            <div className="text-2xl font-bold text-yellow-900">{stats.requestsPending}</div>
          </div>
          <div className="bg-green-50 border border-green-200 rounded p-3">
            <div className="text-xs text-green-700 mb-1">Responses</div>
            <div className="text-2xl font-bold text-green-900">{stats.responsesGenerated}</div>
          </div>
          <div className="bg-red-50 border border-red-200 rounded p-3">
            <div className="text-xs text-red-700 mb-1">Errors</div>
            <div className="text-2xl font-bold text-red-900">{stats.errors}</div>
          </div>
          <div className="bg-blue-50 border border-blue-200 rounded p-3">
            <div className="text-xs text-blue-700 mb-1">Archived</div>
            <div className="text-2xl font-bold text-blue-900">{stats.archived}</div>
          </div>
        </div>
      )}

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

      {/* Request ID */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Request ID (optional - auto-generated if empty)
        </label>
        <input
          type="text"
          value={requestId}
          onChange={(e) => setRequestId(e.target.value)}
          placeholder={`req-${Date.now()}`}
          className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-blue-500 focus:border-blue-500"
        />
      </div>

      {/* Data Editor */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Request Data (JSON)
        </label>
        <textarea
          value={data}
          onChange={(e) => setData(e.target.value)}
          className="w-full h-48 px-3 py-2 border border-gray-300 rounded-md font-mono text-sm focus:ring-blue-500 focus:border-blue-500"
          placeholder='{"field": "value"}'
        />
      </div>

      {/* Action Buttons */}
      <div className="grid grid-cols-2 gap-4 mb-4">
        <button
          onClick={createRequestFile}
          disabled={loading}
          className="flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400"
        >
          <Upload className="w-4 h-4" />
          Create Request File
        </button>
        <button
          onClick={processRequests}
          disabled={loading}
          className="flex items-center justify-center gap-2 px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:bg-gray-400"
        >
          <RefreshCw className="w-4 h-4" />
          Process Requests
        </button>
      </div>

      {/* Success Display */}
      {success && !error && (
        <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-md flex items-start gap-2">
          <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-green-800">Request File Created</p>
            <p className="text-sm text-green-700">
              Request ID: <code className="font-mono">{requestId}</code>
            </p>
            <p className="text-xs text-green-600 mt-1">
              File created in: <code className="font-mono">{sftpRoot}/requests/</code>
            </p>
          </div>
        </div>
      )}

      {/* Error Display */}
      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-md flex items-start gap-2">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-red-800">Error</p>
            <p className="text-sm text-red-700">{error}</p>
          </div>
        </div>
      )}

      {/* Responses */}
      {responses.length > 0 && (
        <div className="mt-4">
          <div className="flex items-center justify-between mb-2">
            <label className="block text-sm font-medium text-gray-700">
              Recent Responses ({responses.length})
            </label>
            <button
              onClick={loadResponses}
              className="text-sm text-blue-600 hover:text-blue-800"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {responses.map((resp, idx) => (
              <div key={idx} className="p-3 bg-gray-50 border border-gray-200 rounded">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-mono text-gray-600">{resp.requestId}</span>
                  <span className={`text-xs px-2 py-0.5 rounded ${
                    resp.success ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  }`}>
                    {resp.success ? 'Success' : 'Error'}
                  </span>
                </div>
                <pre className="text-xs font-mono overflow-auto">
                  {JSON.stringify(resp.data, null, 2)}
                </pre>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Directory Structure Info */}
      <details className="mt-4">
        <summary className="cursor-pointer text-sm font-medium text-gray-700 hover:text-gray-900">
          SFTP Directory Structure
        </summary>
        <div className="mt-2 p-4 bg-gray-50 rounded border border-gray-200">
          <div className="text-sm font-mono space-y-1">
            <div className="flex items-center gap-2">
              <FolderOpen className="w-4 h-4 text-gray-500" />
              <span>{sftpRoot}/</span>
            </div>
            <div className="ml-6 space-y-1">
              <div className="flex items-center gap-2">
                <FolderOpen className="w-4 h-4 text-yellow-500" />
                <span>requests/ - Incoming request files</span>
              </div>
              <div className="flex items-center gap-2">
                <FolderOpen className="w-4 h-4 text-green-500" />
                <span>responses/ - Generated response files</span>
              </div>
              <div className="flex items-center gap-2">
                <FolderOpen className="w-4 h-4 text-red-500" />
                <span>errors/ - Error files</span>
              </div>
              <div className="flex items-center gap-2">
                <FolderOpen className="w-4 h-4 text-blue-500" />
                <span>archive/ - Processed requests</span>
              </div>
            </div>
          </div>
        </div>
      </details>

      {/* File Format Reference */}
      <details className="mt-4">
        <summary className="cursor-pointer text-sm font-medium text-gray-700 hover:text-gray-900">
          Request File Format
        </summary>
        <div className="mt-2 p-4 bg-gray-50 rounded border border-gray-200">
          <p className="text-sm text-gray-600 mb-2">Example request file:</p>
          <pre className="text-xs font-mono overflow-auto">
{`{
  "service": "${serviceName}",
  "operation": "${operation}",
  "data": ${data},
  "timestamp": ${Date.now()},
  "requestId": "${requestId || `req-${Date.now()}`}"
}`}
          </pre>
        </div>
      </details>
    </div>
  );
}
