import { type FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  EnterpriseBadge,
  EnterpriseButton,
  EnterpriseCard,
  EnterpriseInput,
  EnterpriseModal,
  EnterpriseErrorState,
  EnterprisePageHeader,
  EnterpriseSearch,
  EnterpriseSectionTitle,
  EnterpriseSkeletonTable,
  EnterpriseTable,
  useDisclosure,
  useEnterpriseConfirm,
  criticiteVariant,
  statutPanneVariant,
  validationVariant,
} from '@/design-system';
import { useMutationFeedback } from '@/shared/hooks/useMutationFeedback';
import { useAuth } from '@/features/auth/context/AuthContext';
import type { Equipment, Failure, Intervention } from '@/shared/types';
import { useEquipmentHistory } from '../hooks/useEquipmentHistory';
import { useEquipmentList } from '../hooks/useEquipmentList';

export default function EquipmentPage() {
  const { hasRole } = useAuth();
  const { confirm } = useEnterpriseConfirm();
  const { loading, execute } = useMutationFeedback();
  const [search, setSearch] = useState('');
  const formModal = useDisclosure();
  const historyModal = useDisclosure();
  const [form, setForm] = useState({ code: '', designation: '', famille: '', zone: '', constructeur: '' });
  const [editId, setEditId] = useState<string | null>(null);
  const [historyEquipmentId, setHistoryEquipmentId] = useState<string | null>(null);
  const [historyEquipmentCode, setHistoryEquipmentCode] = useState('');

  const {
    equipment: items,
    isLoading,
    isError,
    refetch,
    createEquipment,
    updateEquipment,
    deleteEquipment,
  } = useEquipmentList({ search, page: 0, size: 50 });

  const historyQuery = useEquipmentHistory(historyEquipmentId, historyModal.isOpen);
  const history = historyQuery.data ?? null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const result = await execute(
      async () => {
        if (editId) return updateEquipment.mutateAsync({ id: editId, data: form });
        return createEquipment.mutateAsync(form);
      },
      {
        successMessage: editId ? 'Équipement mis à jour' : 'Équipement créé',
        errorMessage: 'Impossible d\'enregistrer l\'équipement.',
      },
    );

    if (!result) return;
    formModal.close();
    setEditId(null);
    setForm({ code: '', designation: '', famille: '', zone: '', constructeur: '' });
  };

  const handleDelete = async (id: string) => {
    const ok = await confirm({
      title: 'Supprimer l\'équipement',
      message: 'Cette action est irréversible.',
      confirmLabel: 'Supprimer',
      variant: 'danger',
    });
    if (!ok) return;
    await execute(() => deleteEquipment.mutateAsync(id).then(() => true), {
      successMessage: 'Équipement supprimé',
      errorMessage: 'Impossible de supprimer l\'équipement.',
    });
  };

  const showHistory = (equipment: Equipment) => {
    setHistoryEquipmentId(equipment.id);
    setHistoryEquipmentCode(equipment.code);
    historyModal.open();
  };

  return (
    <div className="space-y-6">
      <EnterprisePageHeader
        title="Équipements"
        description="Parc industriel et historique de maintenance"
        actions={
          hasRole('ADMIN') && (
            <EnterpriseButton onClick={() => { setEditId(null); formModal.open(); }}>
              Ajouter
            </EnterpriseButton>
          )
        }
      />

      <EnterpriseSearch
        placeholder="Rechercher un équipement…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />

      <EnterpriseCard padding="none">
        {isLoading && <EnterpriseSkeletonTable rows={6} />}
        {isError && (
          <EnterpriseErrorState
            title="Erreur de chargement"
            message="Impossible de charger la liste des équipements."
            onRetry={() => void refetch()}
          />
        )}
        {!isLoading && !isError && (
          <EnterpriseTable
            data={items}
            keyExtractor={(e) => e.id}
            columns={[
              { key: 'code', header: 'Code', render: (e) => <span className="font-medium">{e.code}</span> },
              { key: 'designation', header: 'Désignation', render: (e) => e.designation },
              { key: 'famille', header: 'Famille', render: (e) => e.famille || '—' },
              { key: 'zone', header: 'Zone', render: (e) => e.zone || '—' },
              { key: 'failures', header: 'Pannes', render: (e) => e.failureCount },
              {
                key: 'actions',
                header: 'Actions',
                render: (e) => (
                  <div className="flex flex-wrap gap-2">
                    <EnterpriseButton variant="ghost" size="sm" onClick={() => showHistory(e)}>
                      Historique
                    </EnterpriseButton>
                    {hasRole('ADMIN') && (
                      <>
                        <EnterpriseButton
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setEditId(e.id);
                            setForm({
                              code: e.code,
                              designation: e.designation,
                              famille: e.famille ?? '',
                              zone: e.zone ?? '',
                              constructeur: e.constructeur ?? '',
                            });
                            formModal.open();
                          }}
                        >
                          Modifier
                        </EnterpriseButton>
                        <EnterpriseButton variant="ghost" size="sm" onClick={() => handleDelete(e.id)}>
                          Supprimer
                        </EnterpriseButton>
                      </>
                    )}
                  </div>
                ),
              },
            ]}
          />
        )}
      </EnterpriseCard>

      <EnterpriseModal
        open={formModal.isOpen}
        onClose={formModal.close}
        title={editId ? 'Modifier l\'équipement' : 'Nouvel équipement'}
        footer={
          <>
            <EnterpriseButton variant="secondary" onClick={formModal.close}>Annuler</EnterpriseButton>
            <EnterpriseButton type="submit" form="equipment-form" loading={loading}>Enregistrer</EnterpriseButton>
          </>
        }
      >
        <form id="equipment-form" onSubmit={handleSubmit} className="space-y-4">
          {(['code', 'designation', 'famille', 'zone', 'constructeur'] as const).map((field) => (
            <EnterpriseInput
              key={field}
              label={field.charAt(0).toUpperCase() + field.slice(1)}
              value={form[field]}
              onChange={(e) => setForm({ ...form, [field]: e.target.value })}
              required={field === 'code' || field === 'designation'}
            />
          ))}
        </form>
      </EnterpriseModal>

      <EnterpriseModal
        open={historyModal.isOpen}
        onClose={() => {
          historyModal.close();
          setHistoryEquipmentId(null);
        }}
        title={`Historique — ${historyEquipmentCode}`}
        size="lg"
        footer={
          <EnterpriseButton
            variant="secondary"
            onClick={() => {
              historyModal.close();
              setHistoryEquipmentId(null);
            }}
          >
            Fermer
          </EnterpriseButton>
        }
      >
        {historyQuery.isLoading && (
          <p className="text-sm text-slate-500">Chargement de l&apos;historique…</p>
        )}
        {historyQuery.isError && (
          <p className="text-sm text-red-600">Impossible de charger l&apos;historique.</p>
        )}
        {history && (
          <div className="max-h-[65vh] space-y-6 overflow-y-auto">
            <p className="text-sm text-slate-600 dark:text-slate-400">
              {history.failures.length} panne(s) · {history.interventions.length} intervention(s)
            </p>

            <div>
              <EnterpriseSectionTitle title="Pannes" />
              {history.failures.length === 0 ? (
                <p className="text-sm text-slate-500">Aucune panne enregistrée.</p>
              ) : (
                <ul className="mt-3 space-y-2">
                  {history.failures.map((failure: Failure) => (
                    <li key={failure.id} className="rounded-lg border border-slate-200 p-3 dark:border-slate-700">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <Link
                          to={`/failures/${failure.id}`}
                          className="text-sm font-medium text-emerald-600 hover:underline dark:text-emerald-400"
                        >
                          {new Date(failure.dateHeure).toLocaleString('fr-FR')}
                        </Link>
                        <div className="flex gap-2">
                          <EnterpriseBadge label={failure.criticite} variant={criticiteVariant(failure.criticite)} />
                          <EnterpriseBadge label={failure.statut} variant={statutPanneVariant(failure.statut)} />
                        </div>
                      </div>
                      {failure.codeDefaut && (
                        <p className="mt-1 text-xs text-slate-500">Code : {failure.codeDefaut}</p>
                      )}
                      {failure.descriptionInitiale && (
                        <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">{failure.descriptionInitiale}</p>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </div>

            <div>
              <EnterpriseSectionTitle title="Interventions" />
              {history.interventions.length === 0 ? (
                <p className="text-sm text-slate-500">Aucune intervention enregistrée.</p>
              ) : (
                <ul className="mt-3 space-y-2">
                  {history.interventions.map((intervention: Intervention) => (
                    <li key={intervention.id} className="rounded-lg border border-slate-200 p-3 dark:border-slate-700">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="text-sm font-medium text-slate-900 dark:text-slate-100">
                          {intervention.technicienNom}
                        </span>
                        <EnterpriseBadge
                          label={intervention.statutValidation}
                          variant={validationVariant(intervention.statutValidation)}
                        />
                      </div>
                      <p className="text-xs text-slate-500">
                        {new Date(intervention.createdAt).toLocaleString('fr-FR')}
                      </p>
                      {intervention.symptomes && (
                        <p className="mt-1 text-sm text-slate-700 dark:text-slate-300">{intervention.symptomes}</p>
                      )}
                      {intervention.causeRacine && (
                        <p className="text-xs text-slate-500">Cause : {intervention.causeRacine}</p>
                      )}
                      {intervention.failureId && (
                        <Link
                          to={`/failures/${intervention.failureId}`}
                          className="mt-2 inline-block text-xs font-medium text-emerald-600 hover:underline dark:text-emerald-400"
                        >
                          Voir la panne
                        </Link>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}
      </EnterpriseModal>
    </div>
  );
}
