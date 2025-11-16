/**
 * Dashboard Page
 */

import React from 'react';
import Card from '../components/common/Card';
import { FileCode, Server, CheckCircle, TrendingUp } from 'lucide-react';

const Dashboard: React.FC = () => {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-2 text-gray-600">
          Welcome to the Dynamic XSD Service Generation Platform
        </p>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card className="!p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Total Schemas</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">0</p>
            </div>
            <div className="bg-primary-100 p-3 rounded-full">
              <FileCode className="text-primary-600" size={24} />
            </div>
          </div>
        </Card>

        <Card className="!p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Active Services</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">0</p>
            </div>
            <div className="bg-green-100 p-3 rounded-full">
              <Server className="text-green-600" size={24} />
            </div>
          </div>
        </Card>

        <Card className="!p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Success Rate</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">100%</p>
            </div>
            <div className="bg-blue-100 p-3 rounded-full">
              <CheckCircle className="text-blue-600" size={24} />
            </div>
          </div>
        </Card>

        <Card className="!p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Total Requests</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">0</p>
            </div>
            <div className="bg-yellow-100 p-3 rounded-full">
              <TrendingUp className="text-yellow-600" size={24} />
            </div>
          </div>
        </Card>
      </div>

      {/* Quick Actions */}
      <Card title="Quick Actions">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <a
            href="/schemas"
            className="p-4 border-2 border-dashed border-gray-300 rounded-lg hover:border-primary-500 hover:bg-primary-50 transition-colors group"
          >
            <FileCode className="text-gray-400 group-hover:text-primary-600 mb-2" size={32} />
            <h3 className="font-semibold text-gray-900">Upload XSD Schema</h3>
            <p className="mt-1 text-sm text-gray-600">
              Upload and process new XSD schemas
            </p>
          </a>

          <a
            href="/schemas"
            className="p-4 border-2 border-dashed border-gray-300 rounded-lg hover:border-primary-500 hover:bg-primary-50 transition-colors group"
          >
            <Server className="text-gray-400 group-hover:text-primary-600 mb-2" size={32} />
            <h3 className="font-semibold text-gray-900">View Schemas</h3>
            <p className="mt-1 text-sm text-gray-600">
              Browse and manage uploaded schemas
            </p>
          </a>

          <a
            href="/docs"
            className="p-4 border-2 border-dashed border-gray-300 rounded-lg hover:border-primary-500 hover:bg-primary-50 transition-colors group"
          >
            <FileCode className="text-gray-400 group-hover:text-primary-600 mb-2" size={32} />
            <h3 className="font-semibold text-gray-900">API Documentation</h3>
            <p className="mt-1 text-sm text-gray-600">
              View generated API documentation
            </p>
          </a>
        </div>
      </Card>

      {/* Getting Started */}
      <Card title="Getting Started">
        <div className="prose prose-sm max-w-none">
          <p>
            Welcome to the Dynamic XSD Service Generation Platform! This platform allows you to:
          </p>
          <ol>
            <li>Upload XSD schema files</li>
            <li>Automatically generate Java POJOs with JAXB and Jackson annotations</li>
            <li>Compile classes at runtime</li>
            <li>Generate REST APIs supporting JSON and XML</li>
            <li>Generate SOAP Web Services with WSDL</li>
          </ol>
          <p>
            To get started, navigate to the <a href="/schemas" className="text-primary-600 hover:text-primary-700">Schemas</a> page and upload your first XSD file!
          </p>
        </div>
      </Card>
    </div>
  );
};

export default Dashboard;
