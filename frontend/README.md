# Dynamic XSD Service Platform - Frontend

Modern React + TypeScript frontend for the Dynamic XSD Service Generation Platform.

## 🚀 Features

- **React 18** with TypeScript
- **Vite** for fast development
- **Tailwind CSS** for styling
- **React Router** for navigation
- **TanStack Query** for server state management
- **Axios** for HTTP requests
- **Lucide React** for icons

## 📦 Tech Stack

### Core
- React 18
- TypeScript 5
- Vite

### UI & Styling
- Tailwind CSS 3
- Lucide React (icons)

### Data Management
- TanStack Query (React Query) - Server state
- Zustand - Client state
- Axios - HTTP client

### Utilities
- date-fns - Date formatting
- React Router DOM - Routing

## 🛠️ Development

### Prerequisites

- Node.js 18+
- npm or yarn
- Backend API running on http://localhost:8080

### Install Dependencies

```bash
npm install
```

### Run Development Server

```bash
npm run dev
```

The application will be available at http://localhost:3000

### Build for Production

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

## 📁 Project Structure

```
src/
├── api/                  # API service layer
│   ├── client.ts        # Axios client configuration
│   └── schemaService.ts # Schema management API
├── components/
│   ├── common/          # Reusable UI components
│   │   ├── Button.tsx
│   │   ├── Card.tsx
│   │   ├── Modal.tsx
│   │   └── StatusBadge.tsx
│   └── layout/          # Layout components
│       ├── Header.tsx
│       ├── Sidebar.tsx
│       └── MainLayout.tsx
├── config/              # Configuration files
│   └── api.ts          # API configuration
├── pages/              # Page components
│   ├── Dashboard.tsx
│   ├── SchemaManagement.tsx
│   └── ServicesPage.tsx
├── types/              # TypeScript type definitions
│   ├── api.ts
│   └── schema.ts
├── App.tsx            # Main app component
├── main.tsx           # Entry point
└── index.css          # Global styles

## 🎨 UI Components

### Common Components

- **Button** - Reusable button with variants (primary, secondary, danger, outline)
- **Card** - Container component with optional header
- **Modal** - Dialog component with backdrop
- **StatusBadge** - Colored status indicators

### Layout Components

- **Header** - Application header with logo and user info
- **Sidebar** - Navigation sidebar
- **MainLayout** - Main layout wrapper

## 📡 API Integration

### Configuration

API base URL is configured via environment variable:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### API Services

Located in `src/api/`:

- `client.ts` - Axios instance with interceptors
- `schemaService.ts` - Schema management endpoints

### Usage Example

```tsx
import { useQuery } from '@tanstack/react-query';
import schemaService from './api/schemaService';

const { data, isLoading } = useQuery({
  queryKey: ['schemas'],
  queryFn: () => schemaService.list(),
});
```

## 🎯 Available Pages

| Route | Page | Status |
|-------|------|--------|
| `/` | Dashboard | ✅ Implemented |
| `/schemas` | Schema Management | ✅ Implemented |
| `/services` | Services | 🚧 Placeholder |
| `/testing` | API Testing | 🚧 Placeholder |
| `/docs` | Documentation | 🚧 Placeholder |
| `/monitoring` | Monitoring | 🚧 Placeholder |
| `/settings` | Settings | 🚧 Placeholder |

## 🔧 Configuration

### Environment Variables

Create a `.env` file in the frontend directory:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=Dynamic XSD Service Platform
VITE_APP_VERSION=1.0.0
```

### Proxy Configuration

Vite proxy is configured in `vite.config.ts` to forward API requests to the backend:

```ts
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
}
```

## 🎨 Styling

### Tailwind CSS

Configured in `tailwind.config.js` with custom theme:

```js
theme: {
  extend: {
    colors: {
      primary: {
        // Blue color palette
      },
    },
  },
}
```

### Global Styles

Custom global styles in `src/index.css`:

- Custom scrollbar
- Font smoothing
- Base resets

## 📱 Responsive Design

- Mobile-first approach
- Breakpoints: sm (640px), md (768px), lg (1024px), xl (1280px)
- Mobile menu for navigation
- Responsive tables and cards

## 🔐 Features

### Schema Management

- ✅ Upload XSD files
- ✅ List schemas with pagination
- ✅ Search and filter
- ✅ View schema details
- ✅ Delete schemas
- ✅ Real-time status updates

### Dashboard

- ✅ Metrics overview
- ✅ Quick actions
- ✅ Getting started guide

## 🚀 Future Enhancements

- [ ] Service deployment interface
- [ ] API testing playground
- [ ] Real-time monitoring
- [ ] User authentication
- [ ] Dark mode
- [ ] Advanced filtering
- [ ] Export functionality

## 🐛 Troubleshooting

### Port 3000 Already in Use

Change the port in `vite.config.ts`:

```ts
server: {
  port: 3001,
}
```

### API Connection Issues

1. Ensure backend is running on http://localhost:8080
2. Check CORS configuration in backend
3. Verify proxy settings in `vite.config.ts`

### Build Errors

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

## 📄 License

MIT License

## 🤝 Contributing

Contributions welcome! Please open an issue or PR.
