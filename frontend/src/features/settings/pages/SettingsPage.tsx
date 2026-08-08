import {
  EnterpriseButton,
  EnterpriseCard,
  EnterprisePageHeader,
  EnterpriseSelect,
  useTheme,
  type ThemeMode,
} from '@/design-system';

export default function SettingsPage() {
  const { mode, setMode, resolvedTheme, toggleTheme } = useTheme();

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <EnterprisePageHeader title="Paramètres" description="Préférences d'affichage et interface" />

      <EnterpriseCard>
        <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Apparence</h2>
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
          Thème actif : {resolvedTheme === 'dark' ? 'Sombre' : 'Clair'}
        </p>
        <div className="mt-4 space-y-4">
          <EnterpriseSelect
            label="Mode d'affichage"
            value={mode}
            onChange={(e) => setMode(e.target.value as ThemeMode)}
          >
            <option value="system">Système</option>
            <option value="light">Clair</option>
            <option value="dark">Sombre</option>
          </EnterpriseSelect>
          <EnterpriseButton variant="secondary" onClick={toggleTheme}>
            Basculer rapidement
          </EnterpriseButton>
        </div>
      </EnterpriseCard>

      <EnterpriseCard>
        <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Accessibilité</h2>
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
          Navigation clavier, focus visibles sur les composants Enterprise, modales piégées et toasts d&apos;erreur assertifs.
          Contrastes vérifiés sur le dashboard et l&apos;assistant IA en modes clair et sombre.
        </p>
      </EnterpriseCard>
    </div>
  );
}
