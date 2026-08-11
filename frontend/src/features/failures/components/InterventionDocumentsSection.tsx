import { useRef, type ChangeEvent } from 'react';
import { EnterpriseButton } from '@/design-system';
import type { Document } from '@/shared/types';

function formatFileSize(bytes?: number): string {
  if (!bytes) return '';
  if (bytes < 1024) return `${bytes} o`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
}

export interface InterventionDocumentsSectionProps {
  documents: Document[];
  canManage: boolean;
  loading: boolean;
  onUpload: (file: File) => void;
  onDownload: (documentId: string, filename: string) => void;
  onDelete: (documentId: string) => void;
}

export function InterventionDocumentsSection({
  documents,
  canManage,
  loading,
  onUpload,
  onDownload,
  onDelete,
}: InterventionDocumentsSectionProps) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) onUpload(file);
    event.target.value = '';
  };

  return (
    <section className="mt-4 rounded-lg border border-slate-200 bg-slate-50/60 p-4 dark:border-slate-700 dark:bg-slate-800/30">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <p className="text-xs font-semibold uppercase tracking-wide text-violet-700 dark:text-violet-300">
          Documents et photos
        </p>
        {canManage && (
          <>
            <input
              ref={inputRef}
              type="file"
              accept=".pdf,.png,.jpg,.jpeg,.docx,application/pdf,image/png,image/jpeg,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              className="hidden"
              onChange={handleFileChange}
            />
            <EnterpriseButton
              variant="secondary"
              size="sm"
              disabled={loading}
              onClick={() => inputRef.current?.click()}
            >
              Ajouter un fichier
            </EnterpriseButton>
          </>
        )}
      </div>

      {documents.length === 0 ? (
        <p className="text-sm text-slate-500 dark:text-slate-400">
          {canManage
            ? 'Aucun document — ajoutez photos, rapports PDF ou DOCX (max 10 Mo).'
            : 'Aucun document associé.'}
        </p>
      ) : (
        <ul className="space-y-2">
          {documents.map((doc) => (
            <li
              key={doc.id}
              className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900/50"
            >
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-slate-800 dark:text-slate-100" title={doc.nomFichier}>
                  {doc.nomFichier}
                </p>
                {doc.tailleOctets != null && (
                  <p className="text-xs text-slate-500">{formatFileSize(doc.tailleOctets)}</p>
                )}
              </div>
              <div className="flex shrink-0 gap-2">
                <EnterpriseButton
                  variant="ghost"
                  size="sm"
                  disabled={loading}
                  onClick={() => onDownload(doc.id, doc.nomFichier)}
                >
                  Télécharger
                </EnterpriseButton>
                {canManage && (
                  <EnterpriseButton
                    variant="ghost"
                    size="sm"
                    disabled={loading}
                    onClick={() => onDelete(doc.id)}
                  >
                    Supprimer
                  </EnterpriseButton>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
