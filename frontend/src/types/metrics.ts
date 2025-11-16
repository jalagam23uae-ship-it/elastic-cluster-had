/**
 * Type definitions for Metrics and Monitoring
 */

import type { ApiResponse } from './api';
export type { ApiResponse };

export type MetricType =
  | 'REQUEST_COUNT'
  | 'RESPONSE_TIME'
  | 'ERROR_RATE'
  | 'SUCCESS_RATE'
  | 'THROUGHPUT'
  | 'MEMORY_USAGE'
  | 'CPU_USAGE';

export interface ServiceMetric {
  id: string;
  serviceDefinitionId: string;
  timestamp: string;
  metricType: MetricType;
  metricValue: number;
  tags: Record<string, any>;
  aggregationWindow: string | null;
}

export interface MetricStatistics {
  metricType: MetricType;
  count: number;
  average: number;
  min: number;
  max: number;
  sum: number;
  stdDev: number;
  percentile50: number;
  percentile95: number;
  percentile99: number;
}

export interface TimeSeriesDataPoint {
  timestamp: string;
  average: number;
  min: number;
  max: number;
  count: number;
}

export interface AuditLog {
  id: string;
  username: string;
  action: AuditAction;
  serviceName: string | null;
  status: AuditStatus;
  details: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  timestamp: string;
  durationMs: number | null;
}

export type AuditAction =
  | 'SCHEMA_UPLOAD'
  | 'SCHEMA_VALIDATE'
  | 'SCHEMA_DELETE'
  | 'SERVICE_DEPLOY'
  | 'SERVICE_UNDEPLOY'
  | 'API_CALL'
  | 'LOGIN'
  | 'LOGOUT';

export type AuditStatus = 'SUCCESS' | 'FAILURE' | 'PARTIAL';

export interface AuditStatistics {
  username: string;
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  successRate: number;
  averageResponseTime: number;
  lastActivity: string;
}

export interface RateLimitStatus {
  identifier: string;
  bucketType: 'USER' | 'SERVICE' | 'IP' | 'GLOBAL';
  tokensRemaining: number;
  maxTokens: number;
  refillRate: number;
  resetAt: string;
  blocked: boolean;
}
