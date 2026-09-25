import { useCallback, useEffect, useState } from 'react';

import { replaceSearch } from '../../../shared/url.js';

const PARAM = 'producto';

/** Which product's quick view is open, mirrored in ?producto=slug so it can be linked directly. */
export function useOpenProduct() {
  const [openSlug, setOpenSlug] = useState(() => new URLSearchParams(window.location.search).get(PARAM));

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (openSlug) params.set(PARAM, openSlug);
    else params.delete(PARAM);
    replaceSearch(params.toString());
  }, [openSlug]);

  // Stable, so memoized cards that receive `open` never re-render because of it.
  const open = useCallback((slug) => setOpenSlug(slug), []);
  const close = useCallback(() => setOpenSlug(null), []);

  return { openSlug, open, close };
}
