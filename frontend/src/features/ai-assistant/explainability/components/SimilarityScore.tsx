interface SimilarityScoreProps {
  percent: number;
}

export function SimilarityScore({ percent }: SimilarityScoreProps) {
  return (
    <span className="inline-flex items-center rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium tabular-nums text-slate-700 dark:bg-slate-800 dark:text-slate-300">
      Similarité {percent.toFixed(0)}%
    </span>
  );
}
