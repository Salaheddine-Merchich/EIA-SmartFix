import { motion } from 'framer-motion';
import type { KpiCardData } from '../types';
import { staggerContainer } from '@/features/ai-assistant/animations';
import { KpiCard } from './KpiCard';

interface KpiGridProps {
  cards: KpiCardData[];
}

export function KpiGrid({ cards }: KpiGridProps) {
  return (
    <motion.section
      aria-label="Indicateurs clés"
      className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4"
      variants={staggerContainer}
      initial="initial"
      animate="animate"
    >
      {cards.map((card) => (
        <KpiCard key={card.id} card={card} />
      ))}
    </motion.section>
  );
}
