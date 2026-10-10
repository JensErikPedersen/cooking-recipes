// Where signing in returns to: the page a signed-out browser was heading for, carried to the login
// page as ?next=. Used by proxy.ts, by lib/api.ts on a 401, and by the login page.

/** The login page, remembering `path` - the path and query asked for - unless it is the start page. */
export function loginPath(path: string): string {
  return path === "/" ? "/login" : `/login?next=${encodeURIComponent(path)}`;
}

/**
 * Where to go after signing in. Only a path on this site: a crafted link could otherwise send a
 * user who has just signed in somewhere else. "//host" and "/\host" are other sites to a browser.
 */
export function afterSignIn(next: string | null): string {
  return next?.startsWith("/") && !next.startsWith("//") && !next.startsWith("/\\") ? next : "/";
}
