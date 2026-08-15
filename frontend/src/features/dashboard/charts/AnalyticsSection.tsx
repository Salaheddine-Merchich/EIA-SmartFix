import {

  Bar,

  BarChart,

  CartesianGrid,

  Line,

  LineChart,

  Cell,

  Pie,

  PieChart,

  ResponsiveContainer,

  Tooltip,

  XAxis,

  YAxis,

} from 'recharts';

import type { DashboardStats } from '@/shared/types';

import { DashboardPanel } from '../components/DashboardPanel';

import { EmptyState } from '../components/EmptyState';



const brand = 'var(--color-brand)';
const muted = 'var(--ds-text-muted)';
const border = 'var(--ds-border)';
const palette = [brand, '#0d9488', '#0891b2', '#0284c7', '#6366f1', muted];



interface AnalyticsSectionProps {

  stats: DashboardStats;

}



function ChartTooltip({ active, payload, label }: {
  active?: boolean;
  payload?: { value: number; name?: string }[];
  label?: string;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs shadow-sm dark:border-slate-600 dark:bg-slate-800">
      <p className="font-medium text-slate-800 dark:text-slate-100">{label}</p>
      <p className="mt-1 text-slate-600 dark:text-slate-300">{payload[0].value}</p>
    </div>
  );
}



function CausesChartTooltip({ active, payload }: {
  active?: boolean;
  payload?: { value: number; payload?: { fullName?: string; name?: string } }[];
}) {
  if (!active || !payload?.length) return null;
  const item = payload[0].payload;
  const label = item?.fullName ?? item?.name ?? '';
  return (
    <div className="max-w-xs rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs shadow-sm dark:border-slate-600 dark:bg-slate-800">
      <p className="font-medium text-slate-800 dark:text-slate-100">{label}</p>
      <p className="mt-1 text-slate-600 dark:text-slate-300">{payload[0].value}</p>
    </div>
  );
}

function formatMonthLabel(month: string): string {

  const [year, m] = month.split('-');

  const date = new Date(Number(year), Number(m) - 1, 1);

  return date.toLocaleDateString('fr-FR', { month: 'short', year: 'numeric' });

}

const VALIDATION_ORDER = ['Validées', 'En attente', 'Brouillon', 'Rejetées'] as const;

function validationColor(name: string): string {
  switch (name) {
    case 'Validées':
      return '#059669';
    case 'En attente':
      return '#d97706';
    case 'Brouillon':
      return '#64748b';
    case 'Rejetées':
      return '#dc2626';
    default:
      return brand;
  }
}



export function AnalyticsSection({ stats }: AnalyticsSectionProps) {

  const familleData = stats.failuresByFamille.map((item) => ({

    name: item.famille,

    fullName: item.famille,

    value: item.count,

  }));



  const causesData = stats.topCauses.map((item, index) => ({

    name: `#${index + 1}`,

    fullName: item.cause,

    value: item.count,

  }));



  const monthlyData = (stats.failuresByMonth ?? []).map((item) => ({

    name: formatMonthLabel(item.month),

    value: item.count,

  }));



  const validationData = [
    { name: 'Validées', value: stats.validatedInterventions },
    { name: 'En attente', value: stats.pendingValidations },
    { name: 'Brouillon', value: stats.draftInterventions },
    { name: 'Rejetées', value: stats.rejectedInterventions },
  ]
    .filter((item) => item.value > 0)
    .sort(
      (a, b) =>
        VALIDATION_ORDER.indexOf(a.name as (typeof VALIDATION_ORDER)[number]) -
        VALIDATION_ORDER.indexOf(b.name as (typeof VALIDATION_ORDER)[number]),
    );



  return (

    <section className="grid grid-cols-1 gap-4 xl:grid-cols-2">

      <DashboardPanel title="Pannes par famille" subtitle="Répartition des pannes">

        {familleData.length === 0 ? (

          <EmptyState

            title="Aucune donnée disponible"

            description="Les statistiques par famille apparaîtront dès que des pannes seront enregistrées."

          />

        ) : (

          <div className="h-72">

            <ResponsiveContainer width="100%" height="100%">

              <BarChart data={familleData} margin={{ top: 8, right: 8, left: -16, bottom: 4 }}>

                <CartesianGrid strokeDasharray="3 3" stroke={border} vertical={false} />

                <XAxis
                  dataKey="name"
                  tick={{ fontSize: 11, fill: muted }}
                  angle={-25}
                  textAnchor="end"
                  height={60}
                  interval={0}
                />

                <YAxis tick={{ fontSize: 11, fill: muted }} allowDecimals={false} />

                <Tooltip content={<CausesChartTooltip />} />

                <Bar dataKey="value" radius={[6, 6, 0, 0]}>

                  {familleData.map((entry, index) => (

                    <Cell key={entry.name} fill={palette[index % palette.length]} />

                  ))}

                </Bar>

              </BarChart>

            </ResponsiveContainer>

          </div>

        )}

      </DashboardPanel>



      <DashboardPanel title="Causes les plus fréquentes" subtitle="Top causes identifiées">

        {causesData.length === 0 ? (

          <EmptyState

            title="Aucune cause enregistrée"

            description="Les causes racines validées alimenteront ce graphique."

          />

        ) : (

          <>

            <div className="h-56">

              <ResponsiveContainer width="100%" height="100%">

                <BarChart data={causesData} layout="vertical" margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>

                  <CartesianGrid strokeDasharray="3 3" stroke={border} horizontal={false} />

                  <XAxis type="number" tick={{ fontSize: 11, fill: muted }} allowDecimals={false} />

                  <YAxis
                    type="category"
                    dataKey="name"
                    width={36}
                    tick={{ fontSize: 11, fill: muted }}
                  />

                  <Tooltip content={<CausesChartTooltip />} />

                  <Bar dataKey="value" radius={[0, 6, 6, 0]}>

                    {causesData.map((entry, index) => (

                      <Cell key={entry.fullName} fill={palette[index % palette.length]} />

                    ))}

                  </Bar>

                </BarChart>

              </ResponsiveContainer>

            </div>

            <ul className="mt-3 space-y-2">

              {causesData.map((item, index) => (

                <li key={item.fullName} className="flex items-start gap-2 text-xs">

                  <span

                    className="mt-0.5 h-2.5 w-2.5 shrink-0 rounded-full"

                    style={{ backgroundColor: palette[index % palette.length] }}

                  />

                  <span className="min-w-0 flex-1 text-slate-600 dark:text-slate-300">{item.fullName}</span>

                  <span className="shrink-0 font-semibold text-slate-800 dark:text-slate-100">{item.value}</span>

                </li>

              ))}

            </ul>

          </>

        )}

      </DashboardPanel>



      <DashboardPanel title="Validation des interventions" subtitle="État du pipeline qualité">

        {validationData.length === 0 ? (

          <EmptyState

            title="Aucune intervention"

            description="Les volumes de validation s'afficheront ici."

          />

        ) : (

          <>

            <div className="h-56">

              <ResponsiveContainer width="100%" height="100%">

                <PieChart>

                  <Pie

                    data={validationData}

                    dataKey="value"

                    nameKey="name"

                    innerRadius={58}

                    outerRadius={88}

                    paddingAngle={3}

                  >

                    {validationData.map((entry) => (

                      <Cell key={entry.name} fill={validationColor(entry.name)} />

                    ))}

                  </Pie>

                  <Tooltip content={<ChartTooltip />} />

                </PieChart>

              </ResponsiveContainer>

            </div>

            <ul className="mt-3 space-y-2">

              {validationData.map((item) => (

                <li key={item.name} className="flex items-center gap-2 text-xs text-slate-600 dark:text-slate-300">

                  <span

                    className="h-2.5 w-2.5 shrink-0 rounded-full"

                    style={{ backgroundColor: validationColor(item.name) }}

                  />

                  <span className="min-w-0 flex-1">{item.name}</span>

                  <span className="shrink-0 font-semibold tabular-nums text-slate-800 dark:text-slate-100">{item.value}</span>

                </li>

              ))}

            </ul>

          </>

        )}

      </DashboardPanel>



      <DashboardPanel title="Pannes par mois" subtitle="Tendance par date d'incident">

        {monthlyData.length === 0 ? (

          <EmptyState

            title="Aucune donnée disponible"

            description="Les tendances mensuelles par date d'incident apparaîtront dès que des pannes seront enregistrées."

          />

        ) : (

          <div className="h-72">

            <ResponsiveContainer width="100%" height="100%">

              <LineChart data={monthlyData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>

                <CartesianGrid strokeDasharray="3 3" stroke={border} vertical={false} />

                <XAxis dataKey="name" tick={{ fontSize: 11, fill: muted }} />

                <YAxis tick={{ fontSize: 11, fill: muted }} allowDecimals={false} />

                <Tooltip content={<ChartTooltip />} />

                <Line type="monotone" dataKey="value" stroke={brand} strokeWidth={2} dot={{ r: 4 }} />

              </LineChart>

            </ResponsiveContainer>

          </div>

        )}

      </DashboardPanel>

    </section>

  );

}

