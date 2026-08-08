import { useEffect, useState } from 'react';

import { Link } from 'react-router-dom';

import {

  EnterpriseBadge,

  EnterpriseButton,

  EnterpriseCard,

  EnterpriseEmptyState,

  EnterpriseErrorState,

  EnterpriseInput,

  EnterprisePageHeader,

  EnterpriseSelect,

  validationVariant,

} from '@/design-system';

import { equipmentApi, searchApi } from '@/shared/api';

import type { Equipment, Intervention } from '@/shared/types';



export default function SearchPage() {

  const [q, setQ] = useState('');

  const [symptom, setSymptom] = useState('');

  const [faultCode, setFaultCode] = useState('');

  const [equipmentId, setEquipmentId] = useState('');

  const [equipment, setEquipment] = useState<Equipment[]>([]);

  const [results, setResults] = useState<Intervention[]>([]);
  const [totalResults, setTotalResults] = useState(0);

  const [searched, setSearched] = useState(false);

  const [loading, setLoading] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);



  useEffect(() => {

    equipmentApi

      .list({ page: 0, size: 100 })

      .then((r) => setEquipment(r.content))

      .catch(() => setEquipment([]));

  }, []);



  const runSearch = async () => {
    setLoading(true);
    setSearchError(null);

    try {
      const params: Record<string, string | number> = { q, symptom, page: 0, size: 50 };
      if (faultCode) params.faultCode = faultCode;
      if (equipmentId) params.equipmentId = equipmentId;

      const data = await searchApi.search(params);
      setResults(data.interventions);
      setTotalResults(data.totalElements);
      setSearched(true);
    } catch {
      setResults([]);
      setTotalResults(0);
      setSearched(true);
      setSearchError('La recherche a échoué. Vérifiez votre connexion et réessayez.');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    await runSearch();
  };



  return (

    <div className="space-y-6">

      <EnterprisePageHeader

        title="Knowledge"

        description="Recherche dans la base de connaissances maintenance"

      />



      <EnterpriseCard>

        <form onSubmit={handleSearch} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">

          <EnterpriseInput

            placeholder="Mot-clé…"

            value={q}

            onChange={(e) => setQ(e.target.value)}

          />

          <EnterpriseInput

            placeholder="Symptôme…"

            value={symptom}

            onChange={(e) => setSymptom(e.target.value)}

          />

          <EnterpriseInput

            placeholder="Code défaut…"

            value={faultCode}

            onChange={(e) => setFaultCode(e.target.value)}

          />

          <EnterpriseSelect

            label="Équipement"

            value={equipmentId}

            onChange={(e) => setEquipmentId(e.target.value)}

          >

            <option value="">Tous les équipements</option>

            {equipment.map((e) => (

              <option key={e.id} value={e.id}>{e.code} — {e.designation}</option>

            ))}

          </EnterpriseSelect>

          <div className="flex items-end sm:col-span-2 lg:col-span-3">

            <EnterpriseButton type="submit" loading={loading}>Rechercher</EnterpriseButton>

          </div>

        </form>

      </EnterpriseCard>



      {searched && (

        <div className="space-y-3">

          {searchError ? (
            <EnterpriseErrorState
              title="Erreur de recherche"
              message={searchError}
              onRetry={() => void runSearch()}
            />
          ) : (
          <>
          <p className="text-sm text-slate-500 dark:text-slate-400">{totalResults} résultat(s)</p>

          {results.length === 0 ? (

            <EnterpriseEmptyState

              title="Aucun résultat"

              description="Affinez vos critères de recherche ou consultez l'assistant IA."

            />

          ) : (

            results.map((item) => (

              <EnterpriseCard key={item.id} hover>

                <div className="flex items-start justify-between gap-3">

                  <span className="font-semibold text-slate-900 dark:text-slate-100">{item.equipmentCode}</span>

                  <EnterpriseBadge label={item.statutValidation} variant={validationVariant(item.statutValidation)} />

                </div>

                {item.symptomes && <p className="mt-2 text-sm text-slate-700 dark:text-slate-300">{item.symptomes}</p>}

                {item.causeRacine && (

                  <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Cause: {item.causeRacine}</p>

                )}

                {item.failureId && (

                  <Link

                    to={`/failures/${item.failureId}`}

                    className="mt-2 inline-block text-sm font-medium text-emerald-600 hover:underline dark:text-emerald-400"

                  >

                    Voir la panne associée

                  </Link>

                )}

              </EnterpriseCard>

            ))

          )}

          </>
          )}
        </div>

      )}

    </div>

  );

}

