/**
 * Services Page - Placeholder
 */

import React from 'react';
import Card from '../components/common/Card';

const ServicesPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Services</h1>
        <p className="mt-2 text-gray-600">
          Manage and monitor deployed services
        </p>
      </div>

      <Card>
        <div className="text-center py-12">
          <p className="text-gray-600">Service management coming soon...</p>
        </div>
      </Card>
    </div>
  );
};

export default ServicesPage;
