/**
 * Main App Component
 */

import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import MainLayout from './components/layout/MainLayout';
import Dashboard from './pages/Dashboard';
import SchemaManagement from './pages/SchemaManagement';
import ServicesPage from './pages/ServicesPage';

// Create QueryClient instance
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<MainLayout />}>
            <Route index element={<Dashboard />} />
            <Route path="schemas" element={<SchemaManagement />} />
            <Route path="services" element={<ServicesPage />} />
            <Route path="testing" element={<PlaceholderPage title="API Testing" />} />
            <Route path="docs" element={<PlaceholderPage title="Documentation" />} />
            <Route path="monitoring" element={<PlaceholderPage title="Monitoring" />} />
            <Route path="settings" element={<PlaceholderPage title="Settings" />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
};

// Placeholder component for routes not yet implemented
const PlaceholderPage: React.FC<{ title: string }> = ({ title }) => (
  <div className="space-y-6">
    <div>
      <h1 className="text-3xl font-bold text-gray-900">{title}</h1>
    </div>
    <div className="bg-white rounded-lg shadow-md border border-gray-200 p-6">
      <div className="text-center py-12">
        <p className="text-gray-600">This page is coming soon...</p>
      </div>
    </div>
  </div>
);

export default App;
