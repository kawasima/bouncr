import { AlertCircle } from 'lucide-react';
import type { Problem } from '@/api/types';

interface ProblemAlertProps {
  problem: Problem | null;
}

export function ProblemAlert({ problem }: ProblemAlertProps) {
  if (!problem) return null;

  return (
    <div className="border border-destructive/40 bg-destructive/5 px-5 py-4">
      <div className="flex items-start gap-3">
        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-destructive" />
        <div>
          <p className="text-sm font-medium uppercase tracking-wider text-destructive">Error</p>
          {problem.detail && <p className="mt-1 text-sm text-foreground/80">{problem.detail}</p>}
          {problem.violations && problem.violations.length > 0 && (
            <ul className="mt-2 list-none space-y-1 text-sm text-foreground/80">
              {problem.violations.map((v, i) => (
                <li key={i}>
                  <span className="text-destructive">{v.field}</span>: {v.message}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
