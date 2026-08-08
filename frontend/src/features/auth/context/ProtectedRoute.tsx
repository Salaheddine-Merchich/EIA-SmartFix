import { Navigate, Outlet } from 'react-router-dom';

import { useAuth } from './AuthContext';

import type { Role } from '@/shared/types';



export function ProtectedRoute({ roles }: { roles?: Role[] }) {

  const { isAuthenticated, isBootstrapping, hasRole } = useAuth();



  if (isBootstrapping) {

    return (

      <div className="flex min-h-screen items-center justify-center bg-slate-50 text-sm text-slate-600 dark:bg-slate-950 dark:text-slate-400">

        Restauration de la session…

      </div>

    );

  }



  if (!isAuthenticated) {

    return <Navigate to="/login" replace />;

  }



  if (roles && !roles.some((r) => hasRole(r))) {

    return <Navigate to="/" replace />;

  }



  return <Outlet />;

}


