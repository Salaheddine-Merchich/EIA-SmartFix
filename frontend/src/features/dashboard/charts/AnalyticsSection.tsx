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

    <div className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs shadow-sm">

      <p className="font-medium text-slate-800">{label}</p>

      <p className="mt-1 text-slate-600">{payload[0].value}</p>

    </div>

  );

}



function formatMonthLabel(month: string): string {

  const [year, m] = month.split('-');

  const date = new Date(Number(year), Number(m) - 1, 1);

  return date.toLocaleDateString('fr-FR', { month: 'short', year: 'numeric' });

}



export function AnalyticsSection({ stats }: AnalyticsSectionProps) {

  const familleData = stats.failuresByFamille.map((item) => ({

    name: item.famille,

    value: item.count,

  }));



  const causesData = stats.topCauses.map((item) => ({

    name: item.cause.length > 28 ? `${item.cause.slice(0, 28)}…` : item.cause,

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

    { name: 'Pannes ouvertes', value: stats.openFailures },

  ].filter((item) => item.value > 0);



  return (

    <section className="grid grid-cols-1 gap-4 xl:grid-cols-2">

      <DashboardPanel title="Équipements par famille" subtitle="Répartition des pannes">

        {familleData.length === 0 ? (

          <EmptyState

            title="Aucune donnée disponible"

            description="Les statistiques par famille apparaîtront dès que des pannes seront enregistrées."

          />

        ) : (

          <div className="h-72">

            <ResponsiveContainer width="100%" height="100%">

              <BarChart data={familleData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>

                <CartesianGrid strokeDasharray="3 3" stroke={border} vertical={false} />

                <XAxis dataKey="name" tick={{ fontSize: 11, fill: muted }} />

                <YAxis tick={{ fontSize: 11, fill: muted }} allowDecimals={false} />

                <Tooltip content={<ChartTooltip />} />

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

          <div className="h-72">

            <ResponsiveContainer width="100%" height="100%">

              <BarChart data={causesData} layout="vertical" margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>

                <CartesianGrid strokeDasharray="3 3" stroke={border} horizontal={false} />

                <XAxis type="number" tick={{ fontSize: 11, fill: muted }} allowDecimals={false} />

                <YAxis type="category" dataKey="name" width={120} tick={{ fontSize: 11, fill: muted }} />

                <Tooltip content={<ChartTooltip label={undefined} />} />

                <Bar dataKey="value" fill={brand} radius={[0, 6, 6, 0]} />

              </BarChart>

            </ResponsiveContainer>

          </div>

        )}

      </DashboardPanel>



      <DashboardPanel title="Validation des interventions" subtitle="État du pipeline qualité">

        {validationData.length === 0 ? (

          <EmptyState

            title="Aucune intervention"

            description="Les volumes de validation s'afficheront ici."

          />

        ) : (

          <div className="h-72">

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

                  {validationData.map((entry, index) => (

                    <Cell key={entry.name} fill={palette[index % palette.length]} />

                  ))}

                </Pie>

                <Tooltip content={<ChartTooltip />} />

              </PieChart>

            </ResponsiveContainer>

            <ul className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-3">

              {validationData.map((item, index) => (

                <li key={item.name} className="flex items-center gap-2 text-xs text-slate-600">

                  <span

                    className="h-2.5 w-2.5 rounded-full"

                    style={{ backgroundColor: palette[index % palette.length] }}

                  />

                  <span>{item.name}</span>

                  <span className="ml-auto font-semibold text-slate-800">{item.value}</span>

                </li>

              ))}

            </ul>

          </div>

        )}

      </DashboardPanel>



      <DashboardPanel title="Pannes par mois" subtitle="Tendance temporelle">

        {monthlyData.length === 0 ? (

          <EmptyState

            title="Aucune donnée disponible"

            description="Les tendances mensuelles apparaîtront dès que des pannes seront enregistrées."

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

