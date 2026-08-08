import {
  EnterpriseAvatar,
  EnterpriseBadge,
  EnterpriseCard,
  EnterprisePageHeader,
  EnterprisePanel,
} from '@/design-system';
import { useAuth } from '@/features/auth/context/AuthContext';

export default function ProfilePage() {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <EnterprisePageHeader title="Profil" description="Informations de votre compte" />
      <EnterpriseCard>
        <div className="flex items-center gap-4">
          <EnterpriseAvatar name={user.nomPrenom} size="lg" />
          <div>
            <p className="text-lg font-semibold text-slate-900 dark:text-slate-100">{user.nomPrenom}</p>
            <p className="text-sm text-slate-500 dark:text-slate-400">{user.email}</p>
          </div>
        </div>
      </EnterpriseCard>
      <EnterprisePanel title="Rôle et accès">
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-sm text-slate-600 dark:text-slate-400">Rôle</span>
            <EnterpriseBadge label={user.role.replace('_', ' ')} variant="info" />
          </div>
          <div className="flex items-center justify-between">
            <span className="text-sm text-slate-600 dark:text-slate-400">Organisation</span>
            <span className="text-sm font-medium text-slate-900 dark:text-slate-100">OCP — EIA SmartFix</span>
          </div>
        </div>
      </EnterprisePanel>
    </div>
  );
}
