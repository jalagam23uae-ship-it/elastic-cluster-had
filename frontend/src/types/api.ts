/**
 * Common API response types
 */

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  errors?: FieldError[];
  traceId?: string;
}

export interface FieldError {
  field: string;
  message: string;
  rejectedValue: any;
}

export interface ApiError extends Error {
  response?: ErrorResponse;
  status?: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
