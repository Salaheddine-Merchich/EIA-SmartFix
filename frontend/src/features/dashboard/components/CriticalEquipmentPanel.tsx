import { Link } from 'react-router-dom';
import type { DashboardStats } from '@/shared/types';
import { DashboardPanel } from './DashboardPanel';
import { EmptyState } from './EmptyState';

interface CriticalEquipmentPanelProps {
  equipment: DashboardStats['topFailingEquipment'];
}

export function CriticalEquipmentPanel({ equipment }: CriticalEquipmentPanelProps) {
  return (
    <DashboardPanel title="Équipements critiques" subtitle="Top équipements par nombre de pannes">
      {equipment.length === 0 ? (
        <EmptyState
          title="Aucun équipement critique"
          description="Le classement s'affichera lorsque des pannes seront associées aux équipements."
        />
      ) : (
        <ul className="space-y-3">
          {equipment.map((item, index) => (
            <li
              key={item.equipmentId}
              className="flex items-center gap-3 rounded-xl border border-slate-200 px-4 py-3 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-800/60"
            >
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-slate-900 text-xs font-bold text-white">
                {index + 1}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-slate-900 dark:text-slate-100">{item.code}</p>
                <p className="truncate text-xs text-slate-500 dark:text-slate-400" title={item.designation}>{item.designation}</p>
              </div>
              <div className="text-right">
                <p className="text-sm font-bold text-red-700 dark:text-red-400">{item.failureCount}</p>
                <p className="text-[11px] text-slate-500 dark:text-slate-400">pannes</p>
              </div>
            </li>
          ))}
        </ul>
      )}
      <div className="mt-4">
        <Link to="/equipment" className="text-xs font-medium text-emerald-700 hover:underline dark:text-emerald-400">
          Voir tous les équipements
        </Link>
      </div>
    </DashboardPanel>
  );
}
