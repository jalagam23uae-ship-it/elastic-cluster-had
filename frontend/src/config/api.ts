/**
 * API Configuration
 */

export const API_CONFIG = {
  BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  TIMEOUT: 30000, // 30 seconds
  ENDPOINTS: {
    // Schema Management
    SCHEMA_UPLOAD: '/api/v1/schema/upload',
    SCHEMA_LIST: '/api/v1/schema/list',
    SCHEMA_GET: '/api/v1/schema',
    SCHEMA_DELETE: '/api/v1/schema',

    // Service Management
    SERVICE_DEPLOY: '/api/v1/management/deploy',
    SERVICE_STATUS: '/api/v1/management/status',
    SERVICE_LIST: '/api/v1/management/services',

    // Health & Metrics
    HEALTH: '/actuator/health',
    METRICS: '/actuator/metrics',
  },
} as const;

export default API_CONFIG;
