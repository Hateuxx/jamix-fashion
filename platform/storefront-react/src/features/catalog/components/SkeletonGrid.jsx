/** Placeholder cards while the catalogue loads (same grid as the real one, so nothing jumps). */
export default function SkeletonGrid() {
  return (
    <ul aria-hidden="true" className="grid grid-cols-2 gap-4 sm:gap-6 lg:grid-cols-4">
      {Array.from({ length: 8 }, (_, i) => (
        <li key={i} className="overflow-hidden rounded-2xl bg-jx-white">
          <div className="aspect-[3/4] animate-pulse bg-jx-natural/70" />
          <div className="space-y-2 p-5">
            <div className="h-4 w-2/3 animate-pulse rounded bg-jx-natural/70" />
            <div className="h-4 w-1/3 animate-pulse rounded bg-jx-natural/70" />
          </div>
        </li>
      ))}
    </ul>
  );
}
