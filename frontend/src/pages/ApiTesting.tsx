/**
 * API Testing Page
 * Comprehensive testing interface for all 7 protocols
 * REST, SOAP, WebSocket, gRPC, GraphQL, ActiveMQ, SFTP
 */

import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import toast from 'react-hot-toast';
import { GraphQLTester } from '../components/protocols/GraphQLTester';
import { ActiveMQTester } from '../components/protocols/ActiveMQTester';
import { SFTPTester } from '../components/protocols/SFTPTester';

const API_BASE_URL = 'http://localhost:8080/api/v1';

interface Endpoint {
  id: string;
  type: string;
  path: string;
  httpMethod: string;
  operationName: string;
  description: string;
  serviceName: string;
  serviceId: string;
}

type ProtocolTab = 'REST/SOAP' | 'WebSocket' | 'gRPC' | 'GraphQL' | 'ActiveMQ' | 'SFTP';

const ApiTesting: React.FC = () => {
  const [activeTab, setActiveTab] = useState<ProtocolTab>('REST/SOAP');
  const [selectedService, setSelectedService] = useState<string>('');
  const [selectedEndpoint, setSelectedEndpoint] = useState<Endpoint | null>(null);
  const [requestBody, setRequestBody] = useState('{\n  \n}');
  const [response, setResponse] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  // Fetch all available endpoints
  const { data: endpointsData, isLoading } = useQuery({
    queryKey: ['endpoints'],
    queryFn: async () => {
      const res = await axios.get(`${API_BASE_URL}/catalog/endpoints`);
      return res.data.data;
    },
  });

  const endpoints: Endpoint[] = endpointsData || [];

  const handleTestEndpoint = async () => {
    if (!selectedEndpoint) {
      toast.error('Please select an endpoint to test');
      return;
    }

    setLoading(true);
    setResponse(null);

    try {
      const method = selectedEndpoint.httpMethod.toLowerCase();
      const url = `http://localhost:8080${selectedEndpoint.path}`;

      let result;
      const config = {
        headers: {
          'Content-Type': selectedEndpoint.type === 'SOAP' ? 'text/xml' : 'application/json',
        },
      };

      if (method === 'get') {
        result = await axios.get(url, config);
      } else if (method === 'delete') {
        result = await axios.delete(url, config);
      } else if (method === 'post') {
        const body = selectedEndpoint.type === 'SOAP' ? requestBody : JSON.parse(requestBody);
        result = await axios.post(url, body, config);
      } else if (method === 'put') {
        const body = selectedEndpoint.type === 'SOAP' ? requestBody : JSON.parse(requestBody);
        result = await axios.put(url, body, config);
      } else {
        throw new Error(`Unsupported HTTP method: ${method}`);
      }

      setResponse({
        status: result.status,
        statusText: result.statusText,
        headers: result.headers,
        data: result.data,
      });
      toast.success('Request successful!');
    } catch (error: any) {
      setResponse({
        status: error.response?.status || 'Error',
        statusText: error.response?.statusText || error.message,
        headers: error.response?.headers || {},
        data: error.response?.data || { error: error.message },
      });
      toast.error('Request failed');
    } finally {
      setLoading(false);
    }
  };

  const formatJson = () => {
    try {
      const parsed = JSON.parse(requestBody);
      setRequestBody(JSON.stringify(parsed, null, 2));
      toast.success('JSON formatted');
    } catch (error) {
      toast.error('Invalid JSON');
    }
  };

  // Get unique service names from endpoints
  const serviceNames = React.useMemo(() => {
    const names = new Set(endpoints.map((e) => e.serviceName));
    return Array.from(names);
  }, [endpoints]);

  // Set first service as default
  React.useEffect(() => {
    if (serviceNames.length > 0 && !selectedService) {
      setSelectedService(serviceNames[0]);
    }
  }, [serviceNames, selectedService]);

  const tabs: { id: ProtocolTab; label: string; description: string }[] = [
    { id: 'REST/SOAP', label: 'REST & SOAP', description: 'HTTP/XML Web Services' },
    { id: 'WebSocket', label: 'WebSocket', description: 'Real-time bidirectional' },
    { id: 'gRPC', label: 'gRPC', description: 'High-performance RPC' },
    { id: 'GraphQL', label: 'GraphQL', description: 'Flexible query language' },
    { id: 'ActiveMQ', label: 'ActiveMQ', description: 'Message queue (JMS)' },
    { id: 'SFTP', label: 'SFTP', description: 'File-based integration' },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Multi-Protocol Testing</h1>
        <p className="mt-2 text-gray-600">Test services across all 7 supported protocols</p>
      </div>

      {/* Service Selection */}
      {serviceNames.length > 0 && (
        <div className="bg-white rounded-lg shadow p-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Select Service
          </label>
          <select
            value={selectedService}
            onChange={(e) => setSelectedService(e.target.value)}
            className="w-full max-w-md px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            {serviceNames.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Protocol Tabs */}
      <div className="bg-white rounded-lg shadow">
        <div className="border-b border-gray-200">
          <nav className="-mb-px flex overflow-x-auto" aria-label="Tabs">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`${
                  activeTab === tab.id
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                } whitespace-nowrap py-4 px-6 border-b-2 font-medium text-sm flex-shrink-0`}
              >
                <div className="flex flex-col items-start">
                  <span>{tab.label}</span>
                  <span className="text-xs text-gray-400 mt-0.5">{tab.description}</span>
                </div>
              </button>
            ))}
          </nav>
        </div>

        {/* Tab Content */}
        <div className="p-6">
          {activeTab === 'REST/SOAP' && (
            <RestSoapTester
              endpoints={endpoints}
              isLoading={isLoading}
              selectedEndpoint={selectedEndpoint}
              setSelectedEndpoint={setSelectedEndpoint}
              requestBody={requestBody}
              setRequestBody={setRequestBody}
              response={response}
              setResponse={setResponse}
              loading={loading}
              setLoading={setLoading}
              handleTestEndpoint={handleTestEndpoint}
              formatJson={formatJson}
            />
          )}

          {activeTab === 'WebSocket' && selectedService && (
            <WebSocketTester serviceName={selectedService} />
          )}

          {activeTab === 'gRPC' && selectedService && (
            <GrpcTester serviceName={selectedService} />
          )}

          {activeTab === 'GraphQL' && selectedService && (
            <GraphQLTester serviceName={selectedService} />
          )}

          {activeTab === 'ActiveMQ' && selectedService && (
            <ActiveMQTester serviceName={selectedService} />
          )}

          {activeTab === 'SFTP' && selectedService && (
            <SFTPTester serviceName={selectedService} />
          )}

          {!selectedService && activeTab !== 'REST/SOAP' && (
            <div className="text-center py-12 text-gray-500">
              Please deploy a service first to test {activeTab} protocol
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// REST/SOAP Tester Component
interface RestSoapTesterProps {
  endpoints: Endpoint[];
  isLoading: boolean;
  selectedEndpoint: Endpoint | null;
  setSelectedEndpoint: (endpoint: Endpoint | null) => void;
  requestBody: string;
  setRequestBody: (body: string) => void;
  response: any;
  setResponse: (response: any) => void;
  loading: boolean;
  setLoading: (loading: boolean) => void;
  handleTestEndpoint: () => void;
  formatJson: () => void;
}

const RestSoapTester: React.FC<RestSoapTesterProps> = ({
  endpoints,
  isLoading,
  selectedEndpoint,
  setSelectedEndpoint,
  requestBody,
  setRequestBody,
  response,
  setResponse,
  loading,
  setLoading,
  handleTestEndpoint,
  formatJson,
}) => {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      {/* Endpoints List */}
      <div className="lg:col-span-1">
        <div className="bg-gray-50 rounded-lg border border-gray-200">
          <div className="px-4 py-3 border-b border-gray-200 bg-white">
            <h2 className="text-lg font-semibold text-gray-900">Available Endpoints</h2>
            </div>
            <div className="p-4">
              {isLoading ? (
                <div className="text-center py-8 text-gray-500">Loading endpoints...</div>
              ) : endpoints.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  No endpoints available. Deploy a service first.
                </div>
              ) : (
                <div className="space-y-2">
                  {endpoints.map((endpoint) => (
                    <button
                      key={endpoint.id}
                      onClick={() => setSelectedEndpoint(endpoint)}
                      className={`w-full text-left p-3 rounded-lg border transition-colors ${
                        selectedEndpoint?.id === endpoint.id
                          ? 'bg-blue-50 border-blue-500'
                          : 'bg-white border-gray-200 hover:bg-gray-50'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span
                          className={`px-2 py-0.5 text-xs font-semibold rounded ${
                            endpoint.httpMethod === 'GET'
                              ? 'bg-green-100 text-green-800'
                              : endpoint.httpMethod === 'POST'
                              ? 'bg-blue-100 text-blue-800'
                              : endpoint.httpMethod === 'PUT'
                              ? 'bg-yellow-100 text-yellow-800'
                              : endpoint.httpMethod === 'DELETE'
                              ? 'bg-red-100 text-red-800'
                              : 'bg-gray-100 text-gray-800'
                          }`}
                        >
                          {endpoint.httpMethod}
                        </span>
                        <span className="text-xs text-gray-500">{endpoint.type}</span>
                      </div>
                      <div className="text-sm font-medium text-gray-900 truncate">
                        {endpoint.operationName}
                      </div>
                      <div className="text-xs text-gray-500 truncate">{endpoint.path}</div>
                      <div className="text-xs text-gray-400 mt-1">
                        Service: {endpoint.serviceName}
                      </div>
                    </button>
                  ))}
                </div>
              )}
          </div>
        </div>
      </div>

      {/* Request/Response Panel */}
      <div className="lg:col-span-2">
        <div className="bg-white rounded-lg border border-gray-200">
          <div className="px-4 py-3 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-900">
                {selectedEndpoint ? `Test: ${selectedEndpoint.operationName}` : 'Select an Endpoint'}
              </h2>
            </div>
            <div className="p-4 space-y-4">
              {selectedEndpoint ? (
                <>
                  {/* Endpoint Info */}
                  <div className="bg-gray-50 rounded-lg p-4 space-y-2">
                    <div className="flex items-center gap-2">
                      <span
                        className={`px-3 py-1 text-sm font-semibold rounded ${
                          selectedEndpoint.httpMethod === 'GET'
                            ? 'bg-green-100 text-green-800'
                            : selectedEndpoint.httpMethod === 'POST'
                            ? 'bg-blue-100 text-blue-800'
                            : selectedEndpoint.httpMethod === 'PUT'
                            ? 'bg-yellow-100 text-yellow-800'
                            : selectedEndpoint.httpMethod === 'DELETE'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        {selectedEndpoint.httpMethod}
                      </span>
                      <code className="text-sm font-mono text-gray-700">
                        {selectedEndpoint.path}
                      </code>
                    </div>
                    {selectedEndpoint.description && (
                      <p className="text-sm text-gray-600">{selectedEndpoint.description}</p>
                    )}
                  </div>

                  {/* Request Body (for POST/PUT) */}
                  {(selectedEndpoint.httpMethod === 'POST' ||
                    selectedEndpoint.httpMethod === 'PUT') && (
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <label className="text-sm font-medium text-gray-700">Request Body</label>
                        <button
                          onClick={formatJson}
                          className="text-sm text-blue-600 hover:text-blue-700"
                        >
                          Format JSON
                        </button>
                      </div>
                      <textarea
                        value={requestBody}
                        onChange={(e) => setRequestBody(e.target.value)}
                        rows={8}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg font-mono text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder={
                          selectedEndpoint.type === 'SOAP'
                            ? 'Enter SOAP XML envelope...'
                            : 'Enter JSON request body...'
                        }
                      />
                    </div>
                  )}

                  {/* Test Button */}
                  <button
                    onClick={handleTestEndpoint}
                    disabled={loading}
                    className="w-full bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
                  >
                    {loading ? 'Sending Request...' : 'Send Request'}
                  </button>

                  {/* Response */}
                  {response && (
                    <div>
                      <h3 className="text-sm font-medium text-gray-700 mb-2">Response</h3>
                      <div className="bg-gray-50 rounded-lg p-4 space-y-2">
                        <div className="flex items-center gap-2">
                          <span
                            className={`px-2 py-1 text-xs font-semibold rounded ${
                              response.status >= 200 && response.status < 300
                                ? 'bg-green-100 text-green-800'
                                : 'bg-red-100 text-red-800'
                            }`}
                          >
                            {response.status} {response.statusText}
                          </span>
                        </div>
                        <div className="mt-4">
                          <div className="text-xs font-medium text-gray-700 mb-1">Body:</div>
                          <pre className="bg-white border border-gray-200 rounded p-3 text-xs overflow-x-auto">
                            {typeof response.data === 'string'
                              ? response.data
                              : JSON.stringify(response.data, null, 2)}
                          </pre>
                        </div>
                      </div>
                    </div>
                  )}
                </>
              ) : (
                <div className="text-center py-12 text-gray-500">
                  Select an endpoint from the list to start testing
                </div>
              )}
          </div>
        </div>

        {/* Swagger Link */}
        <div className="mt-4 bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start gap-3">
              <svg
                className="w-5 h-5 text-blue-600 mt-0.5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <div className="flex-1">
                <h4 className="text-sm font-medium text-blue-900">Advanced API Testing</h4>
                <p className="text-sm text-blue-700 mt-1">
                  For advanced testing with auto-generated documentation, visit{' '}
                  <a
                    href="http://localhost:8080/swagger-ui/index.html"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="font-medium underline hover:text-blue-800"
                  >
                    Swagger UI
                  </a>
                </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

// WebSocket Tester Component (placeholder)
const WebSocketTester: React.FC<{ serviceName: string }> = ({ serviceName }) => {
  return (
    <div className="bg-gray-50 rounded-lg p-6 text-center">
      <h3 className="text-lg font-semibold text-gray-900 mb-2">WebSocket Testing</h3>
      <p className="text-gray-600 mb-4">
        WebSocket testing for service: <code className="font-mono font-semibold">{serviceName}</code>
      </p>
      <div className="text-sm text-gray-500 space-y-2">
        <p>WebSocket endpoint: <code className="font-mono">ws://localhost:8080/ws</code></p>
        <p>Topic: <code className="font-mono">/topic/service.{serviceName}</code></p>
        <p>Subscribe: <code className="font-mono">/app/service/{serviceName}</code></p>
      </div>
      <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded">
        <p className="text-sm text-blue-800">
          Use a WebSocket client like Postman or your browser console to test real-time updates
        </p>
      </div>
    </div>
  );
};

// gRPC Tester Component (placeholder)
const GrpcTester: React.FC<{ serviceName: string }> = ({ serviceName }) => {
  return (
    <div className="bg-gray-50 rounded-lg p-6 text-center">
      <h3 className="text-lg font-semibold text-gray-900 mb-2">gRPC Testing</h3>
      <p className="text-gray-600 mb-4">
        gRPC testing for service: <code className="font-mono font-semibold">{serviceName}</code>
      </p>
      <div className="text-sm text-gray-500 space-y-2">
        <p>gRPC server: <code className="font-mono">localhost:9090</code></p>
        <p>Proto file: <code className="font-mono">/api/v1/proto/{serviceName}</code></p>
      </div>
      <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded">
        <p className="text-sm text-blue-800 mb-2">
          Download the proto file and use tools like:
        </p>
        <ul className="text-sm text-blue-700 space-y-1">
          <li>• grpcurl (CLI tool)</li>
          <li>• BloomRPC (GUI client)</li>
          <li>• Postman (gRPC support)</li>
        </ul>
      </div>
    </div>
  );
};

export default ApiTesting;
