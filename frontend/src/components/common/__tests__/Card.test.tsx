import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Card from '../Card';

describe('Card Component', () => {
  it('renders children correctly', () => {
    render(
      <Card>
        <div>Card Content</div>
      </Card>
    );

    expect(screen.getByText('Card Content')).toBeInTheDocument();
  });

  it('renders with title', () => {
    render(
      <Card title="Test Title">
        <div>Content</div>
      </Card>
    );

    expect(screen.getByText('Test Title')).toBeInTheDocument();
    expect(screen.getByText('Content')).toBeInTheDocument();
  });

  it('renders header actions when provided', () => {
    const actions = <button>Action</button>;
    render(
      <Card title="Title" actions={actions}>
        <div>Content</div>
      </Card>
    );

    expect(screen.getByText('Action')).toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = render(
      <Card className="custom-class">Content</Card>
    );

    const card = container.firstChild as HTMLElement;
    expect(card.className).toContain('custom-class');
  });

  it('has default card styling', () => {
    const { container } = render(<Card>Content</Card>);

    const card = container.firstChild as HTMLElement;
    expect(card.className).toContain('bg-white');
    expect(card.className).toContain('rounded-lg');
  });
});
