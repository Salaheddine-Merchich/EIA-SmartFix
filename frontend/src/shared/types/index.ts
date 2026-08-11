export type Role = 'ADMIN' | 'RESPONSABLE_EIA' | 'TECHNICIEN';

export type StatutPanne = 'OUVERTE' | 'EN_COURS' | 'RESOLUE' | 'CLOTUREE';
export type Criticite = 'FAIBLE' | 'MOYENNE' | 'HAUTE' | 'CRITIQUE';
export type StatutValidation = 'BROUILLON' | 'SOUMISE' | 'VALIDEE' | 'REJETEE';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  role: Role;
  nomPrenom: string;
  email: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface User {
  id: string;
  email: string;
  role: Role;
  nomPrenom: string;
  actif: boolean;
}

export interface Equipment {
  id: string;
  code: string;
  designation: string;
  famille?: string;
  zone?: string;
  constructeur?: string;
  miseEnService?: string;
  failureCount: number;
}

export interface Failure {
  id: string;
  equipmentId: string;
  equipmentCode: string;
  equipmentDesignation: string;
  dateHeure: string;
  criticite: Criticite;
  zoneService?: string;
  responsableId?: string;
  responsableNom?: string;
  declarantId?: string;
  declarantNom?: string;
  statut: StatutPanne;
  descriptionInitiale?: string;
  codeDefaut?: string;
  interventionCount: number;
  latestInterventionStatut?: StatutValidation;
}

export interface Intervention {
  id: string;
  failureId: string;
  equipmentCode: string;
  technicienId: string;
  technicienNom: string;
  description?: string;
  symptomes?: string;
  causeRacine?: string;
  analyseTechnique?: string;
  actionsCorrectives?: string;
  piecesRemplacees?: string;
  dureeArretMinutes?: number;
  tempsInterventionMinutes?: number;
  statutValidation: StatutValidation;
  validateurId?: string;
  validateurNom?: string;
  dateValidation?: string;
  commentaireValidation?: string;
  createdAt: string;
  documents: Document[];
}

export interface Document {
  id: string;
  nomFichier: string;
  typeMime?: string;
  tailleOctets?: number;
}

export interface DashboardStats {
  totalFailures: number;
  openFailures: number;
  validatedInterventions: number;
  pendingValidations: number;
  draftInterventions: number;
  rejectedInterventions: number;
  knowledgeDocuments: number;
  activeKnowledgeDocuments: number;
  indexedInterventions: number;
  mttrMinutes?: number;
  mtbfHours?: number;
  topFailingEquipment: { equipmentId: string; code: string; designation: string; failureCount: number }[];
  topCauses: { cause: string; count: number }[];
  failuresByFamille: { famille: string; count: number }[];
  failuresByMonth: { month: string; count: number }[];
  aiReliability?: {
    diagnosticsCount: number;
    averageConfidence: number;
    totalRetrievals: number;
  } | null;
}

export interface EquipmentHistory {
  failures: Failure[];
  interventions: Intervention[];
}

export interface RecurringDefectItem {
  codeDefaut: string;
  occurrenceCount: number;
  affectedEquipmentCount: number;
  lastSeenMonth: string;
}

export interface RecurringDefectsAnalysis {
  defects: RecurringDefectItem[];
  analysis: string;
  recommendations: string;
  disclaimer: string;
}

export type {
  AiDiagnosticTrace,
  RetrievedDocument,
  RetrievalStep,
} from '@/features/ai-assistant/explainability/types';

export interface AiAssistResponse {
  similarInterventions: {
    interventionId: string;
    equipmentCode: string;
    symptomes?: string;
    causeRacine?: string;
    actionsCorrectives?: string;
    similarity: number;
  }[];
  suggestions: {
    probableCauses: string[];
    correctiveActions: string[];
    summary: string;
    advice: string;
  };
  disclaimer: string;
  diagnosticTrace?: import('@/features/ai-assistant/explainability/types').AiDiagnosticTrace | null;
}
