import { Outlet } from 'react-router-dom';
import { Navbar } from '@/components/navbar';
import { Footer } from '@/components/footer';

export function RootLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className="flex-1 container mx-auto px-6 py-10">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
