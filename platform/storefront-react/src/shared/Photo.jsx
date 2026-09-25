import { useState } from 'react';

import { photoSources } from './photos.js';

/**
 * <img> that loads the right-sized WebP for the screen. If a variant is missing (a photo added
 * without `npm run images`), it falls back to the original instead of showing a broken image.
 */
export default function Photo({ imageUrl, apiBaseUrl, sizes, alt = '', ...props }) {
  const { src, srcSet } = photoSources(imageUrl, { apiBaseUrl, assetBase: import.meta.env.BASE_URL });
  const [failed, setFailed] = useState(false);
  const useVariants = srcSet && !failed;

  return (
    <img
      src={src}
      srcSet={useVariants ? srcSet : undefined}
      sizes={useVariants ? sizes : undefined}
      alt={alt}
      onError={useVariants ? () => setFailed(true) : undefined}
      {...props}
    />
  );
}
