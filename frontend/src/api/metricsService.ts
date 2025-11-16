/**
 * Metrics & Monitoring API Service
 */

import apiClient from './client';
import { API_CONFIG } from '../config/api';
import type {
  ServiceMetric,
  MetricStatistics,
  TimeSeriesDataPoint,
  AuditLog,
  AuditStatistics,
  RateLimitStatus,
  MetricType,
  ApiResponse,
} from '../types/metrics';

export const metricsService = {
  /**
   * Get metrics for a service
   */
  async getServiceMetrics(
    serviceId: string,
    metricType?: MetricType,
    startTime?: Date,
    endTime?: Date
  ): Promise<ServiceMetric[]> {
    const params: any = {};
    if (metricType) params.metricType = metricType;
    if (startTime) params.startTime = startTime.toISOString();
    if (endTime) params.endTime = endTime.toISOString();

    const response = await apiClient.get<ApiResponse<ServiceMetric[]>>(
      `${API_CONFIG.ENDPOINTS.MANAGEMENT_METRICS}/${serviceId}`,
      { params }
    );
    return response.data.data;
  },

  /**
   * Get metric statistics
   */
  async getMetricStatistics(
    serviceId: string,
    metricType: MetricType,
    startTime?: Date,
    endTime?: Date
  ): Promise<MetricStatistics> {
    const params: any = { metricType };
    if (startTime) params.startTime = startTime.toISOString();
    if (endTime) params.endTime = endTime.toISOString();

    const response = await apiClient.get<ApiResponse<MetricStatistics>>(
      `${API_CONFIG.ENDPOINTS.MANAGEMENT_METRICS}/${serviceId}/statistics`,
      { params }
    );
    return response.data.data;
  },

  /**
   * Get time series data
   */
  async getTimeSeries(
    serviceId: string,
    metricType: MetricType,
    startTime?: Date,
    endTime?: Date,
    windowMinutes: number = 5
  ): Promise<TimeSeriesDataPoint[]> {
    const params: any = { metricType, windowMinutes };
    if (startTime) params.startTime = startTime.toISOString();
    if (endTime) params.endTime = endTime.toISOString();

    const response = await apiClient.get<ApiResponse<TimeSeriesDataPoint[]>>(
      `${API_CONFIG.ENDPOINTS.MANAGEMENT_METRICS}/${serviceId}/timeseries`,
      { params }
    );
    return response.data.data;
  },

  /**
   * Get rate limit status for a user
   */
  async getRateLimitStatus(username: string): Promise<RateLimitStatus> {
    const response = await apiClient.get<ApiResponse<RateLimitStatus>>(
      `${API_CONFIG.ENDPOINTS.MANAGEMENT_RATE_LIMIT}/${username}`
    );
    return response.data.data;
  },

  /**
   * Get audit logs
   */
  async getAuditLogs(params?: {
    username?: string;
    serviceName?: string;
    startTime?: Date;
    endTime?: Date;
    limit?: number;
  }): Promise<{ logs: AuditLog[] }> {
    const queryParams: any = { limit: params?.limit || 100 };
    if (params?.username) queryParams.username = params.username;
    if (params?.serviceName) queryParams.serviceName = params.serviceName;
    if (params?.startTime) queryParams.startTime = params.startTime.toISOString();
    if (params?.endTime) queryParams.endTime = params.endTime.toISOString();

    const response = await apiClient.get<ApiResponse<{ logs: AuditLog[] }>>(
      API_CONFIG.ENDPOINTS.MANAGEMENT_AUDIT,
      { params: queryParams }
    );
    return response.data.data;
  },

  /**
   * Get user audit statistics
   */
  async getUserAuditStatistics(username: string): Promise<AuditStatistics> {
    const response = await apiClient.get<ApiResponse<AuditStatistics>>(
      `${API_CONFIG.ENDPOINTS.MANAGEMENT_AUDIT}/statistics/${username}`
    );
    return response.data.data;
  },
};

export default metricsService;
