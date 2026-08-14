import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AssistantMessageBubble } from './MessageBubble';
import type { AssistantMessage } from '../types';

describe('AssistantMessageBubble', () => {
  it('renders summary, causes, actions and disclaimer', () => {
    const message: AssistantMessage = {
      id: 'a1',
      role: 'assistant',
      createdAt: new Date().toISOString(),
      response: {
        disclaimer: 'Assistance uniquement',
        similarInterventions: [],
        suggestions: {
          summary: 'Piste ventilation',
          probableCauses: ['Filtre obstrué'],
          correctiveActions: ['Nettoyer le filtre'],
          advice: "Couper l'alimentation",
        },
      },
    };

    render(<AssistantMessageBubble message={message} />);

    expect(screen.getByText('Résumé')).toBeInTheDocument();
    expect(screen.getByText('Piste ventilation')).toBeInTheDocument();
    expect(screen.getByText('Filtre obstrué')).toBeInTheDocument();
    expect(screen.getByText('Nettoyer le filtre')).toBeInTheDocument();
    expect(screen.getByText('Assistance uniquement')).toBeInTheDocument();
  });

  it('shows schema button when relevantSchemas are present', () => {
    const message: AssistantMessage = {
      id: 'a3',
      role: 'assistant',
      createdAt: new Date().toISOString(),
      response: {
        disclaimer: 'Assistance uniquement',
        similarInterventions: [],
        relevantSchemas: [
          {
            schemaId: 's1',
            equipmentId: 'e1',
            equipmentCode: 'VAR-VEI-SI23',
            label: 'Cablage X1',
            schemaType: 'wiring',
            downloadUrl: '/api/v1/equipment/e1/schemas/s1/download',
          },
        ],
        suggestions: {
          summary: 'Pompe PV',
          probableCauses: ['Veille variateur'],
          correctiveActions: ['Verifier X1'],
          advice: 'Consulter manuel',
        },
      },
    };

    render(<AssistantMessageBubble message={message} />);

    expect(screen.getByRole('button', { name: /Schémas/i })).toBeInTheDocument();
    expect(screen.getByText('Schémas')).toBeInTheDocument();
  });

  it('hides schema button when relevantSchemas is empty', () => {
    const message: AssistantMessage = {
      id: 'a4',
      role: 'assistant',
      createdAt: new Date().toISOString(),
      response: {
        disclaimer: 'Assistance uniquement',
        similarInterventions: [],
        suggestions: {
          summary: 'Pompe PV',
          probableCauses: ['Veille variateur'],
          correctiveActions: ['Verifier X1'],
          advice: 'Consulter manuel',
        },
      },
    };

    render(<AssistantMessageBubble message={message} />);

    expect(screen.queryByRole('button', { name: /Schémas/i })).not.toBeInTheDocument();
  });

  it('renders hard error without response', () => {
    const message: AssistantMessage = {
      id: 'a2',
      role: 'assistant',
      createdAt: new Date().toISOString(),
      response: null,
      error: { kind: 'backend', message: 'Le serveur est indisponible.' },
    };

    render(<AssistantMessageBubble message={message} />);
    expect(screen.getByRole('status')).toHaveTextContent('Le serveur est indisponible.');
  });
});
