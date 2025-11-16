/**
 * Catalog & Discovery API Service
 */

import apiClient from './client';
import { API_CONFIG } from '../config/api';
import type {
  ServiceCatalogEntry,
  ServiceDetails,
  EndpointMapping,
  ApiResponse,
} from '../types/service';

export interface SchemaCatalogEntry {
  id: string;
  serviceName: string;
  version: string;
  status: string;
  uploadedAt: string;
  uploadedBy: string;
  targetNamespace: string;
  generatedPojos: string[];
  deployed: boolean;
}

export const catalogService = {
  /**
   * List all deployed services
   */
  async listServices(): Promise<ServiceCatalogEntry[]> {
    const response = await apiClient.get<ApiResponse<ServiceCatalogEntry[]>>(
      API_CONFIG.ENDPOINTS.CATALOG_SERVICES
    );
    return response.data.data;
  },

  /**
   * Get detailed service information
   */
  async getServiceDetails(serviceName: string): Promise<ServiceDetails> {
    const response = await apiClient.get<ApiResponse<ServiceDetails>>(
      `${API_CONFIG.ENDPOINTS.CATALOG_SERVICE_DETAILS}/${serviceName}`
    );
    return response.data.data;
  },

  /**
   * List all schemas
   */
  async listSchemas(status?: string): Promise<SchemaCatalogEntry[]> {
    const response = await apiClient.get<ApiResponse<SchemaCatalogEntry[]>>(
      API_CONFIG.ENDPOINTS.CATALOG_SCHEMAS,
      { params: { status } }
    );
    return response.data.data;
  },

  /**
   * List all endpoints
   */
  async listEndpoints(): Promise<EndpointMapping[]> {
    const response = await apiClient.get<ApiResponse<EndpointMapping[]>>(
      API_CONFIG.ENDPOINTS.CATALOG_ENDPOINTS
    );
    return response.data.data;
  },

  /**
   * Search services
   */
  async searchServices(query: string): Promise<ServiceCatalogEntry[]> {
    const response = await apiClient.get<ApiResponse<ServiceCatalogEntry[]>>(
      API_CONFIG.ENDPOINTS.CATALOG_SEARCH,
      { params: { query } }
    );
    return response.data.data;
  },
};

export default catalogService;
