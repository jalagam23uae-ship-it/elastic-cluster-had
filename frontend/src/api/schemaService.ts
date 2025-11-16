/**
 * Schema Management API Service
 */

import apiClient from './client';
import { API_CONFIG } from '../config/api';
import type {
  SchemaMetadata,
  SchemaUploadRequest,
  SchemaUploadResponse,
  SchemaListParams,
  PageResponse,
} from '../types/schema';

export const schemaService = {
  /**
   * Upload XSD schema
   */
  async upload(request: SchemaUploadRequest): Promise<SchemaUploadResponse> {
    const formData = new FormData();
    formData.append('file', request.file);
    formData.append('serviceName', request.serviceName);

    if (request.version) formData.append('version', request.version);
    if (request.description) formData.append('description', request.description);
    if (request.namespace) formData.append('namespace', request.namespace);
    if (request.autoDeploy !== undefined) {
      formData.append('autoDeploy', String(request.autoDeploy));
    }

    const response = await apiClient.post<SchemaUploadResponse>(
      API_CONFIG.ENDPOINTS.SCHEMA_UPLOAD,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );

    return response.data;
  },

  /**
   * List schemas with pagination and filters
   */
  async list(params?: SchemaListParams): Promise<PageResponse<SchemaMetadata>> {
    const response = await apiClient.get<PageResponse<SchemaMetadata>>(
      API_CONFIG.ENDPOINTS.SCHEMA_LIST,
      { params }
    );

    return response.data;
  },

  /**
   * Get schema by service name
   */
  async getByServiceName(serviceName: string): Promise<SchemaMetadata> {
    const response = await apiClient.get<SchemaMetadata>(
      `${API_CONFIG.ENDPOINTS.SCHEMA_GET}/${serviceName}`
    );

    return response.data;
  },

  /**
   * Delete schema
   */
  async delete(serviceName: string): Promise<void> {
    await apiClient.delete(`${API_CONFIG.ENDPOINTS.SCHEMA_DELETE}/${serviceName}`);
  },
};

export default schemaService;
