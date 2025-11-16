/**
 * Type definitions for Service Management
 */

import type { ApiResponse } from './api';
export type { ApiResponse };

export type ServiceStatus = 'DEPLOYED' | 'UNDEPLOYED' | 'FAILED' | 'DEPLOYING';

export interface ServiceDefinition {
  id: string;
  serviceName: string;
  version: string;
  schemaId: string;
  status: ServiceStatus;
  deployedAt: string | null;
  deployedBy: string | null;
  wsdlContent: string | null;
  wsdlUrl: string | null;
  totalRequests: number | null;
  averageResponseTime: number | null;
}

export interface ServiceCatalogEntry {
  id: string;
  serviceName: string;
  version: string;
  status: string;
  deployedAt: string;
  totalEndpoints: number;
  restEndpoints: number;
  soapEndpoints: number;
  totalRequests: number;
  averageResponseTime: number;
}

export interface ServiceDetails {
  service: ServiceDefinition;
  schema: any;
  endpoints: EndpointMapping[];
  wsdlAvailable: boolean;
}

export interface EndpointMapping {
  id: string;
  endpointType: 'REST' | 'SOAP';
  path: string;
  httpMethod: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | null;
  operationName: string;
  description: string | null;
  active: boolean;
}

export interface DeployRequest {
  schemaId: string;
  enableRest?: boolean;
  enableSoap?: boolean;
  customPath?: string;
}

export interface DeployResponse {
  success: boolean;
  message: string;
  data: {
    serviceId: string;
    serviceName: string;
    version: string;
    status: string;
    deployedAt: string;
    endpoints: {
      rest: string[];
      soap: string[];
      wsdl: string | null;
    };
  };
}
