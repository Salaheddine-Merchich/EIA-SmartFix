import { lazy, Suspense } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from '@/shared/queryClient';
import {
  createBrowserRouter,
  Navigate,
  Outlet,
  RouterProvider,
} from 'react-router-dom';
import { DesignSystemProviders, EnterprisePageLoader } from '@/design-system';
import { AuthProvider, useAuth } from '@/features/auth/context/AuthContext';
import { LiveProvider } from '@/features/live';
import { ProtectedRoute } from '@/features/auth/context/ProtectedRoute';
import AppLayout from '@/layouts/AppLayout';
import LoginPage from '@/features/auth/pages/LoginPage';
import EquipmentPage from '@/features/equipment/pages/EquipmentPage';
import FailuresPage from '@/features/failures/pages/FailuresPage';
import FailureDetailPage from '@/features/failures/pages/FailureDetailPage';
import SearchPage from '@/features/search/pages/SearchPage';
import { AiConversationProvider } from '@/features/ai-assistant/context/AiConversationProvider';
import UsersPage from '@/features/users/pages/UsersPage';
import ProfilePage from '@/features/profile/pages/ProfilePage';
import SettingsPage from '@/features/settings/pages/SettingsPage';

const EnterpriseDashboardPage = lazy(
  () => import('@/features/dashboard/pages/EnterpriseDashboardPage'),
);
const AiAssistantPage = lazy(() => import('@/features/ai-assistant/pages/AiAssistantPage'));

function withSuspense(element: React.ReactNode) {
  return <Suspense fallback={<EnterprisePageLoader />}>{element}</Suspense>;
}

function HomeRedirect() {
  const { hasRole } = useAuth();
  if (hasRole('ADMIN', 'RESPONSABLE_EIA')) {
    return withSuspense(<EnterpriseDashboardPage />);
  }
  return <Navigate to="/failures" replace />;
}

function AppShell() {
  return (
    <AuthProvider>
      <AiConversationProvider>
        <LiveProvider>
          <Outlet />
        </LiveProvider>
      </AiConversationProvider>
    </AuthProvider>
  );
}

const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      { path: '/login', element: <LoginPage /> },
      {
        element: <ProtectedRoute />,
        children: [
          {
            element: <AppLayout />,
            children: [
              { index: true, element: <HomeRedirect /> },
              { path: 'equipment', element: <EquipmentPage /> },
              { path: 'failures', element: <FailuresPage /> },
              { path: 'failures/:id', element: <FailureDetailPage /> },
              { path: 'search', element: <SearchPage /> },
              { path: 'ai-assistant', element: withSuspense(<AiAssistantPage />) },
              { path: 'profile', element: <ProfilePage /> },
              { path: 'settings', element: <SettingsPage /> },
              {
                element: <ProtectedRoute roles={['ADMIN']} />,
                children: [{ path: 'users', element: <UsersPage /> }],
              },
            ],
          },
        ],
      },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
]);

export default function App() {
  return (
    <DesignSystemProviders>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </DesignSystemProviders>
  );
}
