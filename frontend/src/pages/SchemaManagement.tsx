/**
 * Schema Management Page
 */

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Upload, Search, Filter, RefreshCw, Trash2, Eye } from 'lucide-react';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Modal from '../components/common/Modal';
import StatusBadge from '../components/common/StatusBadge';
import schemaService from '../api/schemaService';
import type { SchemaMetadata, SchemaListParams } from '../types/schema';
import { formatDistanceToNow } from 'date-fns';

const SchemaManagement: React.FC = () => {
  const queryClient = useQueryClient();
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [selectedSchema, setSelectedSchema] = useState<SchemaMetadata | null>(null);

  // Upload form state
  const [file, setFile] = useState<File | null>(null);
  const [serviceName, setServiceName] = useState('');
  const [version, setVersion] = useState('1.0');
  const [description, setDescription] = useState('');

  // List parameters
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  // Fetch schemas
  const { data: schemasData, isLoading, refetch } = useQuery({
    queryKey: ['schemas', { search: searchQuery, status: statusFilter, page, size }],
    queryFn: () => {
      const params: SchemaListParams = { page, size };
      if (searchQuery) params.search = searchQuery;
      if (statusFilter) params.status = statusFilter as any;
      return schemaService.list(params);
    },
  });

  // Upload mutation
  const uploadMutation = useMutation({
    mutationFn: schemaService.upload,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schemas'] });
      setUploadModalOpen(false);
      resetUploadForm();
      alert('Schema uploaded successfully!');
    },
    onError: (error: any) => {
      alert(`Upload failed: ${error.response?.data?.message || error.message}`);
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: schemaService.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schemas'] });
      alert('Schema deleted successfully!');
    },
    onError: (error: any) => {
      alert(`Delete failed: ${error.response?.data?.message || error.message}`);
    },
  });

  const resetUploadForm = () => {
    setFile(null);
    setServiceName('');
    setVersion('1.0');
    setDescription('');
  };

  const handleUpload = (e: React.FormEvent) => {
    e.preventDefault();
    if (!file || !serviceName) {
      alert('Please select a file and enter a service name');
      return;
    }

    uploadMutation.mutate({
      file,
      serviceName,
      version,
      description,
    });
  };

  const handleDelete = (serviceName: string) => {
    if (confirm(`Are you sure you want to delete schema "${serviceName}"?`)) {
      deleteMutation.mutate(serviceName);
    }
  };

  const handleViewDetails = (schema: SchemaMetadata) => {
    setSelectedSchema(schema);
    setDetailModalOpen(true);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Schema Management</h1>
          <p className="mt-2 text-gray-600">
            Upload and manage XSD schemas
          </p>
        </div>
        <Button
          onClick={() => setUploadModalOpen(true)}
          leftIcon={<Upload size={20} />}
        >
          Upload XSD
        </Button>
      </div>

      {/* Filters */}
      <Card>
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="Search by service name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          >
            <option value="">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="FAILED">Failed</option>
            <option value="UPLOADING">Uploading</option>
          </select>
          <Button
            variant="outline"
            onClick={() => refetch()}
            leftIcon={<RefreshCw size={20} />}
          >
            Refresh
          </Button>
        </div>
      </Card>

      {/* Schema List */}
      <Card>
        {isLoading ? (
          <div className="text-center py-12">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            <p className="mt-4 text-gray-600">Loading schemas...</p>
          </div>
        ) : schemasData?.content && schemasData.content.length > 0 ? (
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
                    Uploaded By
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Uploaded
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {schemasData.content.map((schema) => (
                  <tr key={schema.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">
                        {schema.serviceName}
                      </div>
                      {schema.description && (
                        <div className="text-sm text-gray-500">
                          {schema.description}
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {schema.version}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <StatusBadge status={schema.status} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {schema.uploadedBy}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDistanceToNow(new Date(schema.uploadedAt), { addSuffix: true })}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => handleViewDetails(schema)}
                          className="text-primary-600 hover:text-primary-900"
                        >
                          <Eye size={18} />
                        </button>
                        <button
                          onClick={() => handleDelete(schema.serviceName)}
                          className="text-red-600 hover:text-red-900"
                        >
                          <Trash2 size={18} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Pagination */}
            {schemasData.totalPages > 1 && (
              <div className="mt-4 flex items-center justify-between">
                <div className="text-sm text-gray-700">
                  Showing {schemasData.numberOfElements} of {schemasData.totalElements} schemas
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={schemasData.first}
                    onClick={() => setPage(page - 1)}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={schemasData.last}
                    onClick={() => setPage(page + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="text-center py-12">
            <FileCode size={48} className="mx-auto text-gray-400 mb-4" />
            <p className="text-gray-600">No schemas found</p>
            <Button
              className="mt-4"
              onClick={() => setUploadModalOpen(true)}
              leftIcon={<Upload size={20} />}
            >
              Upload Your First XSD
            </Button>
          </div>
        )}
      </Card>

      {/* Upload Modal */}
      <Modal
        isOpen={uploadModalOpen}
        onClose={() => setUploadModalOpen(false)}
        title="Upload XSD Schema"
        footer={
          <>
            <Button variant="outline" onClick={() => setUploadModalOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleUpload}
              isLoading={uploadMutation.isPending}
              leftIcon={<Upload size={20} />}
            >
              Upload
            </Button>
          </>
        }
      >
        <form onSubmit={handleUpload} className="space-y-4">
          {/* File Upload */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              XSD File *
            </label>
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-primary-500 transition-colors">
              <input
                type="file"
                accept=".xsd"
                onChange={(e) => setFile(e.target.files?.[0] || null)}
                className="hidden"
                id="file-upload"
              />
              <label htmlFor="file-upload" className="cursor-pointer">
                {file ? (
                  <div>
                    <FileCode className="mx-auto text-primary-600 mb-2" size={48} />
                    <p className="text-sm font-medium text-gray-900">{file.name}</p>
                    <p className="text-xs text-gray-500 mt-1">
                      {(file.size / 1024).toFixed(2)} KB
                    </p>
                  </div>
                ) : (
                  <div>
                    <Upload className="mx-auto text-gray-400 mb-2" size={48} />
                    <p className="text-sm text-gray-600">
                      Click to upload or drag and drop
                    </p>
                    <p className="text-xs text-gray-500 mt-1">XSD files only (max 5MB)</p>
                  </div>
                )}
              </label>
            </div>
          </div>

          {/* Service Name */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Service Name *
            </label>
            <input
              type="text"
              value={serviceName}
              onChange={(e) => setServiceName(e.target.value)}
              placeholder="e.g., customer-service"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              required
            />
            <p className="mt-1 text-xs text-gray-500">
              Lowercase letters, numbers, and hyphens only
            </p>
          </div>

          {/* Version */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Version
            </label>
            <input
              type="text"
              value={version}
              onChange={(e) => setVersion(e.target.value)}
              placeholder="1.0"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Description
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Brief description of the service..."
              rows={3}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>
        </form>
      </Modal>

      {/* Detail Modal */}
      {selectedSchema && (
        <Modal
          isOpen={detailModalOpen}
          onClose={() => setDetailModalOpen(false)}
          title={selectedSchema.serviceName}
          size="lg"
        >
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Version</p>
                <p className="mt-1 text-sm text-gray-900">{selectedSchema.version}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Status</p>
                <div className="mt-1">
                  <StatusBadge status={selectedSchema.status} />
                </div>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Uploaded By</p>
                <p className="mt-1 text-sm text-gray-900">{selectedSchema.uploadedBy}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Uploaded At</p>
                <p className="mt-1 text-sm text-gray-900">
                  {new Date(selectedSchema.uploadedAt).toLocaleString()}
                </p>
              </div>
            </div>

            {selectedSchema.namespace && (
              <div>
                <p className="text-sm font-medium text-gray-500">Namespace</p>
                <p className="mt-1 text-sm text-gray-900 font-mono bg-gray-50 px-3 py-2 rounded">
                  {selectedSchema.namespace}
                </p>
              </div>
            )}

            {selectedSchema.description && (
              <div>
                <p className="text-sm font-medium text-gray-500">Description</p>
                <p className="mt-1 text-sm text-gray-900">{selectedSchema.description}</p>
              </div>
            )}

            {selectedSchema.generatedArtifacts.pojos.length > 0 && (
              <div>
                <p className="text-sm font-medium text-gray-500 mb-2">Generated POJOs</p>
                <ul className="text-sm text-gray-900 space-y-1 bg-gray-50 px-3 py-2 rounded max-h-40 overflow-y-auto">
                  {selectedSchema.generatedArtifacts.pojos.map((pojo, index) => (
                    <li key={index} className="font-mono text-xs">
                      {pojo}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {selectedSchema.compilationErrors && (
              <div>
                <p className="text-sm font-medium text-red-600">Compilation Errors</p>
                <pre className="mt-1 text-xs text-red-900 bg-red-50 px-3 py-2 rounded overflow-x-auto">
                  {selectedSchema.compilationErrors}
                </pre>
              </div>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
};

export default SchemaManagement;
