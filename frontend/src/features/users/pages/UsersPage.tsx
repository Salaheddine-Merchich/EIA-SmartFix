import { type FormEvent, useEffect, useState } from 'react';

import {

  EnterpriseButton,

  EnterpriseCard,

  EnterpriseErrorState,

  EnterpriseInput,

  EnterpriseModal,

  EnterprisePageHeader,

  EnterpriseSelect,

  EnterpriseSkeletonTable,

  EnterpriseTable,

  useDisclosure,

  useEnterpriseConfirm,

  useEnterpriseToast,

} from '@/design-system';

import { usersApi } from '@/shared/api';

import { useMutationFeedback } from '@/shared/hooks/useMutationFeedback';

import type { Role, User } from '@/shared/types';



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

  const [users, setUsers] = useState<User[]>([]);

  const [listStatus, setListStatus] = useState<'loading' | 'ready' | 'error'>('loading');

  const [editId, setEditId] = useState<string | null>(null);

  const formModal = useDisclosure();

  const [form, setForm] = useState(emptyForm);



  const load = async () => {

    setListStatus('loading');

    try {

      const data = await usersApi.list();

      setUsers(data);

      setListStatus('ready');

    } catch {

      setUsers([]);

      setListStatus('error');

    }

  };

  useEffect(() => { void load(); }, []);



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

    const result = await execute(async () => {

      if (editId) {

        return usersApi.update(editId, {

          email: payload.email,

          nomPrenom: payload.nomPrenom,

          role: payload.role,

          actif: payload.actif,

          password: payload.password || undefined,

        });

      }

      return usersApi.create(payload);

    }, {

      successMessage: editId ? 'Utilisateur mis à jour' : 'Utilisateur créé',

      errorMessage: editId

        ? 'Impossible de mettre à jour l\'utilisateur.'

        : 'Impossible de créer l\'utilisateur.',

    });

    if (!result) return;

    formModal.close();

    setEditId(null);

    setForm(emptyForm);

    load();

  };



  const handleDelete = async (id: string) => {

    const ok = await confirm({

      title: 'Supprimer l\'utilisateur',

      message: 'Cette action est définitive.',

      confirmLabel: 'Supprimer',

      variant: 'danger',

    });

    if (!ok) return;

    const result = await execute(() => usersApi.delete(id).then(() => true), {

      successMessage: 'Utilisateur supprimé',

      errorMessage: 'Impossible de supprimer l\'utilisateur.',

    });

    if (result) load();

  };



  return (

    <div className="space-y-6">

      <EnterprisePageHeader

        title="Utilisateurs"

        description="Administration des comptes et rôles"

        actions={<EnterpriseButton onClick={openCreate}>Ajouter</EnterpriseButton>}

      />



      <EnterpriseCard padding="none">

        {listStatus === 'loading' && <EnterpriseSkeletonTable rows={5} />}

        {listStatus === 'error' && (

          <EnterpriseErrorState

            title="Erreur de chargement"

            message="Impossible de charger la liste des utilisateurs."

            onRetry={() => void load()}

          />

        )}

        {listStatus === 'ready' && (

        <EnterpriseTable

          data={users}

          keyExtractor={(u) => u.id}

          columns={[

            { key: 'name', header: 'Nom', render: (u) => u.nomPrenom },

            { key: 'email', header: 'Email', render: (u) => u.email },

            { key: 'role', header: 'Rôle', render: (u) => u.role.replace('_', ' ') },

            { key: 'active', header: 'Actif', render: (u) => (u.actif ? 'Oui' : 'Non') },

            {

              key: 'actions',

              header: 'Actions',

              render: (u) => (

                <div className="flex gap-1">

                  <EnterpriseButton variant="ghost" size="sm" onClick={() => openEdit(u)}>

                    Modifier

                  </EnterpriseButton>

                  <EnterpriseButton variant="ghost" size="sm" onClick={() => handleDelete(u.id)}>

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


