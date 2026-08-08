import { motion } from 'framer-motion';
import { fadeInUp } from '@/features/ai-assistant/animations';
import { AnalyticsSection } from '../charts/AnalyticsSection';
import { RecurringDefectsPanel } from '../components/RecurringDefectsPanel';
import { AlertsPanel } from '../components/AlertsPanel';
import { CriticalEquipmentPanel } from '../components/CriticalEquipmentPanel';
import { DashboardHeader } from '../components/DashboardHeader';
import { ErrorState } from '@/shared/components/ErrorState';
import { KpiGrid } from '../components/KpiGrid';
import { KpiSkeletonGrid, PanelSkeleton } from '../components/SkeletonBlock';
import { QuickActionsGrid } from '../components/QuickActionsGrid';
import { LiveActivityFeed } from '@/features/live';
import { RecentActivityTimeline } from '../components/RecentActivityTimeline';
import { ReliabilityMetrics } from '../components/ReliabilityMetrics';
import { useDashboardData } from '../hooks/useDashboardData';
import { buildKpiCards } from '../utils/buildKpiCards';
import { mapFailuresToActivity, mapFailuresToAlerts } from '../utils/mapActivity';

export default function EnterpriseDashboardPage() {
  const { viewModel, isLoading, isError, refetch } = useDashboardData();

  if (isLoading) {
    return (
      <div className="min-h-full bg-gradient-to-br from-slate-50/80 via-white to-slate-100/40 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950">
        <DashboardHeader />
        <div className="space-y-6 px-4 py-6 sm:px-6">
          <KpiSkeletonGrid />
          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            <PanelSkeleton />
            <PanelSkeleton />
          </div>
        </div>
      </div>
    );
  }

  if (isError || !viewModel) {
    return (
      <div className="min-h-full bg-gradient-to-br from-slate-50/80 via-white to-slate-100/40 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950">
        <DashboardHeader />
        <div className="px-4 py-6 sm:px-6">
          <ErrorState onRetry={refetch} />
        </div>
      </div>
    );
  }

  const kpiCards = buildKpiCards(
    viewModel.stats,
    viewModel.equipmentCount,
    viewModel.criticalFailures.length,
  );

  return (
    <motion.div
      className="min-h-full bg-gradient-to-br from-slate-50/80 via-white to-slate-100/40 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950"
      variants={fadeInUp}
      initial="initial"
      animate="animate"
    >
      <DashboardHeader />

      <div className="space-y-6 px-4 py-6 sm:px-6">
        <KpiGrid cards={kpiCards} />
        <ReliabilityMetrics stats={viewModel.stats} />
        <AnalyticsSection stats={viewModel.stats} />

        <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
          <div className="space-y-4 xl:col-span-2">
            <LiveActivityFeed />
            <RecentActivityTimeline items={mapFailuresToActivity(viewModel.recentFailures)} />
          </div>
          <AlertsPanel alerts={mapFailuresToAlerts(viewModel.criticalFailures)} />
        </div>

        <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
          <CriticalEquipmentPanel equipment={viewModel.stats.topFailingEquipment} />
          <RecurringDefectsPanel />
          <QuickActionsGrid />
        </div>
      </div>
    </motion.div>
  );
}
