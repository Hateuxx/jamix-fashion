/**
 * Replaces the query string without adding a history entry (filters and the open product are
 * "where you are", not steps to go back through). Keeps the path and hash intact.
 */
export function replaceSearch(search) {
  const query = search.replace(/^\?/, '');
  const { pathname, hash } = window.location;
  window.history.replaceState(window.history.state, '', `${pathname}${query ? `?${query}` : ''}${hash}`);
}
