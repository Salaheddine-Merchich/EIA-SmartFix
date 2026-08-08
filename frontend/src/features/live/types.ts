export type LiveEventCategory = 'maintenance' | 'alert' | 'knowledge' | 'ai' | 'system';

export type LiveEventType =
  | 'FAILURE_CREATED'
  | 'CRITICAL_ALERT'
  | 'INTERVENTION_CREATED'
  | 'INTERVENTION_SUBMITTED'
  | 'INTERVENTION_VALIDATED'
  | 'RAG_REINDEXED'
  | 'AI_UNAVAILABLE'
  | 'STATUS_UPDATE';

export interface LiveEvent {
  id: string;
  type: LiveEventType;
  category: LiveEventCategory;
  title: string;
  message: string;
  occurredAt: string;
  metadata: Record<string, string>;
}

export type ServiceState = 'ONLINE' | 'DEGRADED' | 'OFFLINE';

export interface ServiceStatus {
  name: string;
  state: ServiceState;
}

export interface LiveStatus {
  backend: ServiceStatus;
  database: ServiceStatus;
  ai: ServiceStatus;
  rag: ServiceStatus;
  liveStream: ServiceStatus;
  checkedAt: string;
}

export interface LiveNotification extends LiveEvent {
  read: boolean;
  dismissed: boolean;
}

export type LiveConnectionState = 'connecting' | 'connected' | 'disconnected' | 'error';
