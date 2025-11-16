/**
 * Application Sidebar Component
 */

import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  FileCode,
  Server,
  TestTube,
  FileText,
  BarChart3,
  Settings,
  X,
} from 'lucide-react';

export interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

interface NavItem {
  name: string;
  path: string;
  icon: React.ReactNode;
}

const navItems: NavItem[] = [
  { name: 'Dashboard', path: '/', icon: <LayoutDashboard size={20} /> },
  { name: 'Schemas', path: '/schemas', icon: <FileCode size={20} /> },
  { name: 'Services', path: '/services', icon: <Server size={20} /> },
  { name: 'API Testing', path: '/testing', icon: <TestTube size={20} /> },
  { name: 'Documentation', path: '/docs', icon: <FileText size={20} /> },
  { name: 'Monitoring', path: '/monitoring', icon: <BarChart3 size={20} /> },
  { name: 'Settings', path: '/settings', icon: <Settings size={20} /> },
];

const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  return (
    <>
      {/* Mobile overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`
          fixed lg:static inset-y-0 left-0 z-50
          w-64 bg-white border-r border-gray-200
          transform transition-transform duration-300 ease-in-out
          ${isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
        `}
      >
        {/* Mobile close button */}
        <div className="lg:hidden flex justify-end p-4">
          <button
            onClick={onClose}
            className="p-2 rounded-md text-gray-600 hover:text-gray-900 hover:bg-gray-100"
          >
            <X size={24} />
          </button>
        </div>

        {/* Navigation */}
        <nav className="px-4 py-6 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
                }`
              }
            >
              {item.icon}
              {item.name}
            </NavLink>
          ))}
        </nav>

        {/* Version info */}
        <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-gray-200">
          <p className="text-xs text-gray-500">
            Version 1.0.0
          </p>
          <p className="text-xs text-gray-400 mt-1">
            Powered by Spring Boot & React
          </p>
        </div>
      </aside>
    </>
  );
};

export default Sidebar;
