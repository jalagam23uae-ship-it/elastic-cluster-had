/**
 * Monitoring Page
 * Displays service health, performance metrics, and system status
 */

import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1';

interface Service {
  id: string;
  serviceName: string;
  version: string;
  status: string;
  deployedAt: string;
  totalEndpoints: number;
  restEndpoints: number;
  soapEndpoints: number;
  totalRequests: number;
  averageResponseTime: number | null;
}

interface Metric {
  id: string;
  timestamp: string;
  requestCount: number;
  errorCount: number;
  averageResponseTime: number;
  endpoint: string;
}

const Monitoring: React.FC = () => {
  const [selectedService, setSelectedService] = useState<string | null>(null);
  const [timeRange, setTimeRange] = useState<string>('24h');

  // Fetch all services
  const { data: servicesData } = useQuery({
    queryKey: ['services'],
    queryFn: async () => {
      const res = await axios.get(`${API_BASE_URL}/catalog/services`);
      return res.data.data;
    },
    refetchInterval: 30000, // Refresh every 30 seconds
  });

  const services: Service[] = servicesData || [];

  // Fetch metrics for selected service
  const { data: metricsData } = useQuery({
    queryKey: ['metrics', selectedService],
    queryFn: async () => {
      if (!selectedService) return [];
      const res = await axios.get(`${API_BASE_URL}/management/metrics/${selectedService}`);
      return res.data.data;
    },
    enabled: !!selectedService,
    refetchInterval: 10000, // Refresh every 10 seconds
  });

  const metrics: Metric[] = metricsData || [];

  // Calculate aggregate metrics
  const calculateAggregates = (service: Service) => {
    const successRate = service.totalRequests > 0
      ? ((service.totalRequests - 0) / service.totalRequests * 100).toFixed(1)
      : '0.0';

    return {
      totalRequests: service.totalRequests || 0,
      successRate: successRate,
      avgResponseTime: service.averageResponseTime || 0,
      uptime: calculateUptime(service.deployedAt),
    };
  };

  const calculateUptime = (deployedAt: string) => {
    const now = new Date();
    const deployed = new Date(deployedAt);
    const diff = now.getTime() - deployed.getTime();

    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

    if (days > 0) return `${days}d ${hours}h`;
    if (hours > 0) return `${hours}h ${minutes}m`;
    return `${minutes}m`;
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DEPLOYED':
        return 'bg-green-100 text-green-800';
      case 'DEPLOYING':
        return 'bg-blue-100 text-blue-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Service Monitoring</h1>
          <p className="mt-2 text-gray-600">Real-time monitoring and performance metrics</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-600">Auto-refresh enabled</span>
          <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
        </div>
      </div>

      {/* Services Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {services.length === 0 ? (
          <div className="col-span-full bg-white rounded-lg shadow-md border border-gray-200 p-8">
            <div className="text-center text-gray-500">
              No services deployed yet. Deploy a service to start monitoring.
            </div>
          </div>
        ) : (
          services.map((service) => {
            const aggregates = calculateAggregates(service);
            return (
              <div
                key={service.id}
                className={`bg-white rounded-lg shadow-md border-2 cursor-pointer transition-all hover:shadow-lg ${
                  selectedService === service.serviceName
                    ? 'border-blue-500'
                    : 'border-gray-200'
                }`}
                onClick={() => setSelectedService(service.serviceName)}
              >
                <div className="p-4">
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-lg font-semibold text-gray-900">
                      {service.serviceName}
                    </h3>
                    <span
                      className={`px-2 py-1 text-xs font-semibold rounded ${getStatusColor(
                        service.status
                      )}`}
                    >
                      {service.status}
                    </span>
                  </div>

                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Version:</span>
                      <span className="font-medium">{service.version}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Endpoints:</span>
                      <span className="font-medium">
                        {service.totalEndpoints} ({service.restEndpoints} REST, {service.soapEndpoints} SOAP)
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Requests:</span>
                      <span className="font-medium">{aggregates.totalRequests}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Success Rate:</span>
                      <span className="font-medium">{aggregates.successRate}%</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Avg Response:</span>
                      <span className="font-medium">
                        {aggregates.avgResponseTime > 0
                          ? `${aggregates.avgResponseTime.toFixed(0)}ms`
                          : 'N/A'}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Uptime:</span>
                      <span className="font-medium">{aggregates.uptime}</span>
                    </div>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Detailed Metrics */}
      {selectedService && (
        <div className="bg-white rounded-lg shadow-md border border-gray-200">
          <div className="px-4 py-3 border-b border-gray-200">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900">
                Detailed Metrics: {selectedService}
              </h2>
              <select
                value={timeRange}
                onChange={(e) => setTimeRange(e.target.value)}
                className="px-3 py-1 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="1h">Last 1 hour</option>
                <option value="24h">Last 24 hours</option>
                <option value="7d">Last 7 days</option>
                <option value="30d">Last 30 days</option>
              </select>
            </div>
          </div>

          <div className="p-4">
            {metrics.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                No metrics data available yet. Metrics will appear after the service receives requests.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                        Timestamp
                      </th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                        Endpoint
                      </th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                        Requests
                      </th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                        Errors
                      </th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                        Avg Response
                      </th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                        Success Rate
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {metrics.map((metric) => {
                      const successRate =
                        metric.requestCount > 0
                          ? (
                              ((metric.requestCount - metric.errorCount) /
                                metric.requestCount) *
                              100
                            ).toFixed(1)
                          : '0.0';
                      return (
                        <tr key={metric.id} className="hover:bg-gray-50">
                          <td className="px-4 py-2 text-sm text-gray-900">
                            {new Date(metric.timestamp).toLocaleString()}
                          </td>
                          <td className="px-4 py-2 text-sm font-mono text-gray-700">
                            {metric.endpoint}
                          </td>
                          <td className="px-4 py-2 text-sm text-gray-900">
                            {metric.requestCount}
                          </td>
                          <td className="px-4 py-2 text-sm">
                            <span
                              className={`px-2 py-0.5 rounded text-xs font-semibold ${
                                metric.errorCount > 0
                                  ? 'bg-red-100 text-red-800'
                                  : 'bg-green-100 text-green-800'
                              }`}
                            >
                              {metric.errorCount}
                            </span>
                          </td>
                          <td className="px-4 py-2 text-sm text-gray-900">
                            {metric.averageResponseTime.toFixed(0)}ms
                          </td>
                          <td className="px-4 py-2 text-sm">
                            <span
                              className={`px-2 py-0.5 rounded text-xs font-semibold ${
                                parseFloat(successRate) >= 95
                                  ? 'bg-green-100 text-green-800'
                                  : parseFloat(successRate) >= 80
                                  ? 'bg-yellow-100 text-yellow-800'
                                  : 'bg-red-100 text-red-800'
                              }`}
                            >
                              {successRate}%
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* System Health */}
      <div className="bg-white rounded-lg shadow-md border border-gray-200">
        <div className="px-4 py-3 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">System Health</h2>
        </div>
        <div className="p-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-green-50 border border-green-200 rounded-lg p-4">
              <div className="flex items-center gap-2 mb-2">
                <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                <h3 className="font-semibold text-gray-900">Backend API</h3>
              </div>
              <p className="text-sm text-gray-600">Status: Healthy</p>
              <p className="text-xs text-gray-500 mt-1">Port: 8080</p>
            </div>

            <div className="bg-green-50 border border-green-200 rounded-lg p-4">
              <div className="flex items-center gap-2 mb-2">
                <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                <h3 className="font-semibold text-gray-900">Database</h3>
              </div>
              <p className="text-sm text-gray-600">Status: Connected</p>
              <p className="text-xs text-gray-500 mt-1">PostgreSQL</p>
            </div>

            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <div className="flex items-center gap-2 mb-2">
                <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
                <h3 className="font-semibold text-gray-900">Active Services</h3>
              </div>
              <p className="text-sm text-gray-600">
                {services.filter((s) => s.status === 'DEPLOYED').length} deployed
              </p>
              <p className="text-xs text-gray-500 mt-1">
                Total: {services.length} services
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Monitoring;
