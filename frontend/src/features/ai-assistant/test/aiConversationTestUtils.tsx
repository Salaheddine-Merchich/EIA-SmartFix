import type { ReactNode } from 'react';
import { AiConversationProvider } from '../context/AiConversationProvider';

export function createAiConversationWrapper() {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <AiConversationProvider>{children}</AiConversationProvider>;
  };
}
