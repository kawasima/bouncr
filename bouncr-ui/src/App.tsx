import { RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/auth/auth-context';
import { Toaster } from '@/components/ui/sonner';
import { router } from '@/routes';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      staleTime: 30_000,
    },
  },
});

function App() {
  return (
    <AuthProvider>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
        <Toaster />
      </QueryClientProvider>
    </AuthProvider>
  );
}

export default App;
