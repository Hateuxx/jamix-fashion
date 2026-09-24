import BagDrawer from './components/BagDrawer.jsx';
import { BagBar, BagButton, Toast } from './components/BagBar.jsx';
import ProductGrid from './components/ProductGrid.jsx';
import { CATALOG_API_URL, WHATSAPP_NUMBER } from './config.js';
import { BagProvider } from './hooks/useBag.jsx';

export default function App() {
  return (
    <BagProvider>
      <header className="sticky top-0 z-30 border-b border-jx-natural/70 bg-jx-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6">
          <a href="/" className="flex items-baseline gap-1.5">
            <span className="font-display text-2xl font-bold tracking-tight">Jamix</span>
            <span className="text-[0.65rem] font-medium tracking-[0.35em] text-jx-gold-dark">FASHION</span>
          </a>
          <BagButton />
        </div>
      </header>
      <main>
        <ProductGrid />
      </main>
      <BagDrawer apiBaseUrl={CATALOG_API_URL} whatsappNumber={WHATSAPP_NUMBER} />
      <BagBar />
      <Toast />
    </BagProvider>
  );
}
