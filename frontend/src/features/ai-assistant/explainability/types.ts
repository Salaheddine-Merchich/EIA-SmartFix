export interface AiDiagnosticTrace {
  query: string;
  retrievedDocuments: RetrievedDocument[];
  retrievalSteps: RetrievalStep[];
  vectorResultCount: number;
  textResultCount: number;
  mergedResultCount: number;
  filteredCount: number;
  averageSimilarity: number;
  confidenceScore: number;
  confidenceLevel: 'VERY_HIGH' | 'HIGH' | 'LOW';
  retrievalDurationMs: number;
  llmDurationMs: number;
}

export interface RetrievedDocument {
  interventionId: string;
  equipmentCode: string;
  title: string;
  detail: string;
  similarityPercent: number;
}

export interface RetrievalStep {
  step: string;
  status: string;
  detail: string;
}

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
  diagnosticTrace?: AiDiagnosticTrace | null;
}

export interface AiReliabilityStats {
  diagnosticsCount: number;
  averageConfidence: number;
  totalRetrievals: number;
}
