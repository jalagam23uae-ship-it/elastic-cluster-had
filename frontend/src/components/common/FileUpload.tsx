/**
 * File Upload Component with Drag and Drop
 */

import React, { useCallback } from 'react';
import { useDropzone, Accept } from 'react-dropzone';
import { Upload, FileCode, X } from 'lucide-react';

interface FileUploadProps {
  file: File | null;
  onFileSelect: (file: File | null) => void;
  accept?: Accept;
  maxSize?: number; // in bytes
  disabled?: boolean;
  className?: string;
}

const FileUpload: React.FC<FileUploadProps> = ({
  file,
  onFileSelect,
  accept = { 'text/xml': ['.xsd'] },
  maxSize = 5 * 1024 * 1024, // 5MB default
  disabled = false,
  className = '',
}) => {
  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      if (acceptedFiles.length > 0) {
        onFileSelect(acceptedFiles[0]);
      }
    },
    [onFileSelect]
  );

  const { getRootProps, getInputProps, isDragActive, isDragReject, fileRejections } = useDropzone({
    onDrop,
    accept,
    maxSize,
    multiple: false,
    disabled,
  });

  const handleRemove = (e: React.MouseEvent) => {
    e.stopPropagation();
    onFileSelect(null);
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  const getDropzoneClasses = (): string => {
    const baseClasses =
      'border-2 border-dashed rounded-lg p-6 text-center transition-all cursor-pointer';

    if (disabled) {
      return `${baseClasses} border-gray-200 bg-gray-50 cursor-not-allowed`;
    }

    if (isDragReject || fileRejections.length > 0) {
      return `${baseClasses} border-red-400 bg-red-50`;
    }

    if (isDragActive) {
      return `${baseClasses} border-primary-500 bg-primary-50`;
    }

    if (file) {
      return `${baseClasses} border-green-400 bg-green-50`;
    }

    return `${baseClasses} border-gray-300 hover:border-primary-500 hover:bg-gray-50`;
  };

  return (
    <div className={className}>
      <div {...getRootProps()} className={getDropzoneClasses()}>
        <input {...getInputProps()} />

        {file ? (
          // File selected
          <div className="relative">
            <button
              onClick={handleRemove}
              className="absolute top-0 right-0 p-1 text-red-600 hover:text-red-800 hover:bg-red-100 rounded-full"
              disabled={disabled}
              type="button"
            >
              <X size={20} />
            </button>
            <FileCode className="mx-auto text-green-600 mb-3" size={48} />
            <p className="text-sm font-medium text-gray-900">{file.name}</p>
            <p className="text-xs text-gray-500 mt-1">{formatFileSize(file.size)}</p>
            <p className="text-xs text-green-600 mt-2">✓ Ready to upload</p>
          </div>
        ) : isDragActive ? (
          // Dragging file over dropzone
          <div>
            <Upload className="mx-auto text-primary-600 mb-3 animate-bounce" size={48} />
            <p className="text-sm font-medium text-primary-600">Drop the file here</p>
          </div>
        ) : isDragReject || fileRejections.length > 0 ? (
          // Invalid file
          <div>
            <X className="mx-auto text-red-600 mb-3" size={48} />
            <p className="text-sm font-medium text-red-600">Invalid file</p>
            {fileRejections.length > 0 && (
              <p className="text-xs text-red-500 mt-1">
                {fileRejections[0].errors[0].message}
              </p>
            )}
          </div>
        ) : (
          // Default state
          <div>
            <Upload className="mx-auto text-gray-400 mb-3" size={48} />
            <p className="text-sm font-medium text-gray-700">
              Click to upload or drag and drop
            </p>
            <p className="text-xs text-gray-500 mt-1">
              XSD files only (max {formatFileSize(maxSize)})
            </p>
          </div>
        )}
      </div>

      {/* Error messages */}
      {fileRejections.length > 0 && (
        <div className="mt-2 text-sm text-red-600">
          {fileRejections[0].errors.map((error, index) => (
            <p key={index}>{error.message}</p>
          ))}
        </div>
      )}
    </div>
  );
};

export default FileUpload;
