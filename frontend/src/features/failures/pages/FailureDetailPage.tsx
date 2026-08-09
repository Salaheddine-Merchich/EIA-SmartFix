import { type FormEvent, useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  EnterpriseBadge,
  EnterpriseButton,
  EnterpriseCard,
  EnterpriseErrorState,
  EnterpriseInput,
  EnterpriseModal,
  EnterprisePageHeader,
  EnterprisePageLoader,
  EnterpriseSelect,
  EnterpriseStat,
  EnterpriseTextarea,
  criticiteVariant,
  statutPanneVariant,
  useDisclosure,
  useEnterpriseConfirm,
} from '@/design-system';
import { useMutationFeedback } from '@/shared/hooks/useMutationFeedback';
import { interventionsApi } from '@/shared/api';
import { useAuth } from '@/features/auth/context/AuthContext';
import type { StatutPanne } from '@/shared/types';
import { FailureSummaryPanel } from '../components/FailureSummaryPanel';
import { InterventionTimeline } from '../components/InterventionTimeline';
import { ValidationModal } from '../components/ValidationModal';
import { useFailureDetail } from '../hooks/useFailureDetail';

const INTERVENTION_FIELD_LABELS: Record<string, string> = {
  description: 'Description détaillée',
  symptomes: 'Symptômes',
  causeRacine: 'Cause racine',
  analyseTechnique: 'Analyse technique',
  actionsCorrectives: 'Actions correctives',
  piecesRemplacees: 'Pièces remplacées',
};

const STATUT_OPTIONS: StatutPanne[] = ['OUVERTE', 'EN_COURS', 'RESOLUE', 'CLOTUREE'];

function formatValidationStatut(statut?: string) {
  switch (statut) {
    case 'VALIDEE':
      return 'Validée';
    case 'SOUMISE':
      return 'Soumise';
    case 'REJETEE':
      return 'Rejetée';
    case 'BROUILLON':
      return 'Brouillon';
    default:
      return 'Aucune';
  }
}

export default function FailureDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { hasRole } = useAuth();
  const { confirm } = useEnterpriseConfirm();
  const { loading, execute } = useMutationFeedback();
  const formModal = useDisclosure();
  const statutModal = useDisclosure();
  const validationModal = useDisclosure();

  const {
    failure,
    interventions,
    isLoading,
    isError,
    isNotFound,
    loadError,
    refetch,
    updateFailure,
    deleteFailure,
    createIntervention,
    submitIntervention,
    validateIntervention,
  } = useFailureDetail(id);

  const [statutEdit, setStatutEdit] = useState<StatutPanne>('OUVERTE');
  const [validationTarget, setValidationTarget] = useState<{ id: string; approved: boolean } | null>(null);
  const [form, setForm] = useState({
    symptomes: '',
    causeRacine: '',
    analyseTechnique: '',
    actionsCorrectives: '',
    piecesRemplacees: '',
    dureeArretMinutes: '',
    tempsInterventionMinutes: '',
    description: '',
  });

  useEffect(() => {
    if (!failure || !id || searchParams.get('newIntervention') !== '1') return;
    formModal.open();
    navigate(`/failures/${id}`, { replace: true });
  }, [failure, id, searchParams, navigate, formModal.open]);

  const openStatutModal = () => {
    if (failure) setStatutEdit(failure.statut);
    statutModal.open();
  };

  const handleUpdateStatut = async (event: FormEvent) => {
    event.preventDefault();
    if (!failure) return;
    const result = await execute(
      () =>
        updateFailure.mutateAsync({
          equipmentId: failure.equipmentId,
          dateHeure: failure.dateHeure,
          criticite: failure.criticite,
          zoneService: failure.zoneService,
          responsableId: failure.responsableId,
          statut: statutEdit,
          descriptionInitiale: failure.descriptionInitiale,
          codeDefaut: failure.codeDefaut,
        }),
      {
        successMessage: 'Statut mis à jour',
        errorMessage: 'Impossible de mettre à jour le statut.',
      },
    );
    if (!result) return;
    statutModal.close();
  };

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault();
    const result = await execute(
      () =>
        createIntervention.mutateAsync({
          failureId: id,
          ...form,
          dureeArretMinutes: form.dureeArretMinutes ? Number(form.dureeArretMinutes) : undefined,
          tempsInterventionMinutes: form.tempsInterventionMinutes
            ? Number(form.tempsInterventionMinutes)
            : undefined,
        }),
      {
        successMessage: 'Intervention enregistrée',
        errorMessage: 'Impossible d\'enregistrer l\'intervention.',
      },
    );
    if (!result) return;
    formModal.close();
  };

  const openValidationModal = (interventionId: string, approved: boolean) => {
    setValidationTarget({ id: interventionId, approved });
    validationModal.open();
  };

  const handleConfirmValidation = async (commentaire: string) => {
    if (!validationTarget) return;
    const result = await execute(
      () =>
        validateIntervention.mutateAsync({
          interventionId: validationTarget.id,
          approved: validationTarget.approved,
          commentaire,
        }),
      {
        successMessage: validationTarget.approved ? 'Intervention validée' : 'Intervention rejetée',
        errorMessage: 'Impossible de valider l\'intervention.',
      },
    );
    if (!result) return;
    validationModal.close();
    setValidationTarget(null);
  };

  const handleSubmitIntervention = async (interventionId: string) => {
    await execute(() => submitIntervention.mutateAsync(interventionId), {
      successMessage: 'Intervention soumise',
      errorMessage: 'Impossible de soumettre l\'intervention.',
    });
  };

  const handleExportPdf = async (interventionId: string) => {
    await execute(() => interventionsApi.exportPdf(interventionId), {
      successMessage: 'PDF exporté avec succès',
      errorMessage: 'Erreur lors de l\'export PDF',
    });
  };

  if (isLoading) {
    return <EnterprisePageLoader message="Chargement de la panne…" />;
  }

  if (isNotFound) {
    return (
      <EnterpriseErrorState
        title="Panne introuvable"
        message={loadError ?? 'Cette panne n\'existe pas.'}
        onRetry={() => navigate('/failures')}
      />
    );
  }

  if (isError || !failure) {
    return (
      <EnterpriseErrorState
        title="Erreur de chargement"
        message={loadError ?? 'Impossible de charger la panne.'}
        onRetry={() => void refetch()}
      />
    );
  }

  const handleDeleteFailure = async () => {
    const ok = await confirm({
      title: 'Supprimer la panne',
      message: `Supprimer définitivement la panne sur ${failure.equipmentCode} et toutes ses interventions ?`,
      confirmLabel: 'Supprimer',
      variant: 'danger',
    });
    if (!ok) return;
    const result = await execute(() => deleteFailure.mutateAsync(), {
      successMessage: 'Panne supprimée',
      errorMessage: 'Impossible de supprimer la panne.',
    });
    if (!result) return;
    navigate('/failures');
  };

  const canEditStatut = hasRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN');
  const canDeleteFailure = hasRole('RESPONSABLE_EIA', 'ADMIN');
  const canValidate = hasRole('RESPONSABLE_EIA', 'ADMIN');
  const interventionCount = failure.interventionCount ?? interventions.length;

  return (
    <div className="min-h-full bg-gradient-to-br from-slate-50/80 via-white to-slate-100/40 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950">
      <div className="space-y-6 px-4 py-6 sm:px-6">
        <EnterprisePageHeader
          breadcrumb={
            <nav className="text-sm text-slate-500 dark:text-slate-400">
              <Link to="/failures" className="hover:text-emerald-700 dark:hover:text-emerald-300">
                Pannes
              </Link>
              <span className="mx-2">/</span>
              <span className="text-slate-700 dark:text-slate-200">{failure.equipmentCode}</span>
            </nav>
          }
          title={`Panne — ${failure.equipmentCode}`}
          description={failure.equipmentDesignation}
          actions={
            <div className="flex flex-wrap gap-2">
              {canEditStatut && (
                <EnterpriseButton variant="secondary" onClick={openStatutModal}>
                  Modifier le statut
                </EnterpriseButton>
              )}
              <EnterpriseButton onClick={formModal.open}>Nouvelle intervention</EnterpriseButton>
              {canDeleteFailure && (
                <EnterpriseButton variant="danger" onClick={() => void handleDeleteFailure()}>
                  Supprimer la panne
                </EnterpriseButton>
              )}
            </div>
          }
        />

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <EnterpriseCard hover>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Statut panne
            </p>
            <div className="mt-3">
              <EnterpriseBadge label={failure.statut} variant={statutPanneVariant(failure.statut)} />
            </div>
          </EnterpriseCard>
          <EnterpriseCard hover>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Criticité
            </p>
            <div className="mt-3">
              <EnterpriseBadge label={failure.criticite} variant={criticiteVariant(failure.criticite)} />
            </div>
          </EnterpriseCard>
          <EnterpriseStat
            label="Interventions"
            value={interventionCount}
            hint="Nombre total d'interventions"
          />
          <EnterpriseStat
            label="Dernière validation"
            value={formatValidationStatut(failure.latestInterventionStatut)}
            hint="Statut de la dernière intervention"
          />
        </div>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
          <FailureSummaryPanel failure={failure} />
          <InterventionTimeline
            interventions={interventions}
            loading={loading}
            canValidate={canValidate}
            onSubmit={(interventionId) => void handleSubmitIntervention(interventionId)}
            onValidate={openValidationModal}
            onExportPdf={(interventionId) => void handleExportPdf(interventionId)}
            onCreateIntervention={formModal.open}
          />
        </div>
      </div>

      <ValidationModal
        open={validationModal.isOpen}
        approved={validationTarget?.approved ?? true}
        loading={loading}
        onClose={() => {
          validationModal.close();
          setValidationTarget(null);
        }}
        onConfirm={(commentaire) => void handleConfirmValidation(commentaire)}
      />

      <EnterpriseModal
        open={statutModal.isOpen}
        onClose={statutModal.close}
        title="Modifier le statut de la panne"
        footer={
          <>
            <EnterpriseButton variant="secondary" onClick={statutModal.close}>Annuler</EnterpriseButton>
            <EnterpriseButton type="submit" form="statut-form" loading={loading}>Enregistrer</EnterpriseButton>
          </>
        }
      >
        <form id="statut-form" onSubmit={handleUpdateStatut} className="space-y-4">
          <EnterpriseSelect
            label="Statut"
            value={statutEdit}
            onChange={(event) => setStatutEdit(event.target.value as StatutPanne)}
            required
          >
            {STATUT_OPTIONS.map((statut) => (
              <option key={statut} value={statut}>{statut}</option>
            ))}
          </EnterpriseSelect>
        </form>
      </EnterpriseModal>

      <EnterpriseModal
        open={formModal.isOpen}
        onClose={formModal.close}
        title="Nouvelle intervention"
        size="lg"
        footer={
          <>
            <EnterpriseButton variant="secondary" onClick={formModal.close}>Annuler</EnterpriseButton>
            <EnterpriseButton type="submit" form="intervention-form" loading={loading}>Enregistrer</EnterpriseButton>
          </>
        }
      >
        <form id="intervention-form" onSubmit={handleCreate} className="max-h-[60vh] space-y-3 overflow-y-auto">
          {(['description', 'symptomes', 'causeRacine', 'analyseTechnique', 'actionsCorrectives', 'piecesRemplacees'] as const).map((field) => (
            <EnterpriseTextarea
              key={field}
              label={INTERVENTION_FIELD_LABELS[field]}
              value={form[field]}
              onChange={(event) => setForm({ ...form, [field]: event.target.value })}
              rows={2}
            />
          ))}
          <div className="grid grid-cols-2 gap-3">
            <EnterpriseInput
              label="Durée arrêt (min)"
              type="number"
              value={form.dureeArretMinutes}
              onChange={(event) => setForm({ ...form, dureeArretMinutes: event.target.value })}
            />
            <EnterpriseInput
              label="Temps intervention (min)"
              type="number"
              value={form.tempsInterventionMinutes}
              onChange={(event) => setForm({ ...form, tempsInterventionMinutes: event.target.value })}
            />
          </div>
        </form>
      </EnterpriseModal>
    </div>
  );
}
