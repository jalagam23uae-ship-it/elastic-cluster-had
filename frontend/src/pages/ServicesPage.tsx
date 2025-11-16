/**
 * Services Management Page
 */

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Server,
  Search,
  RefreshCw,
  Eye,
  Power,
  PowerOff,
  ExternalLink,
  FileText,
  Zap,
  Activity,
  List,
} from 'lucide-react';
import toast from 'react-hot-toast';
import axios from 'axios';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Modal from '../components/common/Modal';
import StatusBadge from '../components/common/StatusBadge';
import FieldsViewModal from '../components/schema/FieldsViewModal';
import catalogService, { type SchemaCatalogEntry } from '../api/catalogService';
import serviceService from '../api/serviceService';
import schemaService from '../api/schemaService';
import { formatDistanceToNow } from 'date-fns';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const ServicesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [deployModalOpen, setDeployModalOpen] = useState(false);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [fieldsModalOpen, setFieldsModalOpen] = useState(false);
  const [selectedService, setSelectedService] = useState<any>(null);
  const [selectedSchema, setSelectedSchema] = useState<SchemaCatalogEntry | null>(null);
  const [fieldsAnalysis, setFieldsAnalysis] = useState<any>(null);
  const [loadingFields, setLoadingFields] = useState(false);

  // Deploy options - all protocols
  const [enableRest, setEnableRest] = useState(true);
  const [enableSoap, setEnableSoap] = useState(true);
  const [enableWebSocket, setEnableWebSocket] = useState(true);
  const [enableGrpc, setEnableGrpc] = useState(true);
  const [enableGraphQL, setEnableGraphQL] = useState(true);
  const [enableActiveMQ, setEnableActiveMQ] = useState(true);
  const [enableSftp, setEnableSftp] = useState(true);

  // Fetch services
  const { data: services = [], isLoading: servicesLoading, refetch } = useQuery({
    queryKey: ['catalog-services'],
    queryFn: catalogService.listServices,
  });

  // Fetch schemas for deployment
  const { data: schemas = [] } = useQuery({
    queryKey: ['catalog-schemas'],
    queryFn: () => catalogService.listSchemas('ACTIVE'),
  });

  // Deploy mutation
  const deployMutation = useMutation({
    mutationFn: ({ schemaId }: { schemaId: string }) =>
      serviceService.deploy(schemaId, {
        enableRest,
        enableSoap,
        enableWebSocket,
        enableGrpc,
        enableGraphQL,
        enableActiveMQ,
        enableSftp,
      }),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['catalog-services'] });
      queryClient.invalidateQueries({ queryKey: ['catalog-schemas'] });
      setDeployModalOpen(false);
      setSelectedSchema(null);
      toast.success(`Service "${data.data.serviceName}" deployed successfully!`);
    },
    onError: (error: any) => {
      const errorMessage = error.response?.data?.message || error.message || 'Deployment failed';
      toast.error(`Deployment failed: ${errorMessage}`);
    },
  });

  // Undeploy mutation
  const undeployMutation = useMutation({
    mutationFn: (serviceId: string) => serviceService.undeploy(serviceId),
    onSuccess: (_, serviceId) => {
      queryClient.invalidateQueries({ queryKey: ['catalog-services'] });
      queryClient.invalidateQueries({ queryKey: ['catalog-schemas'] });
      toast.success('Service undeployed successfully!');
    },
    onError: (error: any) => {
      const errorMessage = error.response?.data?.message || error.message || 'Undeploy failed';
      toast.error(`Undeploy failed: ${errorMessage}`);
    },
  });

  const handleDeploy = (schema: SchemaCatalogEntry) => {
    setSelectedSchema(schema);
    setDeployModalOpen(true);
  };

  const handleConfirmDeploy = () => {
    if (!selectedSchema) return;
    deployMutation.mutate({ schemaId: selectedSchema.id });
  };

  const handleUndeploy = (service: any) => {
    if (
      confirm(
        `Are you sure you want to undeploy "${service.serviceName}"? All endpoints will be removed.`
      )
    ) {
      undeployMutation.mutate(service.id);
    }
  };

  const handleViewDetails = async (service: any) => {
    try {
      const details = await catalogService.getServiceDetails(service.serviceName);
      setSelectedService(details);
      setDetailModalOpen(true);
    } catch (error: any) {
      toast.error('Failed to load service details');
    }
  };

  const handleViewFields = async (service: any) => {
    try {
      setLoadingFields(true);
      const response = await axios.get(
        `${API_BASE_URL}/api/v1/schemas/${service.serviceName}/fields`
      );
      setFieldsAnalysis(response.data);
      setFieldsModalOpen(true);
    } catch (error: any) {
      toast.error('Failed to load schema fields');
    } finally {
      setLoadingFields(false);
    }
  };

  // Filter services
  const filteredServices = React.useMemo(() => {
    if (!searchQuery) return services;
    return services.filter((s) =>
      s.serviceName.toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [services, searchQuery]);

  // Get available schemas for deployment (not already deployed)
  const availableSchemas = React.useMemo(() => {
    const deployedServiceNames = new Set(services.map((s) => s.serviceName));
    return schemas.filter((schema) => !deployedServiceNames.has(schema.serviceName));
  }, [schemas, services]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Service Management</h1>
          <p className="mt-2 text-gray-600">
            Deploy and manage your XSD-generated services
          </p>
        </div>
        <Button
          onClick={() => setDeployModalOpen(true)}
          leftIcon={<Server size={20} />}
          disabled={availableSchemas.length === 0}
        >
          Deploy Service
        </Button>
      </div>

      {/* Filters */}
      <Card>
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="Search services..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>
          <Button
            variant="outline"
            onClick={() => refetch()}
            leftIcon={<RefreshCw size={20} />}
          >
            Refresh
          </Button>
        </div>
      </Card>

      {/* Services List */}
      <Card>
        {servicesLoading ? (
          <div className="text-center py-12">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            <p className="mt-4 text-gray-600">Loading services...</p>
          </div>
        ) : filteredServices.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Service Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Version
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Endpoints
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Requests
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Deployed
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredServices.map((service) => (
                  <tr key={service.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <Server className="text-gray-400" size={18} />
                        <span className="text-sm font-medium text-gray-900">
                          {service.serviceName}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {service.version}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <StatusBadge status={service.status} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex flex-wrap items-center gap-1 text-xs">
                        {service.restEndpoints > 0 && (
                          <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded">
                            {service.restEndpoints} REST
                          </span>
                        )}
                        {service.soapEndpoints > 0 && (
                          <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded">
                            {service.soapEndpoints} SOAP
                          </span>
                        )}
                        {service.webSocketEndpoints && service.webSocketEndpoints > 0 && (
                          <span className="px-2 py-1 bg-green-100 text-green-700 rounded">
                            {service.webSocketEndpoints} WS
                          </span>
                        )}
                        {service.grpcEndpoints && service.grpcEndpoints > 0 && (
                          <span className="px-2 py-1 bg-indigo-100 text-indigo-700 rounded">
                            {service.grpcEndpoints} gRPC
                          </span>
                        )}
                        {service.graphQLEndpoints && service.graphQLEndpoints > 0 && (
                          <span className="px-2 py-1 bg-pink-100 text-pink-700 rounded">
                            {service.graphQLEndpoints} GraphQL
                          </span>
                        )}
                        {service.activeMQEndpoints && service.activeMQEndpoints > 0 && (
                          <span className="px-2 py-1 bg-orange-100 text-orange-700 rounded">
                            {service.activeMQEndpoints} MQ
                          </span>
                        )}
                        {service.sftpEndpoints && service.sftpEndpoints > 0 && (
                          <span className="px-2 py-1 bg-teal-100 text-teal-700 rounded">
                            {service.sftpEndpoints} SFTP
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm">
                        <div className="font-medium text-gray-900">
                          {(service.totalRequests || 0).toLocaleString()}
                        </div>
                        {service.averageResponseTime && (
                          <div className="text-xs text-gray-500">
                            {service.averageResponseTime.toFixed(0)}ms avg
                          </div>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDistanceToNow(new Date(service.deployedAt), { addSuffix: true })}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => handleViewDetails(service)}
                          className="text-primary-600 hover:text-primary-900"
                          title="View details"
                        >
                          <Eye size={18} />
                        </button>
                        <button
                          onClick={() => handleViewFields(service)}
                          className="text-blue-600 hover:text-blue-900"
                          title="View fields"
                          disabled={loadingFields}
                        >
                          <List size={18} />
                        </button>
                        <button
                          onClick={() => handleUndeploy(service)}
                          className="text-red-600 hover:text-red-900"
                          title="Undeploy service"
                          disabled={undeployMutation.isPending}
                        >
                          <PowerOff size={18} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-12">
            <Server size={48} className="mx-auto text-gray-400 mb-4" />
            <p className="text-gray-600 mb-2">No services deployed</p>
            <p className="text-sm text-gray-500 mb-4">
              Deploy a schema to create your first service
            </p>
            <Button
              onClick={() => setDeployModalOpen(true)}
              leftIcon={<Server size={20} />}
              disabled={availableSchemas.length === 0}
            >
              Deploy Service
            </Button>
          </div>
        )}
      </Card>

      {/* Deploy Modal */}
      <Modal
        isOpen={deployModalOpen}
        onClose={() => {
          setDeployModalOpen(false);
          setSelectedSchema(null);
        }}
        title="Deploy Service"
        footer={
          <>
            <Button
              variant="outline"
              onClick={() => {
                setDeployModalOpen(false);
                setSelectedSchema(null);
              }}
            >
              Cancel
            </Button>
            <Button
              onClick={handleConfirmDeploy}
              isLoading={deployMutation.isPending}
              leftIcon={<Server size={20} />}
              disabled={!selectedSchema}
            >
              Deploy
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          {/* Schema Selection */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Select Schema *
            </label>
            {availableSchemas.length > 0 ? (
              <select
                value={selectedSchema?.id || ''}
                onChange={(e) => {
                  const schema = availableSchemas.find((s) => s.id === e.target.value);
                  setSelectedSchema(schema || null);
                }}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                disabled={deployMutation.isPending}
              >
                <option value="">Select a schema...</option>
                {availableSchemas.map((schema) => (
                  <option key={schema.id} value={schema.id}>
                    {schema.serviceName} (v{schema.version}) - {schema.generatedPojos.length} POJOs
                  </option>
                ))}
              </select>
            ) : (
              <div className="text-sm text-gray-600 p-4 bg-gray-50 rounded-lg">
                No schemas available for deployment. All active schemas are already deployed.
              </div>
            )}
          </div>

          {/* Deployment Options */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-3">
              Select Protocols to Enable
            </label>
            <div className="grid grid-cols-2 gap-3">
              {/* REST */}
              <label className="flex items-start p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={enableRest}
                  onChange={(e) => setEnableRest(e.target.checked)}
                  className="mt-1 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  disabled={deployMutation.isPending}
                />
                <div className="ml-3">
                  <span className="text-sm font-medium text-gray-900">REST</span>
                  <p className="text-xs text-gray-500">HTTP/JSON API</p>
                </div>
              </label>

              {/* SOAP */}
              <label className="flex items-start p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={enableSoap}
                  onChange={(e) => setEnableSoap(e.target.checked)}
                  className="mt-1 h-4 w-4 text-purple-600 focus:ring-purple-500 border-gray-300 rounded"
                  disabled={deployMutation.isPending}
                />
                <div className="ml-3">
                  <span className="text-sm font-medium text-gray-900">SOAP</span>
                  <p className="text-xs text-gray-500">XML Web Service</p>
                </div>
              </label>

              {/* WebSocket */}
              <label className="flex items-start p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={enableWebSocket}
                  onChange={(e) => setEnableWebSocket(e.target.checked)}
                  className="mt-1 h-4 w-4 text-green-600 focus:ring-green-500 border-gray-300 rounded"
                  disabled={deployMutation.isPending}
                />
                <div className="ml-3">
                  <span className="text-sm font-medium text-gray-900">WebSocket</span>
                  <p className="text-xs text-gray-500">Real-time updates</p>
                </div>
              </label>

              {/* gRPC */}
              <label className="flex items-start p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={enableGrpc}
                  onChange={(e) => setEnableGrpc(e.target.checked)}
                  className="mt-1 h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                  disabled={deployMutation.isPending}
                />
                <div className="ml-3">
                  <span className="text-sm font-medium text-gray-900">gRPC</span>
                  <p className="text-xs text-gray-500">High-performance RPC</p>
                </div>
              </label>

              {/* GraphQL */}
              <label className="flex items-start p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={enableGraphQL}
                  onChange={(e) => setEnableGraphQL(e.target.checked)}
                  className="mt-1 h-4 w-4 text-pink-600 focus:ring-pink-500 border-gray-300 rounded"
                  disabled={deployMutation.isPending}
                />
                <div className="ml-3">
                  <span className="text-sm font-medium text-gray-900">GraphQL</span>
                  <p className="text-xs text-gray-500">Flexible queries</p>
                </div>
              </label>

              {/* ActiveMQ */}
              <label className="flex items-start p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={enableActiveMQ}
                  onChange={(e) => setEnableActiveMQ(e.target.checked)}
                  className="mt-1 h-4 w-4 text-orange-600 focus:ring-orange-500 border-gray-300 rounded"
                  disabled={deployMutation.isPending}
                />
                <div className="ml-3">
                  <span className="text-sm font-medium text-gray-900">ActiveMQ</span>
                  <p className="text-xs text-gray-500">Message queues</p>
                </div>
              </label>

              {/* SFTP */}
              <label className="flex items-start p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={enableSftp}
                  onChange={(e) => setEnableSftp(e.target.checked)}
                  className="mt-1 h-4 w-4 text-teal-600 focus:ring-teal-500 border-gray-300 rounded"
                  disabled={deployMutation.isPending}
                />
                <div className="ml-3">
                  <span className="text-sm font-medium text-gray-900">SFTP</span>
                  <p className="text-xs text-gray-500">File-based integration</p>
                </div>
              </label>
            </div>
          </div>

          {selectedSchema && (
            <div className="p-4 bg-blue-50 rounded-lg">
              <h4 className="text-sm font-medium text-blue-900 mb-2">Schema Details</h4>
              <div className="text-sm text-blue-800 space-y-1">
                <p>
                  <strong>Name:</strong> {selectedSchema.serviceName}
                </p>
                <p>
                  <strong>Version:</strong> {selectedSchema.version}
                </p>
                <p>
                  <strong>POJOs:</strong> {selectedSchema.generatedPojos.length}
                </p>
                <p>
                  <strong>Namespace:</strong>{' '}
                  <span className="font-mono text-xs">{selectedSchema.targetNamespace}</span>
                </p>
              </div>
            </div>
          )}
        </div>
      </Modal>

      {/* Detail Modal */}
      {selectedService && (
        <Modal
          isOpen={detailModalOpen}
          onClose={() => {
            setDetailModalOpen(false);
            setSelectedService(null);
          }}
          title={`Service: ${selectedService.service.serviceName}`}
          size="lg"
        >
          <div className="space-y-4">
            {/* Service Info */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Version</p>
                <p className="mt-1 text-sm text-gray-900">{selectedService.service.version}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Status</p>
                <div className="mt-1">
                  <StatusBadge status={selectedService.service.status} />
                </div>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Total Requests</p>
                <p className="mt-1 text-sm text-gray-900">
                  {(selectedService.service.totalRequests || 0).toLocaleString()}
                </p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Avg Response Time</p>
                <p className="mt-1 text-sm text-gray-900">
                  {selectedService.service.averageResponseTime?.toFixed(0) || '0'}ms
                </p>
              </div>
            </div>

            {/* Endpoints */}
            <div>
              <p className="text-sm font-medium text-gray-500 mb-2">
                Endpoints ({selectedService.endpoints.length})
              </p>
              <div className="bg-gray-50 rounded-lg p-3 max-h-60 overflow-y-auto space-y-2">
                {selectedService.endpoints.map((endpoint: any) => (
                  <div
                    key={endpoint.id}
                    className="flex items-center justify-between p-2 bg-white rounded"
                  >
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span
                          className={`px-2 py-1 text-xs font-medium rounded ${
                            endpoint.endpointType === 'REST'
                              ? 'bg-blue-100 text-blue-700'
                              : 'bg-purple-100 text-purple-700'
                          }`}
                        >
                          {endpoint.endpointType}
                        </span>
                        {endpoint.httpMethod && (
                          <span className="px-2 py-1 text-xs font-mono bg-gray-100 text-gray-700 rounded">
                            {endpoint.httpMethod}
                          </span>
                        )}
                        <span className="text-sm font-mono text-gray-900">{endpoint.path}</span>
                      </div>
                      {endpoint.description && (
                        <p className="text-xs text-gray-500 mt-1">{endpoint.description}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* WSDL Link */}
            {selectedService.wsdlAvailable && (
              <div>
                <p className="text-sm font-medium text-gray-500 mb-2">WSDL Document</p>
                <a
                  href={`${API_BASE_URL}/ws/${selectedService.service.serviceName}?wsdl`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 px-4 py-2 bg-purple-50 text-purple-700 rounded-lg hover:bg-purple-100 transition-colors"
                >
                  <FileText size={18} />
                  <span>View WSDL</span>
                  <ExternalLink size={16} />
                </a>
              </div>
            )}
          </div>
        </Modal>
      )}

      {/* Fields View Modal */}
      {fieldsAnalysis && (
        <FieldsViewModal
          isOpen={fieldsModalOpen}
          onClose={() => {
            setFieldsModalOpen(false);
            setFieldsAnalysis(null);
          }}
          serviceName={fieldsAnalysis.rootElement || 'Schema Fields'}
          analysisResult={fieldsAnalysis}
        />
      )}
    </div>
  );
};

export default ServicesPage;
