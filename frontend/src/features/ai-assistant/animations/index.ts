// Animation presets for enterprise-grade AI Assistant

export const messageVariants = {
  initial: { 
    opacity: 0, 
    y: 20,
    scale: 0.98
  },
  animate: { 
    opacity: 1, 
    y: 0,
    scale: 1,
    transition: {
      type: "spring" as const,
      stiffness: 300,
      damping: 24,
      duration: 0.4
    }
  },
  exit: { 
    opacity: 0,
    y: -10,
    transition: { duration: 0.2 }
  }
};

export const cardVariants = {
  initial: { 
    opacity: 0,
    y: 12,
    scale: 0.96
  },
  animate: { 
    opacity: 1,
    y: 0,
    scale: 1,
    transition: {
      type: "spring" as const,
      stiffness: 280,
      damping: 26,
      duration: 0.3
    }
  },
  hover: {
    y: -2,
    scale: 1.01,
    boxShadow: "0 8px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)",
    transition: {
      type: "spring" as const,
      stiffness: 400,
      damping: 25
    }
  },
  tap: { scale: 0.98 }
};

export const fadeInUp = {
  initial: { opacity: 0, y: 24 },
  animate: { 
    opacity: 1, 
    y: 0,
    transition: {
      duration: 0.5,
      ease: [0.25, 0.1, 0.25, 1] as const
    }
  }
};

export const staggerContainer = {
  animate: {
    transition: {
      staggerChildren: 0.08
    }
  }
};

export const pulseVariants = {
  initial: { scale: 1, opacity: 0.4 },
  animate: { 
    scale: [1, 1.1, 1],
    opacity: [0.4, 0.8, 0.4],
    transition: {
      duration: 1.4,
      repeat: Infinity,
      ease: "easeInOut" as const
    }
  }
};