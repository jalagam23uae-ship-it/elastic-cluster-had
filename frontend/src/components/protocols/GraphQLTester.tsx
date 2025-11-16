import React, { useState } from 'react';
import { Play, Copy, Check, AlertCircle } from 'lucide-react';

interface GraphQLTesterProps {
  serviceName: string;
}

export function GraphQLTester({ serviceName }: GraphQLTesterProps) {
  const [query, setQuery] = useState(`query {
  ${serviceName}(id: "1") {
    id
    # Add fields here
  }
}`);
  const [result, setResult] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const sampleQueries = {
    query: `query Get${serviceName.charAt(0).toUpperCase() + serviceName.slice(1)} {
  ${serviceName}(id: "1") {
    id
    # Add your fields
  }
}`,
    mutation: `mutation Create${serviceName.charAt(0).toUpperCase() + serviceName.slice(1)} {
  create${serviceName.charAt(0).toUpperCase() + serviceName.slice(1)}(input: {
    # Add your input fields
  }) {
    id
  }
}`,
    list: `query List${serviceName.charAt(0).toUpperCase() + serviceName.slice(1)}s {
  ${serviceName}s(page: 0, pageSize: 10) {
    id
    # Add your fields
  }
}`
  };

  const executeQuery = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('http://localhost:8080/graphql', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ query }),
      });

      const data = await response.json();

      if (data.errors) {
        setError(data.errors.map((e: any) => e.message).join(', '));
        setResult(JSON.stringify(data, null, 2));
      } else {
        setResult(JSON.stringify(data, null, 2));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to execute query');
    } finally {
      setLoading(false);
    }
  };

  const copyResult = () => {
    navigator.clipboard.writeText(result);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const loadSample = (type: keyof typeof sampleQueries) => {
    setQuery(sampleQueries[type]);
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="mb-4">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-lg font-semibold text-gray-900">GraphQL Query Tester</h3>
          <a
            href="http://localhost:8080/graphiql"
            target="_blank"
            rel="noopener noreferrer"
            className="text-sm text-blue-600 hover:text-blue-800"
          >
            Open GraphiQL →
          </a>
        </div>
        <p className="text-sm text-gray-600">
          Test GraphQL queries for {serviceName} service
        </p>
      </div>

      {/* Sample Query Buttons */}
      <div className="mb-4 flex gap-2">
        <button
          onClick={() => loadSample('query')}
          className="px-3 py-1 text-sm bg-gray-100 hover:bg-gray-200 rounded"
        >
          Load Query Sample
        </button>
        <button
          onClick={() => loadSample('mutation')}
          className="px-3 py-1 text-sm bg-gray-100 hover:bg-gray-200 rounded"
        >
          Load Mutation Sample
        </button>
        <button
          onClick={() => loadSample('list')}
          className="px-3 py-1 text-sm bg-gray-100 hover:bg-gray-200 rounded"
        >
          Load List Sample
        </button>
      </div>

      {/* Query Editor */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          GraphQL Query/Mutation
        </label>
        <textarea
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="w-full h-64 px-3 py-2 border border-gray-300 rounded-md font-mono text-sm focus:ring-blue-500 focus:border-blue-500"
          placeholder="Enter your GraphQL query or mutation..."
        />
      </div>

      {/* Execute Button */}
      <button
        onClick={executeQuery}
        disabled={loading || !query.trim()}
        className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
      >
        <Play className="w-4 h-4" />
        {loading ? 'Executing...' : 'Execute Query'}
      </button>

      {/* Error Display */}
      {error && (
        <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-md flex items-start gap-2">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-red-800">Error</p>
            <p className="text-sm text-red-700">{error}</p>
          </div>
        </div>
      )}

      {/* Result Display */}
      {result && (
        <div className="mt-4">
          <div className="flex items-center justify-between mb-2">
            <label className="block text-sm font-medium text-gray-700">
              Result
            </label>
            <button
              onClick={copyResult}
              className="flex items-center gap-1 px-2 py-1 text-sm text-gray-600 hover:text-gray-900"
            >
              {copied ? (
                <>
                  <Check className="w-4 h-4 text-green-600" />
                  <span className="text-green-600">Copied!</span>
                </>
              ) : (
                <>
                  <Copy className="w-4 h-4" />
                  <span>Copy</span>
                </>
              )}
            </button>
          </div>
          <pre className="w-full p-4 bg-gray-50 border border-gray-200 rounded-md overflow-auto max-h-96 text-sm font-mono">
            {result}
          </pre>
        </div>
      )}

      {/* GraphQL Schema Link */}
      <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-md">
        <p className="text-sm text-blue-800">
          <strong>Tip:</strong> Visit{' '}
          <a
            href="http://localhost:8080/graphiql"
            target="_blank"
            rel="noopener noreferrer"
            className="underline hover:no-underline"
          >
            GraphiQL
          </a>{' '}
          for autocomplete, schema exploration, and better query editing experience.
        </p>
      </div>
    </div>
  );
}
