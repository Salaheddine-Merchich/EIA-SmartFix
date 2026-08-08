import { type FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  EnterpriseBadge,
  EnterpriseButton,
  EnterpriseCard,
  EnterpriseErrorState,
  EnterpriseInput,
  EnterpriseModal,
  EnterprisePageHeader,
  EnterpriseSearch,
  EnterpriseSelect,
  EnterpriseSkeletonTable,
  EnterpriseTable,
  EnterpriseTextarea,
  criticiteVariant,
  statutPanneVariant,
  useDisclosure,
} from '@/design-system';
import { useAuth } from '@/features/auth/context/AuthContext';
import { useEquipmentList } from '@/features/equipment/hooks/useEquipmentList';
import { useAssignableUsers } from '@/features/users/hooks/useAssignableUsers';
import { useMutationFeedback } from '@/shared/hooks/useMutationFeedback';
import type { Criticite, StatutPanne } from '@/shared/types';
import { useFailuresList } from '../hooks/useFailuresList';
import { formatInterventionLinkLabel } from '../utils/formatInterventionLinkLabel';

export default function FailuresPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { loading, execute } = useMutationFeedback();
  const [search, setSearch] = useState('');
  const formModal = useDisclosure();

  const { failures, isLoading, isError, refetch, createFailure } = useFailuresList({
    search,
    page: 0,
    size: 50,
  });
  const { equipment } = useEquipmentList({ page: 0, size: 100 });
  const { data: responsables = [] } = useAssignableUsers();

  const [form, setForm] = useState({
    equipmentId: '',
    dateHeure: new Date().toISOString().slice(0, 16),
    criticite: 'MOYENNE' as Criticite,
    zoneService: '',
    responsableId: '',
    descriptionInitiale: '',
    codeDefaut: '',
    statut: 'OUVERTE' as StatutPanne,
  });

  const openCreateModal = () => {
    const defaultResponsableId =
      user && (user.role === 'RESPONSABLE_EIA' || user.role === 'ADMIN')
        ? responsables.find((r) => r.email === user.email)?.id ?? ''
        : '';
    setForm({
      equipmentId: '',
      dateHeure: new Date().toISOString().slice(0, 16),
      criticite: 'MOYENNE',
      zoneService: '',
      responsableId: defaultResponsableId,
      descriptionInitiale: '',
      codeDefaut: '',
      statut: 'OUVERTE',
    });
    formModal.open();
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const created = await execute(
      () =>
        createFailure.mutateAsync({
          ...form,
          dateHeure: new Date(form.dateHeure).toISOString(),
          responsableId: form.responsableId || undefined,
          zoneService: form.zoneService || undefined,
        }),
      {
        successMessage: 'Panne déclarée',
        errorMessage: 'Impossible de déclarer la panne.',
      },
    );
    if (!created) return;
    formModal.close();
    navigate(`/failures/${created.id}?newIntervention=1`);
  };

  return (
    <div className="space-y-6">
      <EnterprisePageHeader
        title="Pannes"
        description="Suivi et déclaration des incidents équipements"
        actions={
          <EnterpriseButton onClick={openCreateModal}>Déclarer une panne</EnterpriseButton>
        }
      />

      <EnterpriseSearch
        placeholder="Rechercher une panne…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />

      <EnterpriseCard padding="none">
        {isLoading && <EnterpriseSkeletonTable rows={6} />}
        {isError && (
          <EnterpriseErrorState
            title="Erreur de chargement"
            message="Impossible de charger la liste des pannes."
            onRetry={() => void refetch()}
          />
        )}
        {!isLoading && !isError && (
          <EnterpriseTable
            data={failures}
            keyExtractor={(f) => f.id}
            emptyMessage="Aucune panne enregistrée"
            columns={[
              {
                key: 'equipment',
                header: 'Équipement',
                render: (f) => (
                  <div>
                    <div className="font-medium text-slate-900 dark:text-slate-100">{f.equipmentCode}</div>
                    <div className="text-xs text-slate-500">{f.equipmentDesignation}</div>
                  </div>
                ),
              },
              {
                key: 'date',
                header: 'Date',
                render: (f) => new Date(f.dateHeure).toLocaleString('fr-FR'),
              },
              {
                key: 'criticite',
                header: 'Criticité',
                render: (f) => <EnterpriseBadge label={f.criticite} variant={criticiteVariant(f.criticite)} />,
              },
              {
                key: 'statut',
                header: 'Statut',
                render: (f) => <EnterpriseBadge label={f.statut} variant={statutPanneVariant(f.statut)} />,
              },
              {
                key: 'zone',
                header: 'Zone / service',
                render: (f) => f.zoneService || '—',
              },
              {
                key: 'declarant',
                header: 'Déclaré par',
                render: (f) => f.declarantNom || '—',
              },
              {
                key: 'responsable',
                header: 'Responsable EIA',
                render: (f) => f.responsableNom || '—',
              },
              {
                key: 'code',
                header: 'Code défaut',
                render: (f) => f.codeDefaut || '—',
              },
              {
                key: 'actions',
                header: 'Actions',
                render: (f) => (
                  <Link
                    to={`/failures/${f.id}`}
                    title="Ouvrir le détail et gérer les interventions"
                    className="text-sm font-medium text-emerald-600 hover:underline dark:text-emerald-400"
                  >
                    {formatInterventionLinkLabel(f)}
                  </Link>
                ),
              },
            ]}
          />
        )}
      </EnterpriseCard>

      <EnterpriseModal
        open={formModal.isOpen}
        onClose={formModal.close}
        title="Nouvelle panne"
        size="lg"
      >
        <form id="failure-form" onSubmit={handleSubmit} className="flex max-h-[calc(100dvh-10rem)] flex-col">
          <div className="min-h-0 flex-1 space-y-4 overflow-y-auto pr-1">
            <EnterpriseSelect
              label="Équipement"
              value={form.equipmentId}
              onChange={(e) => setForm({ ...form, equipmentId: e.target.value })}
              required
            >
              <option value="">Sélectionner un équipement</option>
              {equipment.map((e) => (
                <option key={e.id} value={e.id}>{e.code} — {e.designation}</option>
              ))}
            </EnterpriseSelect>
            <EnterpriseInput
              label="Date et heure"
              type="datetime-local"
              value={form.dateHeure}
              onChange={(e) => setForm({ ...form, dateHeure: e.target.value })}
              required
            />
            <EnterpriseSelect
              label="Criticité"
              value={form.criticite}
              onChange={(e) => setForm({ ...form, criticite: e.target.value as Criticite })}
            >
              {(['FAIBLE', 'MOYENNE', 'HAUTE', 'CRITIQUE'] as const).map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </EnterpriseSelect>
            <EnterpriseSelect
              label="Statut"
              value={form.statut}
              onChange={(e) => setForm({ ...form, statut: e.target.value as StatutPanne })}
            >
              {(['OUVERTE', 'EN_COURS', 'RESOLUE', 'CLOTUREE'] as const).map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </EnterpriseSelect>
            <EnterpriseInput
              label="Zone / service"
              value={form.zoneService}
              onChange={(e) => setForm({ ...form, zoneService: e.target.value })}
              placeholder="Ex. Atelier broyage, Ligne 2"
            />
            <EnterpriseSelect
              label="Responsable EIA (assignation)"
              value={form.responsableId}
              onChange={(e) => setForm({ ...form, responsableId: e.target.value })}
            >
              <option value="">Non assigné</option>
              {responsables.map((u) => (
                <option key={u.id} value={u.id}>{u.nomPrenom} ({u.role})</option>
              ))}
            </EnterpriseSelect>
            <EnterpriseInput
              label="Code défaut"
              value={form.codeDefaut}
              onChange={(e) => setForm({ ...form, codeDefaut: e.target.value })}
            />
            <EnterpriseTextarea
              label="Description initiale"
              value={form.descriptionInitiale}
              onChange={(e) => setForm({ ...form, descriptionInitiale: e.target.value })}
              rows={3}
            />
          </div>
          <div className="mt-4 flex shrink-0 justify-end gap-2 border-t border-slate-100 pt-4 dark:border-slate-800">
            <EnterpriseButton variant="secondary" onClick={formModal.close}>Annuler</EnterpriseButton>
            <EnterpriseButton type="submit" loading={loading}>Enregistrer</EnterpriseButton>
          </div>
        </form>
      </EnterpriseModal>
    </div>
  );
}
