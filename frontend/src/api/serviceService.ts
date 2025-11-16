/**
 * Service Management API Service
 */

import apiClient from './client';
import { API_CONFIG } from '../config/api';
import type {
  ServiceDefinition,
  DeployRequest,
  DeployResponse,
  ApiResponse,
} from '../types/service';

export const serviceService = {
  /**
   * Deploy a schema as a service
   */
  async deploy(schemaId: string, request?: Partial<DeployRequest>): Promise<DeployResponse> {
    const response = await apiClient.post<DeployResponse>(
      `${API_CONFIG.ENDPOINTS.SERVICE_DEPLOY}/${schemaId}`,
      request || {}
    );
    return response.data;
  },

  /**
   * Undeploy a service
   */
  async undeploy(serviceId: string): Promise<ApiResponse<null>> {
    const response = await apiClient.post<ApiResponse<null>>(
      `${API_CONFIG.ENDPOINTS.SERVICE_UNDEPLOY}/${serviceId}`
    );
    return response.data;
  },

  /**
   * Get service deployment status
   */
  async getStatus(serviceId: string): Promise<ServiceDefinition> {
    const response = await apiClient.get<ServiceDefinition>(
      `${API_CONFIG.ENDPOINTS.SERVICE_STATUS}/${serviceId}`
    );
    return response.data;
  },

  /**
   * Get WSDL for a SOAP service
   */
  async getWsdl(serviceName: string): Promise<string> {
    const response = await apiClient.get<string>(
      `${API_CONFIG.ENDPOINTS.SERVICE_WSDL}/${serviceName}`,
      {
        params: { wsdl: '' },
        headers: {
          'Accept': 'text/xml',
        },
      }
    );
    return response.data;
  },
};

export default serviceService;
