/**
 * Type definitions for Schema Management
 */

export type SchemaStatus =
  | 'UPLOADED'
  | 'VALIDATING'
  | 'VALIDATION_FAILED'
  | 'GENERATING'
  | 'GENERATION_FAILED'
  | 'COMPILING'
  | 'COMPILATION_FAILED'
  | 'ACTIVE'
  | 'FAILED'
  | 'DEPRECATED';

export interface SchemaMetadata {
  id: string;
  serviceName: string;
  version: string;
  namespace: string | null;
  description: string | null;
  status: SchemaStatus;
  uploadedBy: string;
  uploadedAt: string;
  updatedAt: string | null;
  generatedArtifacts: GeneratedArtifacts;
  validationErrors: string | null;
  compilationErrors: string | null;
}

export interface GeneratedArtifacts {
  pojos: string[];
  restEndpointCount: number | null;
  soapOperationCount: number | null;
}

export interface SchemaUploadRequest {
  file: File;
  serviceName: string;
  version?: string;
  description?: string;
  namespace?: string;
  autoDeploy?: boolean;
}

export interface SchemaUploadResponse {
  id: string;
  serviceName: string;
  version: string;
  namespace: string | null;
  status: string;
  uploadedAt: string;
  validationWarnings: string[];
  validationErrors: string[];
  message: string;
}

export interface SchemaListParams {
  status?: SchemaStatus;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  numberOfElements: number;
  empty: boolean;
}
