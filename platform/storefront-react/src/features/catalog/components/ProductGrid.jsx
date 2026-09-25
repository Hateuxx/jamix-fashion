import { AnimatePresence } from 'motion/react';
import { memo } from 'react';

import ProductCard from './ProductCard.jsx';

/**
 * Presentational grid. One stable list: cards leaving the filter fade out while the remaining ones
 * glide to their new cells (popLayout). Memoized: only re-renders when the visible list changes.
 */
function ProductGrid({ products, apiBaseUrl, onOpen }) {
  return (
    <ul className="grid grid-cols-2 gap-4 sm:gap-6 lg:grid-cols-4">
      <AnimatePresence mode="popLayout">
        {products.map((product, index) => (
          <ProductCard key={product.id} index={index} product={product} apiBaseUrl={apiBaseUrl} onOpen={onOpen} />
        ))}
      </AnimatePresence>
    </ul>
  );
}

export default memo(ProductGrid);
