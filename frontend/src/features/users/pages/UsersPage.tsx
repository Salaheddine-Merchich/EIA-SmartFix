import { type FormEvent, useState } from 'react';
import {
  EnterpriseAvatar,
  EnterpriseBadge,
  EnterpriseButton,
  EnterpriseCard,
  EnterpriseErrorState,
  EnterpriseInput,
  EnterpriseModal,
  EnterprisePageHeader,
  EnterpriseSelect,
  EnterpriseSkeletonTable,
  EnterpriseTable,
  formatRoleLabel,
  roleAvatarClass,
  roleVariant,
  useDisclosure,
  useEnterpriseConfirm,
  useEnterpriseToast,
} from '@/design-system';
import { useMutationFeedback } from '@/shared/hooks/useMutationFeedback';
import type { Role, User } from '@/shared/types';
import { useUsers } from '../hooks/useUsers';

const emptyForm = {
  email: '',
  password: '',
  nomPrenom: '',
  role: 'TECHNICIEN' as Role,
  actif: true,
};

export default function UsersPage() {
  const { confirm } = useEnterpriseConfirm();
  const { toast } = useEnterpriseToast();
  const { loading, execute } = useMutationFeedback();
  const { users, isLoading, isError, refetch, createUser, updateUser, deleteUser } = useUsers();
  const [editId, setEditId] = useState<string | null>(null);
  const formModal = useDisclosure();
  const [form, setForm] = useState(emptyForm);

  const openCreate = () => {
    setEditId(null);
    setForm(emptyForm);
    formModal.open();
  };

  const openEdit = (user: User) => {
    setEditId(user.id);
    setForm({
      email: user.email,
      password: '',
      nomPrenom: user.nomPrenom,
      role: user.role,
      actif: user.actif,
    });
    formModal.open();
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const payload = {
      email: form.email.trim(),
      nomPrenom: form.nomPrenom.trim(),
      role: form.role,
      actif: form.actif,
      password: form.password,
    };
    if (!payload.nomPrenom) {
      toast('Le nom prénom est obligatoire.', 'error');
      return;
    }
    if (!editId && payload.password.length < 8) {
      toast('Le mot de passe doit contenir au moins 8 caractères.', 'error');
      return;
    }
    if (editId && payload.password && payload.password.length < 8) {
      toast('Le mot de passe doit contenir au moins 8 caractères.', 'error');
      return;
    }

    const result = await execute(
      async () => {
        if (editId) {
          return updateUser.mutateAsync({
            id: editId,
            data: {
              email: payload.email,
              nomPrenom: payload.nomPrenom,
              role: payload.role,
              actif: payload.actif,
              password: payload.password || undefined,
            },
          });
        }
        return createUser.mutateAsync(payload);
      },
      {
        successMessage: editId ? 'Utilisateur mis à jour' : 'Utilisateur créé',
        errorMessage: editId
          ? 'Impossible de mettre à jour l\'utilisateur.'
          : 'Impossible de créer l\'utilisateur.',
      },
    );

    if (!result) return;
    formModal.close();
    setEditId(null);
    setForm(emptyForm);
  };

  const handleDelete = async (id: string) => {
    const ok = await confirm({
      title: 'Supprimer l\'utilisateur',
      message: 'Cette action est définitive.',
      confirmLabel: 'Supprimer',
      variant: 'danger',
    });
    if (!ok) return;
    await execute(() => deleteUser.mutateAsync(id).then(() => true), {
      successMessage: 'Utilisateur supprimé',
      errorMessage: 'Impossible de supprimer l\'utilisateur.',
    });
  };

  return (
    <div className="space-y-6">
      <EnterprisePageHeader
        title="Utilisateurs"
        description="Administration des comptes et rôles"
        actions={<EnterpriseButton onClick={openCreate}>Ajouter</EnterpriseButton>}
      />

      {!isLoading && !isError && users.length > 0 && (
        <p className="text-sm text-slate-500 dark:text-slate-400">
          {users.length} utilisateur{users.length > 1 ? 's' : ''} enregistré{users.length > 1 ? 's' : ''}
        </p>
      )}

      <EnterpriseCard padding="none">
        {isLoading && <EnterpriseSkeletonTable rows={5} />}
        {isError && (
          <EnterpriseErrorState
            title="Erreur de chargement"
            message="Impossible de charger la liste des utilisateurs."
            onRetry={() => void refetch()}
          />
        )}
        {!isLoading && !isError && (
          <EnterpriseTable
            data={users}
            keyExtractor={(u) => u.id}
            emptyMessage="Aucun utilisateur enregistré"
            columns={[
              {
                key: 'name',
                header: 'Nom',
                width: '14rem',
                render: (u) => (
                  <div className="flex min-w-0 items-center gap-3">
                    <EnterpriseAvatar
                      name={u.nomPrenom}
                      size="sm"
                      className={roleAvatarClass(u.role)}
                    />
                    <span className="truncate font-medium text-slate-900 dark:text-slate-100" title={u.nomPrenom}>
                      {u.nomPrenom}
                    </span>
                  </div>
                ),
              },
              {
                key: 'email',
                header: 'Email',
                render: (u) => (
                  <span className="block truncate text-slate-600 dark:text-slate-400" title={u.email}>
                    {u.email}
                  </span>
                ),
              },
              {
                key: 'role',
                header: 'Rôle',
                width: '10rem',
                render: (u) => (
                  <EnterpriseBadge label={formatRoleLabel(u.role)} variant={roleVariant(u.role)} />
                ),
              },
              {
                key: 'active',
                header: 'Actif',
                width: '6rem',
                align: 'center',
                render: (u) => (
                  <EnterpriseBadge
                    label={u.actif ? 'Actif' : 'Inactif'}
                    variant={u.actif ? 'success' : 'default'}
                  />
                ),
              },
              {
                key: 'actions',
                header: 'Actions',
                width: '200px',
                align: 'left',
                nowrap: true,
                render: (u) => (
                  <div className="flex items-center gap-1.5 whitespace-nowrap">
                    <EnterpriseButton variant="secondary" size="sm" onClick={() => openEdit(u)}>
                      Modifier
                    </EnterpriseButton>
                    <EnterpriseButton
                      variant="ghost"
                      size="sm"
                      className="text-red-600 hover:bg-red-50 hover:text-red-700 dark:text-red-400 dark:hover:bg-red-950/40 dark:hover:text-red-300"
                      onClick={() => handleDelete(u.id)}
                    >
                      Supprimer
                    </EnterpriseButton>
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
        title={editId ? 'Modifier l\'utilisateur' : 'Nouvel utilisateur'}
        footer={
          <>
            <EnterpriseButton variant="secondary" onClick={formModal.close}>Annuler</EnterpriseButton>
            <EnterpriseButton type="submit" form="user-form" loading={loading}>
              {editId ? 'Enregistrer' : 'Créer'}
            </EnterpriseButton>
          </>
        }
      >
        <form id="user-form" onSubmit={handleSubmit} className="space-y-4">
          <EnterpriseInput
            label="Nom prénom"
            value={form.nomPrenom}
            onChange={(e) => setForm({ ...form, nomPrenom: e.target.value })}
            required
          />
          <EnterpriseInput
            label="Email"
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
          <EnterpriseInput
            label="Mot de passe"
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required={!editId}
            minLength={editId ? undefined : 8}
            placeholder={editId ? 'Laisser vide pour ne pas changer' : 'Minimum 8 caractères (ex. Password123!)'}
          />
          <EnterpriseSelect
            label="Rôle"
            value={form.role}
            onChange={(e) => setForm({ ...form, role: e.target.value as Role })}
          >
            {(['ADMIN', 'RESPONSABLE_EIA', 'TECHNICIEN'] as const).map((r) => (
              <option key={r} value={r}>{r.replace('_', ' ')}</option>
            ))}
          </EnterpriseSelect>
          <EnterpriseSelect
            label="Actif"
            value={form.actif ? 'true' : 'false'}
            onChange={(e) => setForm({ ...form, actif: e.target.value === 'true' })}
          >
            <option value="true">Oui</option>
            <option value="false">Non</option>
          </EnterpriseSelect>
        </form>
      </EnterpriseModal>
    </div>
  );
}
