import { useEffect, useState } from 'react';
import { Search, X } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface SearchInputProps {
  onSearch: (keyword: string) => void;
  placeholder?: string;
  debounceMs?: number;
}

export function SearchInput({ onSearch, placeholder = 'Search...', debounceMs = 300 }: SearchInputProps) {
  const [value, setValue] = useState('');

  useEffect(() => {
    const timer = setTimeout(() => {
      onSearch(value);
    }, debounceMs);
    return () => clearTimeout(timer);
  }, [value, debounceMs, onSearch]);

  return (
    <div className="relative">
      <Search className="absolute left-0 top-1/2 h-4 w-4 -translate-y-1/2 text-gold-muted" />
      <input
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder={placeholder}
        className="mansion-input w-full py-2 pl-7 pr-8"
      />
      {value && (
        <Button
          variant="ghost"
          size="sm"
          className="absolute right-0 top-1/2 h-7 w-7 -translate-y-1/2 p-0 text-muted-foreground hover:text-gold"
          onClick={() => setValue('')}
        >
          <X className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}
