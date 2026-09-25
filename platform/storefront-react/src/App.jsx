import { LazyMotion, MotionConfig } from 'motion/react';

import { BagProvider } from './features/bag/BagProvider.jsx';
import { BagBar, BagButton, Toast } from './features/bag/components/BagBar.jsx';
import BagDrawer from './features/bag/components/BagDrawer.jsx';
import CatalogPage from './features/catalog/components/CatalogPage.jsx';
import { CATALOG_API_URL, WHATSAPP_NUMBER } from './shared/config.js';

// The animation engine arrives in its own chunk after first paint, so the store isn't slower to show.
const loadMotionFeatures = () => import('./motion-features.js').then((module) => module.default);

export default function App() {
  return (
    // reducedMotion="user": every Motion animation honours the OS "reduce motion" setting
    // (movement is dropped, opacity changes stay), without checks in each component.
    // LazyMotion + `m.*` components (strict: using the heavy `motion.*` by mistake throws in dev).
    <MotionConfig reducedMotion="user">
      <LazyMotion features={loadMotionFeatures} strict>
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
            <CatalogPage />
          </main>
          <BagDrawer apiBaseUrl={CATALOG_API_URL} whatsappNumber={WHATSAPP_NUMBER} />
          <BagBar />
          <Toast />
        </BagProvider>
      </LazyMotion>
    </MotionConfig>
  );
}
