/**
 * API Configuration
 */

export const API_CONFIG = {
  BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  WS_URL: import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws',
  TIMEOUT: 30000, // 30 seconds
  ENDPOINTS: {
    // Schema Management
    SCHEMA_UPLOAD: '/api/v1/schemas/upload',
    SCHEMA_VALIDATE: '/api/v1/schemas/validate',
    SCHEMA_LIST: '/api/v1/schemas',
    SCHEMA_GET: '/api/v1/schemas',
    SCHEMA_DELETE: '/api/v1/schemas',

    // Service Deployment
    SERVICE_DEPLOY: '/api/v1/services/deploy',
    SERVICE_UNDEPLOY: '/api/v1/services/undeploy',
    SERVICE_STATUS: '/api/v1/services/status',
    SERVICE_WSDL: '/api/v1/services/wsdl',

    // Catalog & Discovery
    CATALOG_SERVICES: '/api/v1/catalog/services',
    CATALOG_SERVICE_DETAILS: '/api/v1/catalog/services',
    CATALOG_SCHEMAS: '/api/v1/catalog/schemas',
    CATALOG_ENDPOINTS: '/api/v1/catalog/endpoints',
    CATALOG_SEARCH: '/api/v1/catalog/search',

    // Management & Monitoring
    MANAGEMENT_RATE_LIMIT: '/api/v1/management/rate-limit',
    MANAGEMENT_AUDIT: '/api/v1/management/audit',
    MANAGEMENT_METRICS: '/api/v1/management/metrics',
    MANAGEMENT_CLEANUP_AUDIT: '/api/v1/management/cleanup/audit-logs',
    MANAGEMENT_CLEANUP_METRICS: '/api/v1/management/cleanup/metrics',

    // Health & Actuator
    HEALTH: '/actuator/health',
    METRICS_ACTUATOR: '/actuator/metrics',
  },
} as const;

export default API_CONFIG;
