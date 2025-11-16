/**
 * Application Header Component
 */

import React from 'react';
import { Link } from 'react-router-dom';
import { FileCode2, Menu } from 'lucide-react';
import { WebSocketStatus } from '../websocket/WebSocketStatus';

export interface HeaderProps {
  onMenuClick: () => void;
}

const Header: React.FC<HeaderProps> = ({ onMenuClick }) => {
  return (
    <header className="bg-white border-b border-gray-200 shadow-sm sticky top-0 z-40">
      <div className="px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Mobile menu button */}
          <button
            onClick={onMenuClick}
            className="lg:hidden p-2 rounded-md text-gray-600 hover:text-gray-900 hover:bg-gray-100"
          >
            <Menu size={24} />
          </button>

          {/* Logo and Title */}
          <Link to="/" className="flex items-center gap-3">
            <div className="bg-primary-600 p-2 rounded-lg">
              <FileCode2 size={24} className="text-white" />
            </div>
            <div className="hidden sm:block">
              <h1 className="text-xl font-bold text-gray-900">
                XSD Service Platform
              </h1>
              <p className="text-xs text-gray-500">Dynamic Service Generation</p>
            </div>
          </Link>

          {/* Right side actions */}
          <div className="flex items-center gap-4">
            {/* WebSocket Status */}
            <WebSocketStatus showText={true} showStats={false} />

            <div className="text-right hidden md:block">
              <p className="text-sm font-medium text-gray-900">Admin User</p>
              <p className="text-xs text-gray-500">Administrator</p>
            </div>
            <div className="w-10 h-10 rounded-full bg-primary-600 flex items-center justify-center text-white font-semibold">
              A
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
