import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import FileUpload from '../FileUpload';

describe('FileUpload Component', () => {
  it('renders dropzone correctly', () => {
    const onFileSelect = vi.fn();
    render(<FileUpload file={null} onFileSelect={onFileSelect} />);

    expect(screen.getByText(/click to upload or drag and drop/i)).toBeInTheDocument();
  });

  it('displays selected file information', () => {
    const onFileSelect = vi.fn();
    const file = new File(['test'], 'test.xsd', { type: 'text/xml' });

    render(<FileUpload file={file} onFileSelect={onFileSelect} />);

    expect(screen.getByText('test.xsd')).toBeInTheDocument();
    expect(screen.getByText(/Ready to upload/i)).toBeInTheDocument();
  });

  it('calls onFileSelect when file is dropped', async () => {
    const onFileSelect = vi.fn();
    render(<FileUpload file={null} onFileSelect={onFileSelect} />);

    const file = new File(['test content'], 'test.xsd', { type: 'text/xml' });
    const dropzone = screen.getByText(/click to upload or drag and drop/i).closest('div');

    Object.defineProperty(dropzone, 'files', {
      value: [file],
    });

    if (dropzone) {
      fireEvent.drop(dropzone, {
        dataTransfer: {
          files: [file],
        },
      });

      await waitFor(() => {
        expect(onFileSelect).toHaveBeenCalledWith(file);
      });
    }
  });

  it('removes file when remove button is clicked', () => {
    const onFileSelect = vi.fn();
    const file = new File(['test'], 'test.xsd', { type: 'text/xml' });

    render(<FileUpload file={file} onFileSelect={onFileSelect} />);

    const removeButton = screen.getByRole('button');
    fireEvent.click(removeButton);

    expect(onFileSelect).toHaveBeenCalledWith(null);
  });

  it('disables interaction when disabled prop is true', () => {
    const onFileSelect = vi.fn();
    render(<FileUpload file={null} onFileSelect={onFileSelect} disabled />);

    const dropzone = screen.getByText(/click to upload or drag and drop/i).closest('div');
    expect(dropzone?.className).toContain('cursor-not-allowed');
  });

  it('shows file size correctly', () => {
    const onFileSelect = vi.fn();
    const file = new File(['a'.repeat(1024)], 'large.xsd', { type: 'text/xml' });

    render(<FileUpload file={file} onFileSelect={onFileSelect} />);

    expect(screen.getByText(/1.00 KB/i)).toBeInTheDocument();
  });

  it('validates file type', () => {
    const onFileSelect = vi.fn();
    render(
      <FileUpload
        file={null}
        onFileSelect={onFileSelect}
        accept={{ 'text/xml': ['.xsd'] }}
      />
    );

    expect(screen.getByText(/XSD files only/i)).toBeInTheDocument();
  });
});
