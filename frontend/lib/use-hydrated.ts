import { useSyncExternalStore } from "react";

const subscribe = () => () => {};

/**
 * False in the server's HTML, true once React runs the page in the browser. A form shown before
 * that keeps its fields disabled: what is typed into them earlier, React never sees, and the form
 * would send them empty.
 */
export function useHydrated(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => true,
    () => false,
  );
}
