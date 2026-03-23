import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import type { Problem } from '@/api/types';

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().min(1, 'Description is required'),
});

type FormData = z.infer<typeof schema>;

interface NameDescriptionFormProps {
  target: { name: string; description: string } | null;
  onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
  problem: Problem | null;
  canUpdate?: boolean;
}

export function NameDescriptionForm({ target, onSubmit, problem, canUpdate = true }: NameDescriptionFormProps) {
  const isReadOnly = !!target && !canUpdate;
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: target ?? { name: '', description: '' },
  });

  async function handleFormSubmit(data: FormData) {
    if (isReadOnly) return;
    await onSubmit(data);
  }

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="max-w-md space-y-6">
      <ProblemAlert problem={problem} />
      <div className="space-y-2">
        <label htmlFor="name" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
          Name
        </label>
        <input id="name" {...register('name')} disabled={!!target || isReadOnly} className="mansion-input w-full py-2" />
        {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
      </div>
      <div className="space-y-2">
        <label htmlFor="description" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
          Description
        </label>
        <input id="description" {...register('description')} disabled={isReadOnly} className="mansion-input w-full py-2" />
        {errors.description && <p className="text-sm text-destructive">{errors.description.message}</p>}
      </div>
      {!isReadOnly && (
        <Button
          type="submit"
          disabled={isSubmitting}
          className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
        >
          {isSubmitting ? 'Saving...' : 'Save'}
        </Button>
      )}
    </form>
  );
}
