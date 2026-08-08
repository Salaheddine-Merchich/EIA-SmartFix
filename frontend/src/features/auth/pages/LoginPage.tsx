import { type FormEvent, useState } from 'react';
import { Navigate } from 'react-router-dom';
import axios from 'axios';
import {
  EnterpriseButton,
  EnterpriseCard,
  EnterpriseInput,
} from '@/design-system';
import { useAuth } from '@/features/auth/context/AuthContext';

function mapLoginError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return 'Impossible de joindre le serveur. Ouvrez http://127.0.0.1:3000 et vérifiez que le backend tourne.';
    }
    if (error.response.status === 401) {
      return 'Email ou mot de passe incorrect';
    }
    if (error.response.status === 403) {
      return 'Accès refusé par le serveur. Rechargez la page (Ctrl+Shift+R) sur http://127.0.0.1:3000.';
    }
    const body = error.response.data as { message?: string } | undefined;
    if (body?.message) {
      return body.message;
    }
    return `Connexion impossible (erreur ${error.response.status}). Réessayez.`;
  }
  return 'Email ou mot de passe incorrect';
}

export default function LoginPage() {
  const { login, isAuthenticated, isBootstrapping } = useAuth();
  const [email, setEmail] = useState('technicien@ocp.ma');
  const [password, setPassword] = useState('Password123!');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (isBootstrapping) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-100 text-sm text-slate-600 dark:bg-slate-950">
        Restauration de la session…
      </div>
    );
  }

  if (isAuthenticated) return <Navigate to="/" replace />;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    try {
      await login(email, password);
    } catch (err) {
      setError(mapLoginError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4 dark:bg-slate-950">
      <EnterpriseCard className="w-full max-w-md" padding="lg">
        <div className="mb-8 text-center">
          <h1 className="text-xl font-bold tracking-tight text-slate-900 dark:text-slate-100">EIA SmartFix</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">OCP — Connexion sécurisée</p>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <EnterpriseInput
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <EnterpriseInput
            label="Mot de passe"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          {error && <p className="text-sm text-red-600 dark:text-red-400" role="alert">{error}</p>}
          <EnterpriseButton type="submit" loading={loading} className="w-full">
            Se connecter
          </EnterpriseButton>
        </form>
        <p className="mt-6 text-center text-xs text-slate-400">
          Demo: technicien@ocp.ma / Password123!
          <br />
          URL: http://127.0.0.1:3000 (ou :3001 si 3000 occupé)
        </p>
      </EnterpriseCard>
    </div>
  );
}
