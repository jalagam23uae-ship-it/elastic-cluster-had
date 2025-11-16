/**
 * Status Badge Component
 */

import React from 'react';
import type { SchemaStatus } from '../../types/schema';

export interface StatusBadgeProps {
  status: SchemaStatus | string;
}

const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  const getStatusStyles = (status: string) => {
    switch (status) {
      case 'ACTIVE':
      case 'DEPLOYED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'FAILED':
      case 'VALIDATION_FAILED':
      case 'GENERATION_FAILED':
      case 'COMPILATION_FAILED':
      case 'DEPLOYMENT_FAILED':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'VALIDATING':
      case 'GENERATING':
      case 'COMPILING':
      case 'DEPLOYING':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'UPLOADED':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'DEPRECATED':
      case 'UNDEPLOYED':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const formatStatus = (status: string) => {
    return status
      .split('_')
      .map(word => word.charAt(0) + word.slice(1).toLowerCase())
      .join(' ');
  };

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${getStatusStyles(status)}`}
    >
      {formatStatus(status)}
    </span>
  );
};

export default StatusBadge;
