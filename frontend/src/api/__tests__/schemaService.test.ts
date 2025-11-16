import { describe, it, expect, vi, beforeEach } from 'vitest';
import { schemaService } from '../schemaService';
import apiClient from '../client';

// Mock apiClient
vi.mock('../client');

describe('SchemaService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('upload', () => {
    it('sends FormData with file and metadata', async () => {
      const mockResponse = {
        data: {
          id: '123',
          serviceName: 'test-service',
          status: 'ACTIVE',
        },
      };

      (apiClient.post as any).mockResolvedValue(mockResponse);

      const file = new File(['test'], 'test.xsd', { type: 'text/xml' });
      const request = {
        file,
        serviceName: 'test-service',
        version: '1.0',
      };

      const result = await schemaService.upload(request);

      expect(apiClient.post).toHaveBeenCalled();
      expect(result).toEqual(mockResponse.data);
    });

    it('includes autoDeploy parameter when provided', async () => {
      const mockResponse = {
        data: { id: '123', serviceName: 'test', status: 'ACTIVE' },
      };

      (apiClient.post as any).mockResolvedValue(mockResponse);

      const file = new File(['test'], 'test.xsd', { type: 'text/xml' });
      await schemaService.upload({
        file,
        serviceName: 'test-service',
        autoDeploy: true,
      });

      expect(apiClient.post).toHaveBeenCalled();
    });
  });

  describe('list', () => {
    it('fetches paginated schemas', async () => {
      const mockResponse = {
        data: {
          content: [
            { id: '1', serviceName: 'service-1' },
            { id: '2', serviceName: 'service-2' },
          ],
          totalElements: 2,
          totalPages: 1,
        },
      };

      (apiClient.get as any).mockResolvedValue(mockResponse);

      const result = await schemaService.list({ page: 0, size: 10 });

      expect(apiClient.get).toHaveBeenCalledWith(
        '/api/v1/schemas',
        { params: { page: 0, size: 10 } }
      );
      expect(result).toEqual(mockResponse.data);
    });

    it('includes status filter when provided', async () => {
      (apiClient.get as any).mockResolvedValue({ data: { content: [] } });

      await schemaService.list({ status: 'ACTIVE', page: 0, size: 10 });

      expect(apiClient.get).toHaveBeenCalledWith(
        '/api/v1/schemas',
        { params: { status: 'ACTIVE', page: 0, size: 10 } }
      );
    });
  });

  describe('getByServiceName', () => {
    it('fetches schema by service name', async () => {
      const mockSchema = {
        data: {
          id: '123',
          serviceName: 'test-service',
          version: '1.0',
        },
      };

      (apiClient.get as any).mockResolvedValue(mockSchema);

      const result = await schemaService.getByServiceName('test-service');

      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/schemas/test-service');
      expect(result).toEqual(mockSchema.data);
    });
  });

  describe('delete', () => {
    it('deletes schema by service name', async () => {
      (apiClient.delete as any).mockResolvedValue({});

      await schemaService.delete('test-service');

      expect(apiClient.delete).toHaveBeenCalledWith('/api/v1/schemas/test-service');
    });
  });
});
