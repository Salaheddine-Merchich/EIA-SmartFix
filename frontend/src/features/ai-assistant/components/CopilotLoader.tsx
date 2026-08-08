import { motion } from 'framer-motion';
import { pulseVariants } from '../animations';

interface CopilotLoaderProps {
  message?: string;
}

export function CopilotLoader({ message = "L'assistant analyse votre demande" }: CopilotLoaderProps) {
  return (
    <div
      className="flex items-center gap-3 px-1 py-3"
      role="status"
      aria-live="polite"
      aria-label={message}
    >
      <div className="flex items-center gap-1">
        {[0, 1, 2].map((index) => (
          <motion.div
            key={index}
            className="h-2 w-2 rounded-full bg-slate-400"
            variants={pulseVariants}
            initial="initial"
            animate="animate"
            style={{
              animationDelay: `${index * 0.2}s`
            }}
            transition={{
              delay: index * 0.2,
              duration: 1.4,
              repeat: Infinity,
              ease: "easeInOut"
            }}
          />
        ))}
      </div>
      <span className="text-sm text-slate-600">
        {message}…
      </span>
      <span className="sr-only">{message}</span>
    </div>
  );
}

// Alternative minimal version for inline use
export function CopilotDotsOnly() {
  return (
    <motion.div
      className="flex items-center gap-1.5 px-1 py-2"
      role="status"
      aria-label="Génération en cours"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
    >
      {[0, 1, 2].map((index) => (
        <motion.span
          key={index}
          className="h-1.5 w-1.5 rounded-full bg-slate-400"
          animate={{
            scale: [1, 1.2, 1],
            opacity: [0.4, 0.8, 0.4]
          }}
          transition={{
            duration: 1.2,
            repeat: Infinity,
            delay: index * 0.15,
            ease: "easeInOut"
          }}
        />
      ))}
    </motion.div>
  );
}