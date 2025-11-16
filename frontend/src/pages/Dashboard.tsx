/**
 * Dashboard Page
 */

import React from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Activity,
  Server,
  FileCode,
  TrendingUp,
  Clock,
  AlertCircle,
  CheckCircle,
  Zap,
} from 'lucide-react';
import Card from '../components/common/Card';
import StatusBadge from '../components/common/StatusBadge';
import catalogService from '../api/catalogService';
import { formatDistanceToNow } from 'date-fns';

const Dashboard: React.FC = () => {
  // Fetch services
  const { data: services = [], isLoading: servicesLoading } = useQuery({
    queryKey: ['catalog-services'],
    queryFn: catalogService.listServices,
  });

  // Fetch schemas
  const { data: schemas = [], isLoading: schemasLoading } = useQuery({
    queryKey: ['catalog-schemas'],
    queryFn: () => catalogService.listSchemas(),
  });

  // Fetch endpoints
  const { data: endpoints = [], isLoading: endpointsLoading } = useQuery({
    queryKey: ['catalog-endpoints'],
    queryFn: catalogService.listEndpoints,
  });

  // Calculate statistics
  const stats = React.useMemo(() => {
    const totalServices = services.length;
    const totalSchemas = schemas.length;
    const totalEndpoints = endpoints.length;
    const activeSchemas = schemas.filter((s) => s.deployed).length;

    const totalRequests = services.reduce((sum, s) => sum + (s.totalRequests || 0), 0);
    const avgResponseTime =
      services.length > 0
        ? services.reduce((sum, s) => sum + (s.averageResponseTime || 0), 0) / services.length
        : 0;

    const restEndpoints = endpoints.filter((e) => e.endpointType === 'REST').length;
    const soapEndpoints = endpoints.filter((e) => e.endpointType === 'SOAP').length;
    const webSocketEndpoints = endpoints.filter((e) => e.endpointType === 'WebSocket').length;
    const grpcEndpoints = endpoints.filter((e) => e.endpointType === 'gRPC').length;
    const graphQLEndpoints = endpoints.filter((e) => e.endpointType === 'GraphQL').length;
    const activeMQEndpoints = endpoints.filter((e) => e.endpointType === 'ActiveMQ').length;
    const sftpEndpoints = endpoints.filter((e) => e.endpointType === 'SFTP').length;

    return {
      totalServices,
      totalSchemas,
      totalEndpoints,
      activeSchemas,
      totalRequests,
      avgResponseTime,
      restEndpoints,
      soapEndpoints,
      webSocketEndpoints,
      grpcEndpoints,
      graphQLEndpoints,
      activeMQEndpoints,
      sftpEndpoints,
    };
  }, [services, schemas, endpoints]);

  const isLoading = servicesLoading || schemasLoading || endpointsLoading;

  // Get recent services (top 5)
  const recentServices = React.useMemo(() => {
    return [...services]
      .sort((a, b) => new Date(b.deployedAt).getTime() - new Date(a.deployedAt).getTime())
      .slice(0, 5);
  }, [services]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-2 text-gray-600">
          Overview of your XSD service generation platform
        </p>
      </div>

      {/* Statistics Cards */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {[1, 2, 3, 4].map((i) => (
            <Card key={i}>
              <div className="animate-pulse">
                <div className="h-4 bg-gray-200 rounded w-1/2 mb-2"></div>
                <div className="h-8 bg-gray-200 rounded w-3/4"></div>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {/* Services */}
          <Card className="bg-gradient-to-br from-blue-50 to-blue-100 border-blue-200">
            <div className="flex items-center justify-between">
              <div className="flex-1">
                <p className="text-sm font-medium text-blue-600">Services</p>
                <p className="text-3xl font-bold text-blue-900 mt-1">{stats.totalServices}</p>
                <div className="mt-2 flex flex-wrap gap-1 text-[10px]">
                  {stats.restEndpoints > 0 && (
                    <span className="px-1.5 py-0.5 bg-blue-200 text-blue-800 rounded">
                      {stats.restEndpoints} REST
                    </span>
                  )}
                  {stats.soapEndpoints > 0 && (
                    <span className="px-1.5 py-0.5 bg-purple-200 text-purple-800 rounded">
                      {stats.soapEndpoints} SOAP
                    </span>
                  )}
                  {stats.webSocketEndpoints > 0 && (
                    <span className="px-1.5 py-0.5 bg-green-200 text-green-800 rounded">
                      {stats.webSocketEndpoints} WS
                    </span>
                  )}
                  {stats.grpcEndpoints > 0 && (
                    <span className="px-1.5 py-0.5 bg-indigo-200 text-indigo-800 rounded">
                      {stats.grpcEndpoints} gRPC
                    </span>
                  )}
                  {stats.graphQLEndpoints > 0 && (
                    <span className="px-1.5 py-0.5 bg-pink-200 text-pink-800 rounded">
                      {stats.graphQLEndpoints} GraphQL
                    </span>
                  )}
                  {stats.activeMQEndpoints > 0 && (
                    <span className="px-1.5 py-0.5 bg-orange-200 text-orange-800 rounded">
                      {stats.activeMQEndpoints} MQ
                    </span>
                  )}
                  {stats.sftpEndpoints > 0 && (
                    <span className="px-1.5 py-0.5 bg-teal-200 text-teal-800 rounded">
                      {stats.sftpEndpoints} SFTP
                    </span>
                  )}
                </div>
              </div>
              <Server className="text-blue-600 flex-shrink-0" size={40} />
            </div>
          </Card>

          {/* Schemas */}
          <Card className="bg-gradient-to-br from-green-50 to-green-100 border-green-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-green-600">Schemas</p>
                <p className="text-3xl font-bold text-green-900 mt-1">{stats.totalSchemas}</p>
                <p className="text-xs text-green-600 mt-1">
                  {stats.activeSchemas} deployed
                </p>
              </div>
              <FileCode className="text-green-600" size={40} />
            </div>
          </Card>

          {/* Endpoints */}
          <Card className="bg-gradient-to-br from-purple-50 to-purple-100 border-purple-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-purple-600">Endpoints</p>
                <p className="text-3xl font-bold text-purple-900 mt-1">{stats.totalEndpoints}</p>
                <p className="text-xs text-purple-600 mt-1">Active endpoints</p>
              </div>
              <Zap className="text-purple-600" size={40} />
            </div>
          </Card>

          {/* Total Requests */}
          <Card className="bg-gradient-to-br from-orange-50 to-orange-100 border-orange-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-orange-600">Total Requests</p>
                <p className="text-3xl font-bold text-orange-900 mt-1">
                  {stats.totalRequests.toLocaleString()}
                </p>
                <p className="text-xs text-orange-600 mt-1">
                  Avg {stats.avgResponseTime.toFixed(0)}ms
                </p>
              </div>
              <Activity className="text-orange-600" size={40} />
            </div>
          </Card>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Services */}
        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Recent Services</h2>
            <TrendingUp className="text-gray-400" size={20} />
          </div>

          {isLoading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="animate-pulse">
                  <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                  <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                </div>
              ))}
            </div>
          ) : recentServices.length > 0 ? (
            <div className="space-y-3">
              {recentServices.map((service) => (
                <div
                  key={service.id}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                >
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <p className="font-medium text-gray-900">{service.serviceName}</p>
                      <StatusBadge status={service.status} />
                    </div>
                    <p className="text-xs text-gray-500 mt-1">
                      v{service.version} • {service.totalEndpoints} endpoints •{' '}
                      {formatDistanceToNow(new Date(service.deployedAt), { addSuffix: true })}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-medium text-gray-900">
                      {(service.totalRequests || 0).toLocaleString()}
                    </p>
                    <p className="text-xs text-gray-500">requests</p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <Server className="mx-auto text-gray-400 mb-2" size={40} />
              <p className="text-sm text-gray-600">No services deployed yet</p>
            </div>
          )}
        </Card>

        {/* System Status */}
        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">System Status</h2>
            <Activity className="text-gray-400" size={20} />
          </div>

          <div className="space-y-3">
            <div className="flex items-center justify-between p-3 bg-green-50 rounded-lg">
              <div className="flex items-center gap-2">
                <CheckCircle className="text-green-600" size={20} />
                <span className="font-medium text-gray-900">API Server</span>
              </div>
              <span className="text-sm font-medium text-green-600">Operational</span>
            </div>

            <div className="flex items-center justify-between p-3 bg-green-50 rounded-lg">
              <div className="flex items-center gap-2">
                <CheckCircle className="text-green-600" size={20} />
                <span className="font-medium text-gray-900">Database</span>
              </div>
              <span className="text-sm font-medium text-green-600">Connected</span>
            </div>

            <div className="flex items-center justify-between p-3 bg-green-50 rounded-lg">
              <div className="flex items-center gap-2">
                <CheckCircle className="text-green-600" size={20} />
                <span className="font-medium text-gray-900">Code Compiler</span>
              </div>
              <span className="text-sm font-medium text-green-600">Ready</span>
            </div>

            <div className="flex items-center justify-between p-3 bg-blue-50 rounded-lg">
              <div className="flex items-center gap-2">
                <Clock className="text-blue-600" size={20} />
                <span className="font-medium text-gray-900">Uptime</span>
              </div>
              <span className="text-sm font-medium text-blue-600">99.9%</span>
            </div>
          </div>
        </Card>
      </div>

      {/* Protocol Breakdown */}
      <Card>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Protocol Distribution</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4">
          {/* REST */}
          <div className="text-center p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <div className="text-3xl font-bold text-blue-700">{stats.restEndpoints}</div>
            <div className="text-xs text-blue-600 mt-1 font-medium">REST</div>
            <div className="text-[10px] text-blue-500 mt-0.5">HTTP/JSON</div>
          </div>

          {/* SOAP */}
          <div className="text-center p-4 bg-purple-50 border border-purple-200 rounded-lg">
            <div className="text-3xl font-bold text-purple-700">{stats.soapEndpoints}</div>
            <div className="text-xs text-purple-600 mt-1 font-medium">SOAP</div>
            <div className="text-[10px] text-purple-500 mt-0.5">XML Service</div>
          </div>

          {/* WebSocket */}
          <div className="text-center p-4 bg-green-50 border border-green-200 rounded-lg">
            <div className="text-3xl font-bold text-green-700">{stats.webSocketEndpoints}</div>
            <div className="text-xs text-green-600 mt-1 font-medium">WebSocket</div>
            <div className="text-[10px] text-green-500 mt-0.5">Real-time</div>
          </div>

          {/* gRPC */}
          <div className="text-center p-4 bg-indigo-50 border border-indigo-200 rounded-lg">
            <div className="text-3xl font-bold text-indigo-700">{stats.grpcEndpoints}</div>
            <div className="text-xs text-indigo-600 mt-1 font-medium">gRPC</div>
            <div className="text-[10px] text-indigo-500 mt-0.5">High-perf</div>
          </div>

          {/* GraphQL */}
          <div className="text-center p-4 bg-pink-50 border border-pink-200 rounded-lg">
            <div className="text-3xl font-bold text-pink-700">{stats.graphQLEndpoints}</div>
            <div className="text-xs text-pink-600 mt-1 font-medium">GraphQL</div>
            <div className="text-[10px] text-pink-500 mt-0.5">Flexible</div>
          </div>

          {/* ActiveMQ */}
          <div className="text-center p-4 bg-orange-50 border border-orange-200 rounded-lg">
            <div className="text-3xl font-bold text-orange-700">{stats.activeMQEndpoints}</div>
            <div className="text-xs text-orange-600 mt-1 font-medium">ActiveMQ</div>
            <div className="text-[10px] text-orange-500 mt-0.5">Message Queue</div>
          </div>

          {/* SFTP */}
          <div className="text-center p-4 bg-teal-50 border border-teal-200 rounded-lg">
            <div className="text-3xl font-bold text-teal-700">{stats.sftpEndpoints}</div>
            <div className="text-xs text-teal-600 mt-1 font-medium">SFTP</div>
            <div className="text-[10px] text-teal-500 mt-0.5">File Transfer</div>
          </div>
        </div>
      </Card>

      {/* Quick Stats */}
      <Card>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Quick Statistics</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="text-center p-4 bg-gray-50 rounded-lg">
            <p className="text-sm text-gray-600">Average Response Time</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">
              {stats.avgResponseTime.toFixed(0)}ms
            </p>
          </div>
          <div className="text-center p-4 bg-gray-50 rounded-lg">
            <p className="text-sm text-gray-600">Success Rate</p>
            <p className="text-2xl font-bold text-green-600 mt-1">99.8%</p>
          </div>
          <div className="text-center p-4 bg-gray-50 rounded-lg">
            <p className="text-sm text-gray-600">Active Endpoints</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">{stats.totalEndpoints}</p>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default Dashboard;
