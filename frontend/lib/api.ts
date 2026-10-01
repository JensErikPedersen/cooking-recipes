// The one place the frontend calls the backend. Every path is relative (/api/...), so the browser
// stays on this origin and next.config.ts forwards the call - the session and CSRF cookies are
// first-party and there is no CORS.

/** The backend's error envelope, as every non-2xx response carries it. */
export interface ErrorEnvelope {
  errorCode: number;
  message: string;
  description?: string;
  validationExceptions?: { objectName: string; message: string }[];
}

/** Thrown for any non-2xx response. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly envelope: ErrorEnvelope,
  ) {
    super(envelope.message);
  }
}

export interface AuthenticatedUser {
  username: string;
  roles: string[];
}

function readCookie(name: string): string | undefined {
  return document.cookie
    .split("; ")
    .find((cookie) => cookie.startsWith(`${name}=`))
    ?.slice(name.length + 1);
}

// Writes need the CSRF token from the XSRF-TOKEN cookie, sent back as a header. The backend issues
// that cookie on every response, but logout clears it - so if it is missing, any GET fetches a new
// one, even a 401.
async function csrfToken(): Promise<string> {
  if (!readCookie("XSRF-TOKEN")) {
    await fetch("/api/v1/auth/me");
  }
  return decodeURIComponent(readCookie("XSRF-TOKEN") ?? "");
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  if (method !== "GET") {
    headers["X-XSRF-TOKEN"] = await csrfToken();
  }
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  const response = await fetch(`/api/v1${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  if (!response.ok) {
    throw new ApiError(response.status, JSON.parse(text));
  }
  return (text ? JSON.parse(text) : undefined) as T;
}

/**
 * The audit fields every entity carries. The backend writes dates as "yyyy-MM-dd HH:mm" in UTC,
 * with no offset, and cannot read them back - so they are display-only and never sent.
 */
interface Audited {
  id: string;
  created: string;
  createdBy: string;
  updated?: string;
  updatedBy?: string;
}

export interface Category extends Audited {
  name: string;
  description?: string;
}

export const categories = {
  list: () => request<Category[]>("GET", "/categories"),
  get: (id: string) => request<Category>("GET", `/categories/${encodeURIComponent(id)}`),
};

export const auth = {
  me: () => request<AuthenticatedUser>("GET", "/auth/me"),
  login: (username: string, password: string) =>
    request<AuthenticatedUser>("POST", "/auth/login", { username, password }),
  logout: () => request<void>("POST", "/auth/logout"),
};
