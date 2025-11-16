/**
 * Fields View Modal Component
 * Displays detailed XSD field analysis with validation rules and constraints
 */

import React, { useState } from 'react';
import { FileText, Search, CheckCircle, XCircle, Info } from 'lucide-react';
import Modal from '../common/Modal';

interface XsdFieldInfo {
  name: string;
  type: string;
  minOccurs: string;
  maxOccurs: string;
  required: boolean;
  validations: Record<string, any>;
  enumValues: string[];
  pattern?: string;
  minLength?: string;
  maxLength?: string;
  minInclusive?: string;
  maxInclusive?: string;
  documentation?: string;
}

interface XsdAnalysisResult {
  targetNamespace: string;
  rootElement: string;
  fields: XsdFieldInfo[];
  totalFields: number;
  success: boolean;
  message: string;
}

interface FieldsViewModalProps {
  isOpen: boolean;
  onClose: () => void;
  serviceName: string;
  analysisResult: XsdAnalysisResult | null;
}

const FieldsViewModal: React.FC<FieldsViewModalProps> = ({
  isOpen,
  onClose,
  serviceName,
  analysisResult,
}) => {
  const [searchQuery, setSearchQuery] = useState('');

  if (!analysisResult) {
    return null;
  }

  // Filter fields based on search
  const filteredFields = React.useMemo(() => {
    if (!searchQuery) return analysisResult.fields;
    const query = searchQuery.toLowerCase();
    return analysisResult.fields.filter((field) =>
      field.name.toLowerCase().includes(query) ||
      field.type.toLowerCase().includes(query) ||
      field.documentation?.toLowerCase().includes(query)
    );
  }, [analysisResult.fields, searchQuery]);

  const renderValidations = (field: XsdFieldInfo) => {
    const validations = [];

    if (field.pattern) {
      validations.push(
        <div key="pattern" className="text-xs">
          <span className="font-medium text-purple-700">Pattern:</span>{' '}
          <code className="bg-purple-50 px-1 rounded">{field.pattern}</code>
        </div>
      );
    }

    if (field.minLength || field.maxLength) {
      validations.push(
        <div key="length" className="text-xs">
          <span className="font-medium text-blue-700">Length:</span>{' '}
          {field.minLength && `min: ${field.minLength}`}
          {field.minLength && field.maxLength && ', '}
          {field.maxLength && `max: ${field.maxLength}`}
        </div>
      );
    }

    if (field.minInclusive || field.maxInclusive) {
      validations.push(
        <div key="range" className="text-xs">
          <span className="font-medium text-green-700">Range:</span>{' '}
          {field.minInclusive && `min: ${field.minInclusive}`}
          {field.minInclusive && field.maxInclusive && ', '}
          {field.maxInclusive && `max: ${field.maxInclusive}`}
        </div>
      );
    }

    if (field.enumValues && field.enumValues.length > 0) {
      validations.push(
        <div key="enum" className="text-xs">
          <span className="font-medium text-orange-700">Expected Values ({field.enumValues.length}):</span>
          <div className="mt-1 flex flex-wrap gap-1">
            {field.enumValues.slice(0, 10).map((value, i) => (
              <span
                key={i}
                className="px-2 py-0.5 text-xs bg-orange-50 text-orange-700 border border-orange-200 rounded"
              >
                {value}
              </span>
            ))}
            {field.enumValues.length > 10 && (
              <span className="px-2 py-0.5 text-xs text-orange-600 italic">
                +{field.enumValues.length - 10} more
              </span>
            )}
          </div>
        </div>
      );
    }

    return validations.length > 0 ? (
      <div className="mt-2 space-y-1 p-2 bg-gray-50 rounded">
        <div className="text-xs font-medium text-gray-700 mb-1">Validation Rules:</div>
        {validations}
      </div>
    ) : null;
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`XSD Fields: ${serviceName}`} size="xl">
      <div className="space-y-4">
        {/* Schema Information */}
        <div className="bg-blue-50 rounded-lg p-4 border border-blue-200">
          <div className="flex items-center gap-2 mb-2">
            <FileText className="text-blue-600" size={20} />
            <h3 className="text-sm font-semibold text-blue-900">Schema Information</h3>
          </div>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <span className="text-blue-700 font-medium">Target Namespace:</span>
              <div className="text-blue-900 font-mono text-xs mt-1 break-all">
                {analysisResult.targetNamespace || 'N/A'}
              </div>
            </div>
            <div>
              <span className="text-blue-700 font-medium">Root Element:</span>
              <div className="text-blue-900 font-mono text-xs mt-1">
                {analysisResult.rootElement || 'N/A'}
              </div>
            </div>
          </div>
          <div className="mt-2 pt-2 border-t border-blue-200">
            <span className="text-sm font-medium text-blue-700">Total Fields:</span>{' '}
            <span className="text-lg font-bold text-blue-900">{analysisResult.totalFields}</span>
          </div>
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="Search fields by name, type, or documentation..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          />
        </div>

        {/* Fields List */}
        <div className="max-h-[500px] overflow-y-auto space-y-3">
          {filteredFields.length > 0 ? (
            filteredFields.map((field, index) => (
              <div
                key={index}
                className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow bg-white"
              >
                {/* Field Header */}
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-mono text-sm font-semibold text-gray-900">
                        {index + 1}. {field.name}
                      </span>
                      {field.required ? (
                        <span className="px-2 py-0.5 text-xs font-medium bg-red-100 text-red-700 rounded">
                          Required
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 text-xs font-medium bg-gray-100 text-gray-600 rounded">
                          Optional
                        </span>
                      )}
                    </div>

                    {/* Type and Occurrences */}
                    <div className="flex flex-wrap gap-3 text-xs text-gray-600 mb-2">
                      <div>
                        <span className="font-medium">Type:</span>{' '}
                        <span className={`font-mono ${field.type.includes('ComplexType') ? 'text-purple-600' : 'text-blue-600'}`}>
                          {field.type}
                        </span>
                      </div>
                      <div>
                        <span className="font-medium">Min Occurs:</span> {field.minOccurs}
                      </div>
                      <div>
                        <span className="font-medium">Max Occurs:</span> {field.maxOccurs}
                      </div>
                    </div>

                    {/* Documentation */}
                    {field.documentation && (
                      <div className="flex items-start gap-2 mt-2 p-2 bg-blue-50 rounded border-l-2 border-blue-400">
                        <Info size={14} className="text-blue-600 mt-0.5 flex-shrink-0" />
                        <span className="text-xs text-blue-900">{field.documentation}</span>
                      </div>
                    )}

                    {/* Validations */}
                    {renderValidations(field)}

                    {/* Full Enum Values (if any) */}
                    {field.enumValues && field.enumValues.length > 0 && (
                      <details className="mt-2">
                        <summary className="text-xs text-gray-600 cursor-pointer hover:text-gray-900">
                          View all {field.enumValues.length} allowed values
                        </summary>
                        <div className="mt-2 p-2 bg-gray-50 rounded max-h-32 overflow-y-auto">
                          <div className="flex flex-wrap gap-1">
                            {field.enumValues.map((value, i) => (
                              <span
                                key={i}
                                className="px-2 py-1 text-xs bg-white border border-gray-200 rounded"
                              >
                                {value}
                              </span>
                            ))}
                          </div>
                        </div>
                      </details>
                    )}
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="text-center py-12">
              <Search className="mx-auto text-gray-400 mb-3" size={48} />
              <p className="text-gray-600">No fields found matching your search</p>
            </div>
          )}
        </div>

        {/* Footer Summary */}
        <div className="pt-3 border-t border-gray-200 text-sm text-gray-600">
          Showing <span className="font-semibold">{filteredFields.length}</span> of{' '}
          <span className="font-semibold">{analysisResult.totalFields}</span> fields
        </div>
      </div>
    </Modal>
  );
};

export default FieldsViewModal;
