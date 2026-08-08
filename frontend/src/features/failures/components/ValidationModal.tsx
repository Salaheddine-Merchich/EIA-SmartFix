import { type FormEvent, useEffect, useState } from 'react';
import { EnterpriseButton, EnterpriseModal, EnterpriseTextarea } from '@/design-system';

export interface ValidationModalProps {
  open: boolean;
  approved: boolean;
  loading?: boolean;
  onClose: () => void;
  onConfirm: (commentaire: string) => void;
}

export function ValidationModal({
  open,
  approved,
  loading = false,
  onClose,
  onConfirm,
}: ValidationModalProps) {
  const [commentaire, setCommentaire] = useState('');

  useEffect(() => {
    if (open) {
      setCommentaire('');
    }
  }, [open, approved]);

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    onConfirm(commentaire);
  };

  return (
    <EnterpriseModal
      open={open}
      onClose={onClose}
      title={approved ? 'Valider l\'intervention' : 'Rejeter l\'intervention'}
      footer={
        <>
          <EnterpriseButton variant="secondary" onClick={onClose} disabled={loading}>
            Annuler
          </EnterpriseButton>
          <EnterpriseButton
            type="submit"
            form="validation-form"
            variant={approved ? 'primary' : 'danger'}
            loading={loading}
          >
            {approved ? 'Valider' : 'Rejeter'}
          </EnterpriseButton>
        </>
      }
    >
      <form id="validation-form" onSubmit={handleSubmit} className="space-y-4">
        <p className="text-sm text-slate-600 dark:text-slate-300">
          {approved
            ? 'Ajoutez un commentaire de validation si nécessaire.'
            : 'Indiquez le motif du rejet pour informer le technicien.'}
        </p>
        <EnterpriseTextarea
          label={approved ? 'Commentaire (optionnel)' : 'Motif du rejet'}
          value={commentaire}
          onChange={(event) => setCommentaire(event.target.value)}
          rows={4}
          required={!approved}
        />
      </form>
    </EnterpriseModal>
  );
}
