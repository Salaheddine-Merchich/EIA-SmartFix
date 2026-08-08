import { NavLink, Outlet, useLocation } from 'react-router-dom';
import {
  EnterpriseAvatar,
  EnterpriseButton,
  useTheme,
} from '@/design-system';
import { useAuth } from '@/features/auth/context/AuthContext';
import { LiveNotificationCenter, StatusBar } from '@/features/live';

const navItems = [
  { to: '/', label: 'Tableau de bord', roles: ['ADMIN', 'RESPONSABLE_EIA'] as const },
  { to: '/equipment', label: 'Équipements', roles: ['ADMIN', 'RESPONSABLE_EIA', 'TECHNICIEN'] as const },
  { to: '/failures', label: 'Pannes', roles: ['ADMIN', 'RESPONSABLE_EIA', 'TECHNICIEN'] as const },
  { to: '/search', label: 'Knowledge', roles: ['ADMIN', 'RESPONSABLE_EIA', 'TECHNICIEN'] as const },
  { to: '/ai-assistant', label: 'Assistant IA', roles: ['ADMIN', 'RESPONSABLE_EIA', 'TECHNICIEN'] as const },
  { to: '/users', label: 'Utilisateurs', roles: ['ADMIN'] as const },
];

export default function AppLayout() {
  const { user, logout, hasRole } = useAuth();
  const { resolvedTheme, toggleTheme } = useTheme();
  const location = useLocation();
  const isAssistant = location.pathname === '/ai-assistant';
  const isDashboard = location.pathname === '/';
  const isFullBleed = isAssistant || isDashboard;

  return (
    <div className="flex min-h-screen bg-slate-50 dark:bg-slate-950">
      <aside className="hidden w-64 shrink-0 flex-col border-r border-slate-800 bg-slate-950 text-white sm:flex">
        <div className="relative overflow-visible border-b border-slate-800 p-6">
          <div className="flex items-start justify-between gap-2">
            <div>
              <h1 className="text-lg font-bold tracking-tight text-emerald-400">EIA SmartFix</h1>
              <p className="mt-1 text-xs text-slate-500">OCP — Maintenance industrielle</p>
            </div>
            <LiveNotificationCenter />
          </div>
        </div>
        <nav className="flex-1 space-y-0.5 p-3" aria-label="Navigation principale">
          {navItems.filter((item) => item.roles.some((r) => hasRole(r))).map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `block rounded-lg px-3 py-2 text-sm font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-500 ${
                  isActive
                    ? 'bg-emerald-600/90 text-white'
                    : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="space-y-3 border-t border-slate-800 p-4">
          <div className="flex items-center gap-3">
            <EnterpriseAvatar name={user?.nomPrenom ?? 'User'} size="sm" />
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-slate-200">{user?.nomPrenom}</p>
              <p className="truncate text-xs text-slate-500">{user?.role?.replace('_', ' ')}</p>
            </div>
          </div>
          <div className="flex gap-2">
            <NavLink to="/profile" className="flex-1 rounded-lg px-2 py-1.5 text-center text-xs text-slate-400 hover:bg-slate-900 hover:text-slate-200">
              Profil
            </NavLink>
            <NavLink to="/settings" className="flex-1 rounded-lg px-2 py-1.5 text-center text-xs text-slate-400 hover:bg-slate-900 hover:text-slate-200">
              Paramètres
            </NavLink>
          </div>
          <EnterpriseButton
            variant="secondary"
            size="sm"
            className="w-full border-slate-700 bg-slate-900 text-slate-300 hover:bg-slate-800"
            onClick={toggleTheme}
          >
            {resolvedTheme === 'dark' ? 'Mode clair' : 'Mode sombre'}
          </EnterpriseButton>
          <EnterpriseButton
            variant="ghost"
            size="sm"
            className="w-full text-slate-400 hover:bg-slate-900 hover:text-slate-200"
            onClick={logout}
          >
            Déconnexion
          </EnterpriseButton>
        </div>
      </aside>

      <div className="fixed inset-x-0 bottom-0 z-20 border-t border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900 sm:hidden">
        <nav className="flex overflow-x-auto" aria-label="Navigation mobile">
          {navItems.filter((item) => item.roles.some((r) => hasRole(r))).map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `whitespace-nowrap px-3 py-3 text-xs font-medium focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 ${
                  isActive ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-500 dark:text-slate-400'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>

      <main className={`flex min-w-0 flex-1 flex-col ${isFullBleed ? 'h-screen overflow-hidden pb-12 sm:pb-0' : 'overflow-auto pb-12 sm:pb-0'}`}>
        <div className="flex items-center justify-between border-b border-slate-800 bg-slate-950 px-4 py-3 sm:hidden">
          <div>
            <h1 className="text-base font-bold tracking-tight text-emerald-400">EIA SmartFix</h1>
            <p className="text-xs text-slate-500">OCP — Maintenance industrielle</p>
          </div>
          <LiveNotificationCenter />
        </div>
        <StatusBar />
        {!isFullBleed && (
          <header className="border-b border-slate-200 bg-white px-4 py-3 dark:border-slate-800 dark:bg-slate-900 sm:px-6">
            <p className="text-sm font-medium text-slate-700 dark:text-slate-300">EIA SmartFix</p>
          </header>
        )}
        <div className={isFullBleed ? 'min-h-0 flex-1 overflow-auto' : 'p-4 sm:p-6 lg:p-8'}>
          <Outlet />
        </div>
      </main>
    </div>
  );
}
